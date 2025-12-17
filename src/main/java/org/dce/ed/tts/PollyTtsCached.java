package org.dce.ed.tts;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

public final class PollyTtsCached implements Closeable {

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
        playbackQueue.submit(() -> {
            try {
                speakBlocking(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Blocking: completes only after the clip finishes playing.
     * (Useful for tests, or if you ever want “wait until done”.)
     */
    public void speakBlocking(String text) throws Exception {
        Objects.requireNonNull(text, "text");

        // Pull settings from prefs
        String voiceName = OverlayPreferences.getSpeechVoiceId();
        if (voiceName == null || voiceName.isBlank()) {
            voiceName = "Joanna";
        }

        String engineName = OverlayPreferences.getSpeechEngine();
        Engine engine = "neural".equalsIgnoreCase(engineName)
                ? Engine.NEURAL
                : Engine.STANDARD;

        // For easy playback in JavaSound: request PCM and wrap into WAV ourselves.
        int sampleRate = OverlayPreferences.getSpeechSampleRateHz();
        if (sampleRate != 8000 && sampleRate != 16000) {
            sampleRate = 16000;
        }

        Path wavFile = getOrCreateCachedWav(text, voiceName, engine, sampleRate);
        playWavBlocking(wavFile);
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
                .outputFormat(OutputFormat.PCM) // PCM 16-bit signed little-endian mono
                .sampleRate(Integer.toString(sampleRate))
                .textType(TextType.TEXT)
                .text(text)
                .build();

        Path tmp = voiceDir.resolve(key + ".tmp");
        try (ResponseInputStream<SynthesizeSpeechResponse> audio = polly.synthesizeSpeech(req)) {
            // Write to tmp then move atomically
            writePcmAsWav(audio, tmp, sampleRate);
        }

        try {
            Files.move(tmp, wav);
        } catch (IOException moveEx) {
            // If move fails (e.g., antivirus lock), fall back to copy+delete.
            Files.copy(tmp, wav);
            Files.deleteIfExists(tmp);
        }

        // Append to manifest ONLY when we generated a new file
        appendToManifest(voiceDir, wav.getFileName().toString(), text);

        return wav;
    }

    private Path getVoiceCacheDir(String voiceName, Engine engine, int sampleRate) {
        Path root = OverlayPreferences.getSpeechCacheDir();
        if (root == null) {
            root = Path.of(System.getProperty("user.home"), ".edo", "tts-cache");
        }

        // Separate directories per voice (and engine + sample rate so you don’t mix formats).
        String safeVoice = voiceName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
        String safeEngine = engine.toString().toLowerCase(Locale.ROOT);
        String safeSr = Integer.toString(sampleRate);

        Path path = root.resolve(safeVoice);
        //.resolve(safeEngine).resolve(safeSr);
        return path;
    }

    private void appendToManifest(Path voiceDir, String fileName, String text) {
        synchronized (manifestLock) {
            Path manifest = voiceDir.resolve("manifest.tsv");
            String line = Instant.now() + "\t" + fileName + "\t" + text.replace("\t", " ").replace("\r", " ").replace("\n", " ") + "\n";
            try {
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

    private static void playWavBlocking(Path wavPath) {
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

            // Wait for playback to finish
            done.await();

            clip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes a standard RIFF/WAVE header + PCM payload.
     * Polly PCM is 16-bit signed little-endian mono. :contentReference[oaicite:2]{index=2}
     */
    private static void writePcmAsWav(InputStream pcm, Path wavOut, int sampleRate) throws IOException {
        byte[] pcmBytes = pcm.readAllBytes();

        int numChannels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;

        int subchunk2Size = pcmBytes.length;
        int chunkSize = 36 + subchunk2Size;

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(
                wavOut, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)))) {

            // RIFF header
            out.writeBytes("RIFF");
            writeIntLE(out, chunkSize);
            out.writeBytes("WAVE");

            // fmt subchunk
            out.writeBytes("fmt ");
            writeIntLE(out, 16);                 // Subchunk1Size for PCM
            writeShortLE(out, (short) 1);        // AudioFormat = 1 (PCM)
            writeShortLE(out, (short) numChannels);
            writeIntLE(out, sampleRate);
            writeIntLE(out, byteRate);
            writeShortLE(out, (short) blockAlign);
            writeShortLE(out, (short) bitsPerSample);

            // data subchunk
            out.writeBytes("data");
            writeIntLE(out, subchunk2Size);
            out.write(pcmBytes);
        }
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

    private static String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ");
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
        playbackQueue.shutdownNow();
        try {
            polly.close();
        } catch (Exception e) {
            // ignore
        }
    }

    // Quick smoke test
    public static void main(String[] args) {
        System.out.println("Starting");
        try (PollyTtsCached tts = new PollyTtsCached()) {
            tts.speak("hello");
            tts.speak("this should play after hello finishes");
            tts.speak("and this should play after that");
            System.out.println("Queued.");
            // main can exit; playback thread is daemon. If you want it to wait, sleep briefly.
            Thread.sleep(4000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
