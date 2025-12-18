package org.dce.ed.logreader.sim;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class JournalSimulator {

    private final List<String> sourceLines;
    private int currentIndex;

    private Path outputDir;
    private Path outputFile;

    public JournalSimulator(List<String> sourceLines) {
        this.sourceLines = sourceLines;
        this.currentIndex = 0;
    }

    public void setCurrentIndex(int index) {
        if (index < 0 || index >= sourceLines.size())
            throw new IllegalArgumentException("Invalid index");
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setOutputDirectory(Path dir) throws IOException {
        this.outputDir = dir;
        Files.createDirectories(dir);

        // Keep the same ED-valid journal file for the entire run/session.
        if (this.outputFile == null) {
            this.outputFile = dir.resolve("Journal." + "2025-12-18T004917" + ".01.log");
            if (!Files.exists(this.outputFile)) {
                Files.createFile(this.outputFile);
            }
        }
    }

    private static Path resolveJournalFile(Path dir) {
        // no longer used
        return dir.resolve("Journal.SIMULATOR.log");
    }


    public boolean emitNext() throws IOException {
        if (currentIndex >= sourceLines.size())
            return false;

        String line = sourceLines.get(currentIndex);

        try (BufferedWriter out = Files.newBufferedWriter(
                outputFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            out.write(line);
            out.newLine();
        }

        currentIndex++;
        return true;
    }


//    private static Path resolveJournalFile(Path dir) throws IOException {
//        Files.createDirectories(dir);
//
//        String base =
//                "Journal." +
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss"));
//
//        // ED uses .01, .02, ...; pick the first unused
//        for (int i = 1; i <= 99; i++) {
//            String name = base + String.format(".%02d.log", i);
//            Path candidate = dir.resolve(name);
//            if (!Files.exists(candidate))
//                return candidate;
//        }
//
//        // Extremely unlikely; fall back to overwrite last slot if everything exists
//        return dir.resolve(base + ".99.log");
//    }
}
