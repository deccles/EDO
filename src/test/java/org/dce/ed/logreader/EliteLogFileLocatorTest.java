package org.dce.ed.logreader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EliteLogFileLocatorTest {

    @TempDir
    Path tempDir;

    @Test
    void findStatusFile_missing_returnsNull() {
        assertNull(EliteLogFileLocator.findStatusFile(tempDir));
    }

    @Test
    void findStatusFile_present_returnsPath() throws Exception {
        Path f = tempDir.resolve("Status.json");
        Files.writeString(f, "{}", StandardCharsets.UTF_8);
        assertEquals(f, EliteLogFileLocator.findStatusFile(tempDir));
    }

    @Test
    void findStatusFile_nullJournalDir_returnsNull() {
        assertNull(EliteLogFileLocator.findStatusFile(null));
    }

    @Test
    void findCargoFile_present_returnsPath() throws Exception {
        Path f = tempDir.resolve("Cargo.json");
        Files.writeString(f, "{\"Inventory\":[]}", StandardCharsets.UTF_8);
        assertEquals(f, EliteLogFileLocator.findCargoFile(tempDir));
    }

    @Test
    void findCargoFile_missing_returnsNull() {
        assertNull(EliteLogFileLocator.findCargoFile(tempDir));
    }

    @Test
    void findModulesInfoFile_present_returnsPath() throws Exception {
        Path f = tempDir.resolve("ModulesInfo.json");
        Files.writeString(f, "{}", StandardCharsets.UTF_8);
        assertEquals(f, EliteLogFileLocator.findModulesInfoFile(tempDir));
    }

    @Test
    void looksLikeJournalDirectory_falseForEmptyDir() {
        assertFalse(EliteLogFileLocator.looksLikeJournalDirectory(tempDir));
    }

    @Test
    void looksLikeJournalDirectory_trueWhenJournalDotLogPresent() throws Exception {
        Files.createFile(tempDir.resolve("Journal.2026-04-06T120000.01.log"));
        assertTrue(EliteLogFileLocator.looksLikeJournalDirectory(tempDir));
    }

    @Test
    void findDefaultJournalDirectory_returnsNullOrExistingPath() {
        Path p = EliteLogFileLocator.findDefaultJournalDirectory();
        if (p != null) {
            assertTrue(Files.isDirectory(p));
        }
    }
}
