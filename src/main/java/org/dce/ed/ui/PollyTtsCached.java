package org.dce.ed.ui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.dce.ed.OverlayPreferences;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.VoiceId;

public final class PollyTtsCached {

    /**
     * “Free for now” list: keep it to common Standard US English voices.
     * (You can expand this later, or query DescribeVoices once creds are working.)
     */
    public static final String[] STANDARD_US_ENGLISH_VOICES = new String[] {
        "Joanna", "Matthew", "Ivy", "Justin", "Kendra", "Kimberly", "Salli", "Joey"
    };

    private PollyTtsCached() {
    }

    /**
     * Simple API you can call from anywhere in the overlay.
     */
    public static void speak(String text) {
        if (!OverlayPreferences.isSpeechEnabled()) {
            return;
        }
        if (text == null || text.isBlank()) {
            return;
        }

        Path wav = getOrCreateCachedWav(text);
        playWav(wav);
    }

    /**
     * Quick test entry point.
     *
     * Before this will work, you must have AWS credentials configured
     * (env vars, shared config, SSO, or a profile).
     */
    public static void main(String[] args) {
        OverlayPreferences.setSpeechEnabled(true);
        speak("Elite Dangerous overlay text to speech test.");
    }

    // ------------------------
    // Cache + synth
    // ------------------------

    private static Path getOrCreateCachedWav(String text) {
        String engine = OverlayPreferences.getSpeechEngine();
        String voiceId = OverlayPreferences.getSpeechVoiceId();
        int sampleRate = OverlayPreferences.getSpeechSampleRateHz();

        Path cacheDir = OverlayPreferences.getSpeechCacheDir();
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String key = engine + "|" + voiceId + "|" + sampleRate + "|" + text.trim();
        String hash = sha256Hex(key);
        Path wavPath = cacheDir.resolve(hash + ".wav");

        if (Files.isRegularFile(wavPath)) {
            return wavPath;
        }

        byte[] pcm = synthesizePcm(text, engine, voiceId, sampleRate);
        byte[] wav = wrapPcmAsWav(pcm, sampleRate);

        try {
            Files.write(wavPath, wav);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return wavPath;
    }

    private static byte[] synthesizePcm(String text, String engineStr, String voiceStr, int sampleRate) {
        Region region = Region.of(OverlayPreferences.getSpeechAwsRegion());
        AwsCredentialsProvider creds = buildCredentialsProvider();

        try (PollyClient polly = PollyClient.builder()
            .region(region)
            .credentialsProvider(creds)
            .build()) {

            Engine engine = parseEngine(engineStr);
            VoiceId voice = parseVoice(voiceStr);

            SynthesizeSpeechRequest req = SynthesizeSpeechRequest.builder()
                .engine(engine)
                .voiceId(voice)
                .outputFormat(OutputFormat.PCM)
                .sampleRate(Integer.toString(sampleRate))
                .text(text)
                .build();

            try (ResponseInputStream<SynthesizeSpeechResponse> in = polly.synthesizeSpeech(req)) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static AwsCredentialsProvider buildCredentialsProvider() {
        String profile = OverlayPreferences.getSpeechAwsProfile();
        if (profile != null && !profile.isBlank()) {
            return ProfileCredentialsProvider.builder()
                .profileName(profile.trim())
                .build();
        }
        return DefaultCredentialsProvider.create();
    }

    private static Engine parseEngine(String engineStr) {
        if (engineStr == null) {
            return Engine.STANDARD;
        }
        String v = engineStr.trim().toLowerCase();
        if (v.equals("neural")) {
            return Engine.NEURAL;
        }
        return Engine.STANDARD;
    }

    private static VoiceId parseVoice(String voiceStr) {
        if (voiceStr == null || voiceStr.isBlank()) {
            return VoiceId.JOANNA;
        }
        try {
            return VoiceId.fromValue(voiceStr.trim());
        } catch (Exception e) {
            return VoiceId.JOANNA;
        }
    }

    // ------------------------
    // WAV + playback (no MP3 deps)
    // ------------------------
    


    private static void playWav(Path wavFile) {
        // This is the most reliable way: let AudioSystem open the file itself.
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile.toFile())) {
            playAudioInputStreamBlocking(ais);
        } catch (Exception e) {
			e.printStackTrace();
		}
    }

    private static void playWav(InputStream in) throws Exception {
        // AudioSystem often needs mark/reset; BufferedInputStream provides it.
        try (BufferedInputStream bin = new BufferedInputStream(in);
             AudioInputStream ais = AudioSystem.getAudioInputStream(bin)) {
            playAudioInputStreamBlocking(ais);
        }
    }

    private static void playAudioInputStreamBlocking(AudioInputStream ais) throws Exception {
        Clip clip = AudioSystem.getClip();

        CountDownLatch done = new CountDownLatch(1);
        clip.addLineListener(ev -> {
            if (ev.getType() == LineEvent.Type.STOP || ev.getType() == LineEvent.Type.CLOSE) {
                done.countDown();
            }
        });

        try {
            clip.open(ais);
            clip.start();

            // Wait until playback finishes (prevents “no sound” when program continues/returns).
            done.await();
        } finally {
            clip.stop();
            clip.close();
        }
    }

    /**
     * Polly PCM is 16-bit signed, little-endian, mono.
     */
    private static byte[] wrapPcmAsWav(byte[] pcm, int sampleRate) {
        int numChannels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * numChannels * (bitsPerSample / 8);
        int blockAlign = numChannels * (bitsPerSample / 8);

        int dataSize = pcm.length;
        int riffChunkSize = 36 + dataSize;

        ByteBuffer header = ByteBuffer.allocate(44);
        header.order(ByteOrder.LITTLE_ENDIAN);

        // RIFF
        header.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        header.putInt(riffChunkSize);
        header.put("WAVE".getBytes(StandardCharsets.US_ASCII));

        // fmt
        header.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        header.putInt(16); // PCM fmt chunk size
        header.putShort((short) 1); // audio format = 1 (PCM)
        header.putShort((short) numChannels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);

        // data
        header.put("data".getBytes(StandardCharsets.US_ASCII));
        header.putInt(dataSize);

        byte[] wav = new byte[44 + dataSize];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcm, 0, wav, 44, dataSize);

        return wav;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
