package org.dce.ed.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link LocalCsvBackend}: write then read, missing file, empty file.
 */
class LocalCsvBackendTest {

    @Test
    void appendThenLoad_roundTripsRows(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("log.csv");
        LocalCsvBackend backend = new LocalCsvBackend(csv);
        Instant ts = Instant.parse("2026-02-16T14:30:00Z");
        List<ProspectorLogRow> rows = List.of(
            new ProspectorLogRow(1, "Sol > Earth", ts, "Tritium", 24.5, 10.0, 12.5, 2.5, "user@example.com")
        );
        backend.appendRows(rows);
        List<ProspectorLogRow> loaded = backend.loadRows();
        assertEquals(1, loaded.size());
        ProspectorLogRow r = loaded.get(0);
        assertEquals(1, r.getRun());
        assertEquals("Sol > Earth", r.getFullBodyName());
        assertEquals("Tritium", r.getMaterial());
        assertEquals(24.5, r.getPercent(), 1e-6);
        assertEquals(10.0, r.getBeforeAmount(), 1e-6);
        assertEquals(12.5, r.getAfterAmount(), 1e-6);
        assertEquals(2.5, r.getDifference(), 1e-6);
        assertEquals("user@example.com", r.getEmailAddress());
    }

    @Test
    void loadRows_missingFile_returnsEmpty(@TempDir Path dir) {
        LocalCsvBackend backend = new LocalCsvBackend(dir.resolve("nonexistent.csv"));
        List<ProspectorLogRow> loaded = backend.loadRows();
        assertTrue(loaded.isEmpty());
    }

    @Test
    void loadRows_emptyFile_returnsEmpty(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("empty.csv");
        Files.writeString(csv, "run,body,timestamp,material,percent,before amount,after amount,difference,email address\n");
        LocalCsvBackend backend = new LocalCsvBackend(csv);
        List<ProspectorLogRow> loaded = backend.loadRows();
        assertTrue(loaded.isEmpty());
    }
}
