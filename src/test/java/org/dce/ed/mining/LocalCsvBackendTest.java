package org.dce.ed.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link LocalCsvBackend}: write then read, missing file, empty file,
 * and {@linkplain LocalCsvBackend#updateRunEndTime(String, int, Instant) run end} placement (must match
 * {@link ProspectorMiningLogPolicy} / Google Sheets behavior: one canonical row only).
 */
class LocalCsvBackendTest {

    @Test
    void appendThenLoad_roundTripsRows(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("log.csv");
        LocalCsvBackend backend = new LocalCsvBackend(csv);
        Instant ts = Instant.parse("2026-02-16T14:30:00Z");
        List<ProspectorLogRow> rows = List.of(
            new ProspectorLogRow(1, "Sol > Earth", ts, "Tritium", 24.5, 10.0, 12.5, 2.5, "Commander One")
        );
        backend.appendRows(rows);
        List<ProspectorLogRow> loaded = backend.loadRows();
         ProspectorLogRow r = loaded.get(0);
        assertEquals(1, r.getRun());
        assertEquals("Sol > Earth", r.getFullBodyName());
        assertEquals("Tritium", r.getMaterial());
        assertEquals(24.5, r.getPercent(), 1e-6);
        assertEquals(10.0, r.getBeforeAmount(), 1e-6);
        assertEquals(12.5, r.getAfterAmount(), 1e-6);
        assertEquals(2.5, r.getDifference(), 1e-6);
        assertEquals("Commander One", r.getCommanderName());
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
        Files.writeString(csv, "run,timestamp,material,percent,before amount,after amount,difference,body,commander\n");
        LocalCsvBackend backend = new LocalCsvBackend(csv);
        List<ProspectorLogRow> loaded = backend.loadRows();
        assertTrue(loaded.isEmpty());
    }

    @Test
    void loadRows_legacy7Column_infersRunFromGaps(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("legacy.csv");
        // Legacy: no "run"/"body" header; 7 columns: timestamp,material,percent,before,after,difference,email
        String content = "2/16/2026 14:30:00,Tritium,24.5,10.0,12.5,2.5,cmdr1@ex.com\n"
            + "2/16/2026 14:35:00,Platinum,10.0,0.0,1.0,1.0,cmdr1@ex.com\n"
            + "2/16/2026 15:00:00,Tritium,20.0,1.0,2.0,1.0,cmdr1@ex.com\n"; // >10 min gap -> run 2
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        LocalCsvBackend backend = new LocalCsvBackend(csv);
        List<ProspectorLogRow> loaded = backend.loadRows();
        assertEquals(3, loaded.size());
        assertEquals(1, loaded.get(0).getRun());
        assertEquals(1, loaded.get(1).getRun());
        assertEquals(2, loaded.get(2).getRun());
        assertEquals("", loaded.get(0).getFullBodyName());
        assertEquals("cmdr1@ex.com", loaded.get(0).getCommanderName());
    }

    @Test
    void updateRunEndTime_writesEndOnlyOnCanonicalRow_preferAsteroidA(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("prospector.csv");
        String header = "run,asteroid,timestamp,material,percent,before amount,after amount,actual,core,body,duds,commander,start time,end time\n";
        // Two materials on A (both with start) + B without start — only first A row should get end time.
        String rA1 = "19,A,4/2/2026 15:19:04,Bromellite,10.00,0.00,1.00,1.00,-,Ring,0,Villunus,4/2/2026 15:19:04,\n";
        String rA2 = "19,A,4/2/2026 15:19:10,Tritium,10.00,0.00,1.00,1.00,-,Ring,0,Villunus,4/2/2026 15:19:04,\n";
        String rB = "19,B,4/2/2026 15:25:00,Bromellite,10.00,0.00,1.00,1.00,-,Ring,0,Villunus,,\n";
        Files.writeString(csv, header + rA1 + rA2 + rB, StandardCharsets.UTF_8);

        LocalCsvBackend backend = new LocalCsvBackend(csv);
        Instant end = Instant.parse("2026-04-02T20:40:27Z");
        backend.updateRunEndTime("Villunus", 19, end);

        List<ProspectorLogRow> loaded = backend.loadRows();
        assertEquals(3, loaded.size());
        assertEquals("Bromellite", loaded.get(0).getMaterial());
        assertEquals("Tritium", loaded.get(1).getMaterial());
        assertEquals("Bromellite", loaded.get(2).getMaterial());
        assertEquals(end, loaded.get(0).getRunEndTime());
        assertNull(loaded.get(1).getRunEndTime());
        assertNull(loaded.get(2).getRunEndTime());
    }
}
