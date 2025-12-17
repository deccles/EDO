package org.dce.ed.tts;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

import org.dce.ed.OverlayPreferences;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SpeechMarkType;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

public class PollyTtsCached implements Closeable {

    // Trim leading/trailing silence from Polly PCM before writing cache WAVs.
    private static final int TRIM_ABS_THRESHOLD = 250;  // 16-bit PCM amplitude threshold (0..32767)
    private static final int TRIM_KEEP_MS = 1;          // you tuned this down and liked it

    // Speech-mark parsing (JSON lines)
    private static final Pattern SSML_MARK_LINE = Pattern.compile(".*\\\"value\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?\\\"time\\\"\\s*:\\s*(\\d+).*", Pattern.CASE_INSENSITIVE);

    public static final List<String> STANDARD_US_ENGLISH_VOICES = List.of(
            "Ivy", "Joanna", "Kendra", "Kimberly", "Salli", "Joey", "Justin", "Matthew"
    );

    // Single-thread executor = non-blocking callers, but sequential playback.
    private final ExecutorService playbackQueue = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PollyTtsCached-Playback");
        t.setDaemon(true);
        return t;
    });

    private final PollyClient polly;
    private final Object manifestLock = new Object();

    public PollyTtsCached() {
        this.polly = PollyClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(OverlayPreferences.getSpeechAwsRegion()))
                .build();
    }

    public ExecutorService getPlaybackQueue() {
        return playbackQueue;
    }

    public void speak(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        playbackQueue.submit(() -> {
            try {
                speakBlocking(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void speakBlocking(String text) throws Exception {
        Objects.requireNonNull(text, "text");

        VoiceSettings s = resolveVoiceSettings();
        Path wavFile = getOrCreateCachedWav(text, s.voiceName, s.engine, s.sampleRate);
        playWavBlockingInternal(wavFile);
    }

    public Path ensureCachedWav(String text) throws Exception {
        if (text == null || text.isBlank()) {
            return null;
        }
        VoiceSettings s = resolveVoiceSettings();
        return getOrCreateCachedWav(text, s.voiceName, s.engine, s.sampleRate);
    }

    public List<Path> ensureCachedWavs(List<String> utterances) throws Exception {
        if (utterances == null || utterances.isEmpty()) {
            return List.of();
        }

        List<Path> out = new ArrayList<>();
        for (String u : utterances) {
            Path p = ensureCachedWav(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Option B (SSML marks): synthesize the full SSML sentence once, get SSML-mark times,
     * then cache each chunk by slicing the full PCM between marks.
     *
     * - chunkTexts.size() must match markNames.size()
     * - each mark name should exist in the SSML as <mark name="..."/>
     */
    public List<Path> ensureCachedWavsFromSsmlMarks(List<String> chunkTexts, String ssml, List<String> markNames) throws Exception {
        if (chunkTexts == null || markNames == null || chunkTexts.isEmpty()) {
            return List.of();
        }
        if (chunkTexts.size() != markNames.size()) {
            throw new IllegalArgumentException("chunkTexts.size != markNames.size");
        }
        if (ssml == null || ssml.isBlank()) {
            throw new IllegalArgumentException("ssml is blank");
        }

        VoiceSettings s = resolveVoiceSettings();
        Path voiceDir = getVoiceCacheDir(s.voiceName, s.engine, s.sampleRate);
        Files.createDirectories(voiceDir);

        // Determine which chunk wavs are missing.
        List<Path> paths = new ArrayList<>(chunkTexts.size());
        List<Integer> missingIdx = new ArrayList<>();

        for (int i = 0; i < chunkTexts.size(); i++) {
            String t = chunkTexts.get(i);
            if (t == null || t.isBlank()) {
                paths.add(null);
                continue;
            }
            Path wav = getCachedWavPath(voiceDir, s, t);
            paths.add(wav);
            if (!Files.exists(wav)) {
                missingIdx.add(i);
            }
        }

        if (missingIdx.isEmpty()) {
            return paths;
        }

        // Synthesize full sentence PCM (SSML) and SSML speech marks (timestamps for <mark/>).
        byte[] fullPcm = synthesizePcmSsml(ssml, s);
        Map<String, Integer> markTimesMs = synthesizeSsmlMarkTimes(ssml, s);

        // Ensure we have monotonic mark times for all marks in order.
        int[] startMs = new int[markNames.size()];
        for (int i = 0; i < markNames.size(); i++) {
            String name = markNames.get(i);
            Integer t = markTimesMs.get(name);
            if (t == null) {
                throw new IllegalStateException("Missing SSML mark time for: " + name);
            }
            startMs[i] = t.intValue();
        }

        // Convert mark start times into byte offsets.
        int bytesPerSample = 2; // 16-bit mono
        int[] startByte = new int[startMs.length];
        for (int i = 0; i < startMs.length; i++) {
            long sampleIndex = (long) startMs[i] * (long) s.sampleRate / 1000L;
            long byteIndex = sampleIndex * bytesPerSample;
            if (byteIndex < 0) {
                byteIndex = 0;
            }
            if (byteIndex > fullPcm.length) {
                byteIndex = fullPcm.length;
            }
            startByte[i] = (int) (byteIndex & ~1); // even
        }

        // Cache missing chunks by slicing [mark_i .. mark_{i+1}) (or end for last).
        for (int mi = 0; mi < missingIdx.size(); mi++) {
            int i = missingIdx.get(mi);

            int from = startByte[i];
            int to;
            if (i + 1 < startByte.length) {
                to = startByte[i + 1];
            } else {
                to = fullPcm.length;
            }

            if (to < from) {
                to = from;
            }
            if ((to - from) < 2) {
                // keep at least 1 sample so WAV isn't empty
                to = Math.min(fullPcm.length, from + 2);
            }

            byte[] slice = new byte[to - from];
            System.arraycopy(fullPcm, from, slice, 0, slice.length);

            Path wavPath = paths.get(i);
            // Write to tmp then move (avoid partial file in cache)
            Path tmp = wavPath.resolveSibling(wavPath.getFileName().toString() + ".tmp");
            writePcmBytesAsWav(slice, tmp, s.sampleRate);
            try {
                Files.move(tmp, wavPath);
            } catch (IOException moveEx) {
                Files.copy(tmp, wavPath);
                Files.deleteIfExists(tmp);
            }

            appendToManifest(voiceDir, wavPath.getFileName().toString(), chunkTexts.get(i));
        }

        return paths;
    }

    public void playWavBlocking(Path wavPath) {
        playWavBlockingInternal(wavPath);
    }

    /**
     * Plays a list of WAV files as one continuous stream using a single Clip.
     * All WAVs must share the same AudioFormat.
     */
    void playCombinedWavsBlocking(List<Path> wavPaths) throws Exception {
        if (wavPaths == null || wavPaths.isEmpty()) {
            return;
        }

        List<AudioInputStream> streams = new ArrayList<>();
        AudioFormat format = null;

        try {
            for (Path p : wavPaths) {
                if (p == null) {
                    continue;
                }
                if (!Files.exists(p)) {
                    continue;
                }

                AudioInputStream ais = AudioSystem.getAudioInputStream(p.toFile());
                AudioFormat f = ais.getFormat();

                if (format == null) {
                    format = f;
                } else if (!formatsEquivalent(format, f)) {
                    ais.close();
                    throw new IllegalArgumentException("WAV format mismatch: " + p);
                }

                streams.add(ais);
            }

            if (streams.isEmpty()) {
                return;
            }

            Vector<InputStream> inputs = new Vector<>();
            for (AudioInputStream ais : streams) {
                inputs.add(ais);
            }
            Enumeration<InputStream> en = inputs.elements();
            SequenceInputStream seq = new SequenceInputStream(en);

            // Frame length unknown is fine for Clip in practice; compute if available.
            long totalFrames = 0;
            boolean anyKnown = false;
            for (AudioInputStream ais : streams) {
                long fl = ais.getFrameLength();
                if (fl > 0) {
                    anyKnown = true;
                    totalFrames += fl;
                }
            }
            long combinedFrames = anyKnown ? totalFrames : AudioSystem.NOT_SPECIFIED;

            try (AudioInputStream combined = new AudioInputStream(seq, format, combinedFrames)) {
                playAudioBlocking(combined);
            }
        } finally {
            for (AudioInputStream ais : streams) {
                try {
                    ais.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
    }

    // ------------------------------
    // Cache implementation
    // ------------------------------

    private Path getOrCreateCachedWav(String text, String voiceName, Engine engine, int sampleRate) throws IOException {
        VoiceSettings s = new VoiceSettings(voiceName, engine, sampleRate);

        Path voiceDir = getVoiceCacheDir(voiceName, engine, sampleRate);
        Files.createDirectories(voiceDir);

        Path wav = getCachedWavPath(voiceDir, s, text);
        if (Files.exists(wav)) {
            return wav;
        }

        // Generate PCM with Polly (TEXT) and cache it.
        VoiceId voiceId = VoiceId.fromValue(voiceName);

        SynthesizeSpeechRequest req = SynthesizeSpeechRequest.builder()
                .engine(engine)
                .voiceId(voiceId)
                .outputFormat(OutputFormat.PCM) // PCM 16-bit signed little-endian mono
                .sampleRate(Integer.toString(sampleRate))
                .textType(TextType.TEXT)
                .text(text)
                .build();

        Path tmp = voiceDir.resolve(wav.getFileName().toString() + ".tmp");
        try (ResponseInputStream<SynthesizeSpeechResponse> audio = polly.synthesizeSpeech(req)) {
            writePcmAsWav(audio, tmp, sampleRate);
        }

        try {
            Files.move(tmp, wav);
        } catch (IOException moveEx) {
            Files.copy(tmp, wav);
            Files.deleteIfExists(tmp);
        }

        appendToManifest(voiceDir, wav.getFileName().toString(), text);
        return wav;
    }

    private Path getCachedWavPath(Path voiceDir, VoiceSettings s, String text) {
        String key = sha256(
                "v=" + s.voiceName
                        + "|e=" + s.engine.toString()
                        + "|sr=" + s.sampleRate
                        + "|t=" + normalize(text)
        );
        return voiceDir.resolve(key + ".wav");
    }

    private Path getVoiceCacheDir(String voiceName, Engine engine, int sampleRate) {
        Path root = OverlayPreferences.getSpeechCacheDir();
        if (root == null) {
            root = Path.of(System.getProperty("user.home"), ".edo", "tts-cache");
        }

        // Separate directories per voice (you can re-add engine + sample rate if you want)
        String safeVoice = voiceName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
        return root.resolve(safeVoice);
    }

    private void appendToManifest(Path voiceDir, String fileName, String text) {
        synchronized (manifestLock) {
            Path manifest = voiceDir.resolve("manifest.tsv");
            String line = Instant.now() + "\t" + fileName + "\t"
                    + text.replace("\t", " ").replace("\r", " ").replace("\n", " ")
                    + System.lineSeparator();
            try {
                Files.writeString(
                        manifest,
                        line,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            } catch (Exception e) {
                // ignore
            }
        }
    }

    // ------------------------------
    // Polly synth helpers
    // ------------------------------

    private byte[] synthesizePcmSsml(String ssml, VoiceSettings s) throws IOException {
        VoiceId voiceId = VoiceId.fromValue(s.voiceName);

        SynthesizeSpeechRequest req = SynthesizeSpeechRequest.builder()
                .engine(s.engine)
                .voiceId(voiceId)
                .outputFormat(OutputFormat.PCM)
                .sampleRate(Integer.toString(s.sampleRate))
                .textType(TextType.SSML)
                .text(ssml)
                .build();

        try (ResponseInputStream<SynthesizeSpeechResponse> audio = polly.synthesizeSpeech(req)) {
            return audio.readAllBytes();
        }
    }

    private Map<String, Integer> synthesizeSsmlMarkTimes(String ssml, VoiceSettings s) throws IOException {
        VoiceId voiceId = VoiceId.fromValue(s.voiceName);

        SynthesizeSpeechRequest req = SynthesizeSpeechRequest.builder()
                .engine(s.engine)
                .voiceId(voiceId)
                .outputFormat(OutputFormat.JSON)
                .speechMarkTypes(SpeechMarkType.SSML)
                .textType(TextType.SSML)
                .text(ssml)
                .build();

        try (ResponseInputStream<SynthesizeSpeechResponse> marks = polly.synthesizeSpeech(req)) {
            String jsonLines = new String(marks.readAllBytes(), StandardCharsets.UTF_8);
            return parseSsmlMarks(jsonLines);
        }
    }

    private static Map<String, Integer> parseSsmlMarks(String jsonLines) {
        // Polly returns JSON Lines; we only care about ssml marks: {"time":123,"type":"ssml","value":"C0"}
        Map<String, Integer> out = new LinkedHashMap<>();
        if (jsonLines == null || jsonLines.isBlank()) {
            return out;
        }

        String[] lines = jsonLines.split("\r?\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            Matcher m = SSML_MARK_LINE.matcher(line);
            if (!m.find()) {
                // try alternate field ordering
                // {"time":0,"type":"ssml","value":"C0"} or {"type":"ssml","value":"C0","time":0}
                String v = extractJsonString(line, "value");
                String t = extractJsonNumber(line, "time");
                String ty = extractJsonString(line, "type");
                if (!"ssml".equalsIgnoreCase(ty) || v == null || t == null) {
                    continue;
                }
                out.put(v, Integer.parseInt(t));
                continue;
            }

            String value = m.group(1);
            String time = m.group(2);
            out.put(value, Integer.parseInt(time));
        }
        return out;
    }

    private static String extractJsonString(String line, String key) {
        int idx = line.indexOf("\"" + key + "\"" );
        if (idx < 0) {
            return null;
        }
        int colon = line.indexOf(':', idx);
        if (colon < 0) {
            return null;
        }
        int q1 = line.indexOf('"', colon + 1);
        if (q1 < 0) {
            return null;
        }
        int q2 = line.indexOf('"', q1 + 1);
        if (q2 < 0) {
            return null;
        }
        return line.substring(q1 + 1, q2);
    }

    private static String extractJsonNumber(String line, String key) {
        int idx = line.indexOf("\"" + key + "\"" );
        if (idx < 0) {
            return null;
        }
        int colon = line.indexOf(':', idx);
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        int j = i;
        while (j < line.length() && Character.isDigit(line.charAt(j))) {
            j++;
        }
        if (j == i) {
            return null;
        }
        return line.substring(i, j);
    }

    // ------------------------------
    // Playback helpers
    // ------------------------------

    private static void playWavBlockingInternal(Path wavPath) {
        if (wavPath == null) {
            return;
        }

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavPath.toFile())) {
            Clip clip = AudioSystem.getClip();

            CountDownLatch done = new CountDownLatch(1);
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP || e.getType() == LineEvent.Type.CLOSE) {
                    done.countDown();
                }
            });

            clip.open(ais);
            clip.start();

            done.await();
            clip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playAudioBlocking(AudioInputStream ais) throws Exception {
        Clip clip = AudioSystem.getClip();
        CountDownLatch done = new CountDownLatch(1);

        clip.addLineListener(e -> {
            if (e.getType() == LineEvent.Type.STOP || e.getType() == LineEvent.Type.CLOSE) {
                done.countDown();
            }
        });

        clip.open(ais);
        clip.start();
        done.await();
        clip.close();
    }

    private static boolean formatsEquivalent(AudioFormat a, AudioFormat b) {
        if (!Objects.equals(a.getEncoding(), b.getEncoding())) {
            return false;
        }
        if (a.getSampleRate() != b.getSampleRate()) {
            return false;
        }
        if (a.getSampleSizeInBits() != b.getSampleSizeInBits()) {
            return false;
        }
        if (a.getChannels() != b.getChannels()) {
            return false;
        }
        if (a.isBigEndian() != b.isBigEndian()) {
            return false;
        }
        if (a.getFrameSize() != b.getFrameSize()) {
            return false;
        }
        return a.getFrameRate() == b.getFrameRate();
    }

    // ------------------------------
    // WAV writing + trimming
    // ------------------------------

    private static void writePcmAsWav(InputStream pcm, Path wavOut, int sampleRate) throws IOException {
        byte[] pcmBytes = pcm.readAllBytes();
        writePcmBytesAsWav(pcmBytes, wavOut, sampleRate);
    }

    private static void writePcmBytesAsWav(byte[] pcmBytes, Path wavOut, int sampleRate) throws IOException {
        if (pcmBytes == null) {
            pcmBytes = new byte[0];
        }

        pcmBytes = trimSilencePcm16leMono(pcmBytes, sampleRate, TRIM_ABS_THRESHOLD, TRIM_KEEP_MS);

        int numChannels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;

        int subchunk2Size = pcmBytes.length;
        int chunkSize = 36 + subchunk2Size;

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(
                wavOut, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)))) {

            out.writeBytes("RIFF");
            writeIntLE(out, chunkSize);
            out.writeBytes("WAVE");

            out.writeBytes("fmt ");
            writeIntLE(out, 16);
            writeShortLE(out, (short) 1);
            writeShortLE(out, (short) numChannels);
            writeIntLE(out, sampleRate);
            writeIntLE(out, byteRate);
            writeShortLE(out, (short) blockAlign);
            writeShortLE(out, (short) bitsPerSample);

            out.writeBytes("data");
            writeIntLE(out, subchunk2Size);
            out.write(pcmBytes);
        }
    }

    private static byte[] trimSilencePcm16leMono(byte[] pcm, int sampleRate, int absThreshold, int keepMs) {
        if (pcm == null || pcm.length < 4) {
            return pcm;
        }

        int len = pcm.length & ~1;
        if (len < 2) {
            return pcm;
        }

        int keepSamples = (int) ((keepMs / 1000.0) * sampleRate);
        if (keepSamples < 0) {
            keepSamples = 0;
        }
        int keepBytes = keepSamples * 2;

        int start = 0;
        while (start + 1 < len) {
            int sample = (short) ((pcm[start + 1] << 8) | (pcm[start] & 0xFF));
            if (Math.abs(sample) > absThreshold) {
                break;
            }
            start += 2;
        }

        int end = len - 2;
        while (end >= 0) {
            int sample = (short) ((pcm[end + 1] << 8) | (pcm[end] & 0xFF));
            if (Math.abs(sample) > absThreshold) {
                break;
            }
            end -= 2;
        }

        if (end < start) {
            int outLen = keepBytes;
            if (outLen < 2) {
                outLen = 2;
            }
            if (outLen > len) {
                outLen = len;
            }

            byte[] out = new byte[outLen];
            System.arraycopy(pcm, 0, out, 0, outLen);
            return out;
        }

        start -= keepBytes;
        if (start < 0) {
            start = 0;
        }

        end += keepBytes;
        if (end > len - 2) {
            end = len - 2;
        }

        int outLen = (end - start) + 2;
        if (outLen <= 0) {
            return pcm;
        }

        byte[] out = new byte[outLen];
        System.arraycopy(pcm, start, out, 0, outLen);
        return out;
    }

    private static void writeIntLE(DataOutputStream out, int v) throws IOException {
        out.writeByte(v & 0xFF);
        out.writeByte((v >>> 8) & 0xFF);
        out.writeByte((v >>> 16) & 0xFF);
        out.writeByte((v >>> 24) & 0xFF);
    }

    private static void writeShortLE(DataOutputStream out, short v) throws IOException {
        out.writeByte(v & 0xFF);
        out.writeByte((v >>> 8) & 0xFF);
    }

    // ------------------------------
    // Settings + utils
    // ------------------------------

    private VoiceSettings resolveVoiceSettings() {
        String voiceName = OverlayPreferences.getSpeechVoiceName();
        if (voiceName == null || voiceName.isBlank()) {
            voiceName = "Joanna";
        }

        Engine engine = OverlayPreferences.getSpeechEngine();
        if (engine == null) {
            engine = Engine.NEURAL;
        }

        int sampleRate = OverlayPreferences.getSpeechSampleRateHz();
        if (sampleRate != 8000 && sampleRate != 16000) {
            sampleRate = 16000;
        }

        return new VoiceSettings(voiceName, engine, sampleRate);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("\s+", " ");
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class VoiceSettings {
        private final String voiceName;
        private final Engine engine;
        private final int sampleRate;

        private VoiceSettings(String voiceName, Engine engine, int sampleRate) {
            this.voiceName = voiceName;
            this.engine = engine;
            this.sampleRate = sampleRate;
        }
    }

    @Override
    public void close() {
        playbackQueue.shutdownNow();
        try {
            polly.close();
        } catch (Exception e) {
            // ignore
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting");
        try (PollyTtsCached tts = new PollyTtsCached()) {
            tts.speak("hello");
            tts.speak("this should play after hello finishes");
            tts.speak("and this should play after that");
            System.out.println("Queued.");
            Thread.sleep(4000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
