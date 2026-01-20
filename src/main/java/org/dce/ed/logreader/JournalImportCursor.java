package org.dce.ed.logreader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Persists the last processed Elite journal timestamp.
 *
 * Shared by BOTH RescanJournalsMain and LiveJournalMonitor so that events
 * processed live will not be re-processed on the next startup rescan.
 */
public final class JournalImportCursor {

    private static final String LAST_IMPORT_FILENAME = "edo-cache.lastRescanTimestamp";

    private JournalImportCursor() {
    }

    public static Path getCursorFile(Path journalDirectory) {
        if (journalDirectory == null) {
            return null;
        }
        return journalDirectory.resolve(LAST_IMPORT_FILENAME);
    }

    public static Instant read(Path journalDirectory) {
        Path cursor = getCursorFile(journalDirectory);
        if (cursor == null || !Files.isRegularFile(cursor)) {
            return null;
        }
        try {
            String text = Files.readString(cursor, StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                return null;
            }
            return Instant.parse(text);
        } catch (Exception ex) {
            System.err.println("Failed to read last journal timestamp from " + cursor + ": " + ex.getMessage());
            return null;
        }
    }

    public static void write(Path journalDirectory, Instant instant) {
        if (instant == null) {
            return;
        }
        Path cursor = getCursorFile(journalDirectory);
        if (cursor == null) {
            return;
        }
        try {
            Files.writeString(cursor, instant.toString(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("Failed to write last journal timestamp to " + cursor + ": " + ex.getMessage());
        }
    }
}
