package org.dce.ed.logreader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates the Elite Dangerous journal directory on disk.
 *
 * Main target:
 *   Windows: %USERPROFILE%\Saved Games\Frontier Developments\Elite Dangerous
 *
 * Also includes a couple of common Proton paths (best-effort).
 */
public final class EliteLogFileLocator {

    private EliteLogFileLocator() {
    }

    /**
     * Try to find the default journal directory.
     *
     * @return a Path if found, or null if we couldn't locate anything.
     */
    public static Path findDefaultJournalDirectory() {
        // 1. Windows-style Saved Games using USERPROFILE
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isBlank()) {
            Path candidate = Paths.get(userProfile,
                    "Saved Games",
                    "Frontier Developments",
                    "Elite Dangerous");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }

        // 2. Generic Saved Games using user.home (works on some setups / WINE / Proton)
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            Path candidate = Paths.get(home,
                    "Saved Games",
                    "Frontier Developments",
                    "Elite Dangerous");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }

            // 3. Common Proton/Wine-style path (best effort; may not exist)
            Path protonCandidate = Paths.get(home,
                    ".steam", "steam", "steamapps", "compatdata",
                    "359320", "pfx", "drive_c", "users", "steamuser",
                    "Saved Games", "Frontier Developments", "Elite Dangerous");
            if (Files.isDirectory(protonCandidate)) {
                return protonCandidate;
            }
        }

        // 4. Nothing found
        return null;
    }

    /**
     * Convenience helper for Status.json in the same folder as the journals.
     */
    public static Path findStatusFile(Path journalDirectory) {
        if (journalDirectory == null) {
            return null;
        }
        Path status = journalDirectory.resolve("Status.json");
        return Files.isRegularFile(status) ? status : null;
    }


    /**
     * Convenience helper for Cargo.json in the same folder as the journals.
     */
    public static Path findCargoFile(Path journalDirectory) {
        if (journalDirectory == null) {
            return null;
        }
        Path cargo = journalDirectory.resolve("Cargo.json");
        return Files.isRegularFile(cargo) ? cargo : null;
    }

    /**
     * Convenience helper for ModulesInfo.json in the same folder as the journals.
     */
    public static Path findModulesInfoFile(Path journalDirectory) {
        if (journalDirectory == null) {
            return null;
        }
        Path modules = journalDirectory.resolve("ModulesInfo.json");
        return Files.isRegularFile(modules) ? modules : null;
    }

    /**
     * Quick check for whether the given directory looks like a journal folder.
     */
    public static boolean looksLikeJournalDirectory(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        File[] files = dir.toFile().listFiles((d, name) -> name.startsWith("Journal.") && name.endsWith(".log"));
        return files != null && files.length > 0;
    }
}
