package org.dce.ed;

import org.dce.ed.logreader.EliteLogFileLocator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

/**
 * Centralized preferences for the overlay, including log directory selection.
 */
public final class OverlayPreferences {

    private static final String KEY_IS_OVERLAY_TRANSPARENT = "overlay.transparent";
    private static final String KEY_LOG_AUTO = "log.autoDetect";
    private static final String KEY_LOG_CUSTOM_DIR = "log.customDir";

    // Reuse the same prefs node as OverlayFrame so everything is in one place.
    private static final Preferences PREFS = Preferences.userNodeForPackage(OverlayFrame.class);

    private OverlayPreferences() {
    }

    public static boolean isOverlayTransparent() {
        boolean b = PREFS.getBoolean(KEY_IS_OVERLAY_TRANSPARENT, true);	
        return b;
    }
    public static void setOverlayTransparent(boolean transparent) {
        PREFS.putBoolean(KEY_IS_OVERLAY_TRANSPARENT, transparent);	
    }
    public static boolean isAutoLogDir() {
        return PREFS.getBoolean(KEY_LOG_AUTO, true);
    }

    public static void setAutoLogDir(boolean auto) {
        PREFS.putBoolean(KEY_LOG_AUTO, auto);
    }

    public static String getCustomLogDir() {
        return PREFS.get(KEY_LOG_CUSTOM_DIR, "");
    }

    public static void setCustomLogDir(String path) {
        if (path == null) {
            path = "";
        }
        PREFS.put(KEY_LOG_CUSTOM_DIR, path);
    }

    /**
     * Resolve the journal directory based on preferences:
     * - If "auto" is enabled, use the default journal folder.
     * - Otherwise, try the custom path; if it looks valid, use it.
     * - If custom is invalid, fall back to the default journal folder.
     */
    public static Path resolveJournalDirectory() {
        if (isAutoLogDir()) {
            return EliteLogFileLocator.findDefaultJournalDirectory();
        }

        String custom = getCustomLogDir();
        if (custom != null && !custom.isBlank()) {
            Path p = Paths.get(custom.trim());
            if (Files.isDirectory(p) && EliteLogFileLocator.looksLikeJournalDirectory(p)) {
                return p;
            }
        }

        // Fallback so we don't completely break if the custom dir is bad
        return EliteLogFileLocator.findDefaultJournalDirectory();
    }
}
