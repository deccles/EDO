package org.dce.ed.logreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locates the Elite Dangerous journal directory on disk.
 *
 * Main target:
 *   Windows: %USERPROFILE%\Saved Games\Frontier Developments\Elite Dangerous
 *
 * Notes:
 * - On some Windows installs, the "Saved Games" known folder is redirected
 *   (commonly into OneDrive). In that case, %USERPROFILE%\Saved Games may not exist.
 *   We try the registry-based known-folder location first.
 */
public final class EliteLogFileLocator {

    private EliteLogFileLocator() {
    }

    public static Path findDefaultJournalDirectory() {
        // 0) Windows Known Folder (registry): Saved Games may be redirected (OneDrive, etc.)
        if (isWindows()) {
            Path savedGames = findWindowsSavedGamesDirectory();
            if (savedGames != null) {
                Path candidate = savedGames
                        .resolve("Frontier Developments")
                        .resolve("Elite Dangerous");
                if (Files.isDirectory(candidate)) {
                    return candidate;
                }
            }
        }

        // 1) Windows-style Saved Games using USERPROFILE
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

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).contains("win");
    }

    private static Path findWindowsSavedGamesDirectory() {
        // Saved Games known folder GUID
        String guid = "{4C5C32FF-BB9D-43B0-B5B4-2D72E54EAAA4}";

        String[] keys = new String[] {
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders"
        };

        String[] values = new String[] {
                guid,
                "Saved Games"
        };

        for (String key : keys) {
            for (String value : values) {
                String raw = regQueryValue(key, value);
                if (raw == null || raw.isBlank()) {
                    continue;
                }

                String expanded = expandEnvVars(raw.trim());
                try {
                    Path p = Paths.get(expanded);
                    if (Files.isDirectory(p)) {
                        return p;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private static String regQueryValue(String key, String valueName) {
        try {
            Process p = new ProcessBuilder("reg", "query", key, "/v", valueName)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Typical output line:
                    // <valueName>    REG_EXPAND_SZ    C:\Users\...\Saved Games
                    String trimmed = line.trim();
                    if (!trimmed.toLowerCase(Locale.ROOT).startsWith(valueName.toLowerCase(Locale.ROOT))) {
                        continue;
                    }

                    String[] parts = trimmed.split("\\s+", 3);
                    if (parts.length == 3) {
                        return parts[2];
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String expandEnvVars(String path) {
        if (path == null) {
            return null;
        }

        // Expand %VAR% occurrences using the environment.
        Pattern p = Pattern.compile("%([A-Za-z_][A-Za-z0-9_]*)%");
        Matcher m = p.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String val = System.getenv(var);
            if (val == null) {
                val = m.group(0); // keep original token
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        m.appendTail(sb);

        return sb.toString();
    }
}
