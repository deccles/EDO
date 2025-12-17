package org.dce.ed.tts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public final class TtsCacheIndex {

    private static final String INDEX_FILE_NAME = "index.tsv";

    private final Path cacheRootDir;

    public TtsCacheIndex(Path cacheRootDir) {
        this.cacheRootDir = cacheRootDir;
    }

    public Path getVoiceDir(String voiceId) throws IOException {
        if (voiceId == null || voiceId.isBlank()) {
            throw new IllegalArgumentException("voiceId is required");
        }
        Path dir = cacheRootDir.resolve(safeDirName(voiceId));
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Stable filename for (voiceId + phrase). You can change extension based on output format.
     */
    public String buildCacheFileName(String voiceId, String phrase, String extension) {
        if (extension == null || extension.isBlank()) {
            extension = "wav";
        }
        String hex = sha256Hex(voiceId + "\n" + phrase);
        return hex + "." + extension;
    }

    /**
     * Append an index row only when a NEW audio file is created.
     * index.tsv columns:
     *   epochMillis \t filename \t phrase
     */
    public void appendIndexRow(Path voiceDir, String filename, String phrase) throws IOException {
        Files.createDirectories(voiceDir);

        Path indexFile = voiceDir.resolve(INDEX_FILE_NAME);
        String safePhrase = escapeTsv(phrase);
        String line = Instant.now().toEpochMilli() + "\t" + filename + "\t" + safePhrase + "\n";

        // Synchronized on the class to avoid interleaving lines if you generate concurrently.
        synchronized (TtsCacheIndex.class) {
            try (BufferedWriter w = Files.newBufferedWriter(
                    indexFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND)) {
                w.write(line);
            }
        }
    }

    private static String safeDirName(String voiceId) {
        // Keep it filesystem-friendly; you can get stricter if you want.
        return voiceId.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private static String escapeTsv(String s) {
        if (s == null) {
            return "";
        }
        // Keep it 1-line per record.
        return s
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen on a standard JRE.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
