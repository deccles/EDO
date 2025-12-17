package org.dce.ed.tts;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

public class PollyTtsDemo {

    public static void main(String[] args) throws Exception {

        String text = "Hello Dave. This is Amazon Polly speaking from your Java app.";
        speakPcm(text, VoiceId.JOANNA, Engine.NEURAL, Region.US_EAST_1);
    }

    public static void speakPcm(String text, VoiceId voice, Engine engine, Region region)
            throws IOException, LineUnavailableException {

        try (PollyClient polly = PollyClient.builder()
                .region(region)
                .build()) {

            // Polly engine choices include standard/neural/long-form/generative (voice-dependent). :contentReference[oaicite:2]{index=2}
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                    .text(text)
                    .textType(TextType.TEXT)
                    .voiceId(voice)
                    .engine(engine)
                    .outputFormat(OutputFormat.PCM)
                    .sampleRate("16000")
                    .build();

            try (ResponseInputStream<SynthesizeSpeechResponse> audioStream = polly.synthesizeSpeech(request)) {

                AudioFormat format = new AudioFormat(16000f, 16, 1, true, false); // little-endian
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(format);
                    line.start();

                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = audioStream.read(buffer)) != -1) {
                        line.write(buffer, 0, read);
                    }

                    line.drain();
                    line.stop();
                }
            }
        }
    }
}
