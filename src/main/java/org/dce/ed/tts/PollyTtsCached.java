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
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

import org.dce.ed.OverlayPreferences;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

public class PollyTtsCached implements Closeable {

	// Trim leading/trailing silence from Polly PCM before writing cache WAVs.
	private static final int TRIM_ABS_THRESHOLD = 250;  // 16-bit PCM amplitude threshold (0..32767)
	private static final int TRIM_KEEP_MS = 1;         // keep a little padding to avoid clipped starts/ends

	
    // “Standard US English” voices list (good enough for a simple pick-list).
    // (These are also the well-known US English voices commonly used in examples.)
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
        Region region = Region.of(OverlayPreferences.getSpeechAwsRegion());
        this.polly = PollyClient.builder()
                .region(region)
                .build();
    }

    /**
     * Non-blocking: returns immediately, but audio plays sequentially.
     * If you call speak() while something is playing, it will queue and wait its turn.
     */
    public void speak(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        getPlaybackQueue().submit(() -> {
            try {
                speakBlocking(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Blocking: does not return until the audio has finished playing.
     * (Useful for tests, or if you ever want “wait until done”.)
     */
    public void speakBlocking(String text) throws Exception {
        Objects.requireNonNull(text, "text");

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

        Path wavFile = getOrCreateCachedWav(text, voiceName, engine, sampleRate);
        playWavBlockingInternal(wavFile);
    }

    /**
     * Ensures the given utterance is present in the local cache and returns the cached WAV path.
     * This does NOT play audio.
     */
    public Path ensureCachedWav(String text) throws Exception {
        if (text == null || text.isBlank()) {
            return null;
        }

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

        return getOrCreateCachedWav(text, voiceName, engine, sampleRate);
    }

    /**
     * Ensures all utterances are cached and returns WAV paths in the same order (null/blank inputs skipped).
     * This does NOT play audio.
     */
    public List<Path> ensureCachedWavs(List<String> utterances) throws Exception {
        if (utterances == null || utterances.isEmpty()) {
            return List.of();
        }

        List<Path> out = new java.util.ArrayList<>();
        for (String u : utterances) {
            Path p = ensureCachedWav(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Plays a WAV file synchronously (blocks until finished).
     * This is primarily for higher-level code that assembles audio from cached chunks.
     */
    public void playWavBlocking(Path wavPath) {
        playWavBlockingInternal(wavPath);
    }

    private Path getOrCreateCachedWav(String text, String voiceName, Engine engine, int sampleRate) throws IOException {
        Path voiceDir = getVoiceCacheDir(voiceName, engine, sampleRate);
        Files.createDirectories(voiceDir);

        String key = sha256(
                "v=" + voiceName
                        + "|e=" + engine.toString()
                        + "|sr=" + sampleRate
                        + "|t=" + normalize(text)
        );

        Path wav = voiceDir.resolve(key + ".wav");
        if (Files.exists(wav)) {
            return wav;
        }

        // Generate PCM with Polly
        VoiceId voiceId = VoiceId.fromValue(voiceName);

        SynthesizeSpeechRequest req = SynthesizeSpeechRequest.builder()
                .engine(engine)
                .voiceId(voiceId)
                .outputFormat(OutputFormat.PCM)
                .sampleRate(Integer.toString(sampleRate))
                .textType(TextType.TEXT)
                .text(text)
                .build();

        ResponseInputStream<SynthesizeSpeechResponse> in = polly.synthesizeSpeech(req);
        writePcmToWav(in, wav, sampleRate, (short) 1, (short) 16);

        // Log to manifest
        writeManifestLine(voiceDir, wav.getFileName().toString(), text);

        return wav;
    }

    private static Path getVoiceCacheDir(String voiceName, Engine engine, int sampleRate) {
        Path base = OverlayPreferences.getSpeechCacheDir();
        String name = (voiceName == null) ? "unknown" : voiceName.trim();
        return base.resolve(name);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    private static void writePcmToWav(InputStream pcm, Path wavOut, int sampleRate, short channels, short bitsPerSample) throws IOException {
        // WAV header + raw PCM payload
        // Polly PCM is signed little-endian, 16-bit.
        byte[] pcmBytes = pcm.readAllBytes();

        pcmBytes = trimSilencePcm16leMono(pcmBytes, sampleRate, TRIM_ABS_THRESHOLD, TRIM_KEEP_MS);
        
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        short blockAlign = (short) (channels * (bitsPerSample / 8));
        int dataLen = pcmBytes.length;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(wavOut, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
        )) {
            // RIFF header
            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes(36 + dataLen));
            out.writeBytes("WAVE");

            // fmt chunk
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16)); // PCM fmt chunk size
            out.writeShort(Short.reverseBytes((short) 1)); // audio format = PCM
            out.writeShort(Short.reverseBytes(channels));
            out.writeInt(Integer.reverseBytes(sampleRate));
            out.writeInt(Integer.reverseBytes(byteRate));
            out.writeShort(Short.reverseBytes(blockAlign));
            out.writeShort(Short.reverseBytes(bitsPerSample));

            // data chunk
            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes(dataLen));
            out.write(pcmBytes);
        }
    }

    private static byte[] trimSilencePcm16leMono(byte[] pcm, int sampleRate, int absThreshold, int keepMs) {
        if (pcm == null || pcm.length < 4) {
            return pcm;
        }

        // Ensure even byte count (16-bit samples)
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

        // All silence: keep a tiny amount so we still produce a valid WAV.
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

    
    private void writeManifestLine(Path voiceDir, String wavFileName, String text) {
        synchronized (manifestLock) {
            try {
                Path manifest = voiceDir.resolve("_manifest.txt");
                String line = Instant.now().toString()
                        + "\t" + wavFileName
                        + "\t" + text.replace("\n", "\\n")
                        + System.lineSeparator();

                Files.writeString(
                        manifest,
                        line,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException e) {
                // Don’t break speech if manifest logging fails
                e.printStackTrace();
            }
        }
    }

    void playCombinedWavsBlocking(List<Path> wavPaths) throws Exception {
        if (wavPaths == null || wavPaths.isEmpty()) {
            return;
        }

        List<AudioInputStream> streams = new java.util.ArrayList<>();
        AudioFormat format = null;
        long totalFrames = 0;

        try {
            for (Path p : wavPaths) {
                if (p == null) {
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

                long frames = ais.getFrameLength();
                if (frames > 0) {
                    totalFrames += frames;
                }
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

            // If frame length is unknown (-1), pass -1; Clip still works with many WAVs.
            long combinedFrames = (totalFrames > 0) ? totalFrames : AudioSystem.NOT_SPECIFIED;

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

    private void playAudioBlocking(AudioInputStream ais) throws Exception {
        Clip clip = AudioSystem.getClip();
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

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
        if (!java.util.Objects.equals(a.getEncoding(), b.getEncoding())) {
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

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        getPlaybackQueue().shutdownNow();
        try {
            polly.close();
        } catch (Exception e) {
            e.printStackTrace();
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

	public ExecutorService getPlaybackQueue() {
		return playbackQueue;
	}
}
