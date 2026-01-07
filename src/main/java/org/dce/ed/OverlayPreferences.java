package org.dce.ed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import org.dce.ed.logreader.EliteLogFileLocator;

import software.amazon.awssdk.services.polly.model.Engine;

/**
 * Centralized preferences for the overlay, including log directory selection.
 */
public final class OverlayPreferences {

    private static final String KEY_IS_OVERLAY_TRANSPARENT = "overlay.transparent";
    private static final String KEY_LOG_AUTO = "log.autoDetect";
    private static final String KEY_LOG_CUSTOM_DIR = "log.customDir";

    private static final String KEY_UI_FONT_NAME = "ui.font.name";
    private static final String KEY_UI_FONT_SIZE = "ui.font.size";

    // --- Speech / Polly (new) ---
    private static final String KEY_SPEECH_ENABLED = "speech.enabled";
    private static final String KEY_SPEECH_ENGINE = "speech.engine"; // "standard" or "neural" (we'll default to standard)
    private static final String KEY_SPEECH_VOICE = "speech.voiceId"; // e.g. "Joanna"
    private static final String KEY_SPEECH_REGION = "speech.awsRegion"; // e.g. "us-east-1"
    private static final String KEY_SPEECH_AWS_PROFILE = "speech.awsProfile"; // optional, blank means default chain
    private static final String KEY_SPEECH_CACHE_DIR = "speech.cacheDir";
    private static final String KEY_SPEECH_SAMPLE_RATE = "speech.sampleRate"; // PCM sample rate in Hz (as string)

    // --- Mining / Prospector ---
    private static final String KEY_MINING_PROSPECTOR_MATERIALS = "mining.prospector.materials"; // comma-separated
    private static final String KEY_MINING_PROSPECTOR_MIN_PROP = "mining.prospector.minProportion"; // percent

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

    public static boolean isAutoLogDir(String clientKey) {
        return PREFS.getBoolean(KEY_LOG_AUTO + "." +clientKey, true);
    }

    public static void setAutoLogDir(String clientKey, boolean auto) {
        PREFS.putBoolean(KEY_LOG_AUTO + "." +clientKey, auto);
    }

    public static String getCustomLogDir(String clientKey) {
        return PREFS.get(KEY_LOG_CUSTOM_DIR + "." +clientKey, "");
    }

    public static void setCustomLogDir(String clientKey, String path) {
        if (path == null) {
            path = "";
        }
        PREFS.put(KEY_LOG_CUSTOM_DIR + "." +clientKey, path);
    }

    /**
     * Resolve the journal directory based on preferences:
     * - If "auto" is enabled, use the default journal folder.
     * - Otherwise, try the custom path; if it looks valid, use it.
     * - If custom is invalid, fall back to the default journal folder.
     */
    public static Path resolveJournalDirectory(String clientKey) {
        if (isAutoLogDir(clientKey)) {
            return EliteLogFileLocator.findDefaultJournalDirectory();
        }

        String custom = getCustomLogDir(clientKey);
        if (custom != null && !custom.isBlank()) {
            Path p = Paths.get(custom.trim());
            if (Files.isDirectory(p) && EliteLogFileLocator.looksLikeJournalDirectory(p)) {
                return p;
            }
        }

        // Fallback so we don't completely break if the custom dir is bad
        return EliteLogFileLocator.findDefaultJournalDirectory();
    }

    // ----------------------------
    // Speech / Polly getters/setters
    // ----------------------------

    public static boolean isSpeechEnabled() {
        return PREFS.getBoolean(KEY_SPEECH_ENABLED, false);
    }

    public static void setSpeechEnabled(boolean enabled) {
        PREFS.putBoolean(KEY_SPEECH_ENABLED, enabled);
    }

    public static Engine getSpeechEngine() {
        // Defaults: as you requested, keep sane hardcoded defaults.
        // Start with "standard" to avoid Neural costs.
        return Engine.fromValue(PREFS.get(KEY_SPEECH_ENGINE, "standard"));
    }

    public static void setSpeechEngine(String engine) {
        if (engine == null || engine.isBlank()) {
            engine = "standard";
        }
        PREFS.put(KEY_SPEECH_ENGINE, engine.trim().toLowerCase());
    }

    public static String getSpeechVoiceName() {
        // Default voice: "Joanna" (standard, decent baseline)
        return PREFS.get(KEY_SPEECH_VOICE, "Joanna");
    }

    public static void setSpeechVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            voiceId = "Joanna";
        }
        PREFS.put(KEY_SPEECH_VOICE, voiceId.trim());
    }

    public static String getSpeechAwsRegion() {
        // Default: us-east-1
        return PREFS.get(KEY_SPEECH_REGION, "us-east-1");
    }

    public static void setSpeechAwsRegion(String region) {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
        PREFS.put(KEY_SPEECH_REGION, region.trim());
    }

    public static String getSpeechAwsProfile() {
        // Optional. Blank => DefaultCredentialsProvider chain.
        return PREFS.get(KEY_SPEECH_AWS_PROFILE, "");
    }

    public static void setSpeechAwsProfile(String profile) {
        if (profile == null) {
            profile = "";
        }
        PREFS.put(KEY_SPEECH_AWS_PROFILE, profile.trim());
    }

    public static Path getSpeechCacheDir() {
        String defaultDir = Paths.get(System.getProperty("user.home"), ".edo", "tts-cache").toString();
        String configured = PREFS.get(KEY_SPEECH_CACHE_DIR, defaultDir);
        return Paths.get(configured);
    }

    public static void setSpeechCacheDir(String dir) {
        if (dir == null || dir.isBlank()) {
            dir = Paths.get(System.getProperty("user.home"), ".edo", "tts-cache").toString();
        }
        PREFS.put(KEY_SPEECH_CACHE_DIR, dir.trim());
    }

    public static int getSpeechSampleRateHz() {
        String s = PREFS.get(KEY_SPEECH_SAMPLE_RATE, "16000");
        try {
            int hz = Integer.parseInt(s.trim());
            if (hz < 8000) {
                hz = 8000;
            }
            return hz;
        } catch (Exception e) {
            return 16000;
        }
    }

    public static void setSpeechSampleRateHz(int sampleRateHz) {
        if (sampleRateHz < 8000) {
            sampleRateHz = 8000;
        }
        PREFS.put(KEY_SPEECH_SAMPLE_RATE, Integer.toString(sampleRateHz));
    }

    // ----------------------------
    // Mining / Prospector
    // ----------------------------

    /**
     * Comma-separated list of materials to announce for ProspectedAsteroid events.
     * Leave blank to announce any material meeting the threshold.
     */
    public static String getProspectorMaterialsCsv() {
        return PREFS.get(KEY_MINING_PROSPECTOR_MATERIALS, "").trim();
    }

    public static void setProspectorMaterialsCsv(String csv) {
        if (csv == null) {
            csv = "";
        }
        PREFS.put(KEY_MINING_PROSPECTOR_MATERIALS, csv.trim());
    }

    /**
     * Minimum material proportion (percent) required to trigger an announcement.
     */
    public static double getProspectorMinProportionPercent() {
        String s = PREFS.get(KEY_MINING_PROSPECTOR_MIN_PROP, "20");
        try {
            double v = Double.parseDouble(s.trim());
            if (v < 0.0) {
                v = 0.0;
            }
            if (v > 100.0) {
                v = 100.0;
            }
            return v;
        } catch (Exception e) {
            return 20.0;
        }
    }

    public static void setProspectorMinProportionPercent(double percent) {
        if (percent < 0.0) {
            percent = 0.0;
        }
        if (percent > 100.0) {
            percent = 100.0;
        }
        PREFS.put(KEY_MINING_PROSPECTOR_MIN_PROP, Double.toString(percent));
    }

    // --- UI Font (System / Route / Biology) ---

    /**
     * Font family name used across major panels (System / Route / Biology).
     * Default matches SystemTabPanel's historical font choice.
     */
    public static String getUiFontName() {
        return PREFS.get(KEY_UI_FONT_NAME, "Segoe UI");
    }

    public static void setUiFontName(String fontName) {
        if (fontName == null || fontName.isBlank()) {
            fontName = "Segoe UI";
        }
        PREFS.put(KEY_UI_FONT_NAME, fontName.trim());
    }

    /**
     * Base font size (points) used across major panels.
     * Default matches SystemTabPanel's historical font size.
     */
    public static int getUiFontSize() {
        try {
            int sz = Integer.parseInt(PREFS.get(KEY_UI_FONT_SIZE, "17"));
            if (sz < 8) {
                sz = 8;
            }
            if (sz > 72) {
                sz = 72;
            }
            return sz;
        } catch (Exception e) {
            return 17;
        }
    }

    public static void setUiFontSize(int size) {
        if (size < 8) {
            size = 8;
        }
        if (size > 72) {
            size = 72;
        }
        PREFS.put(KEY_UI_FONT_SIZE, Integer.toString(size));
    }

    /**
     * Convenience: returns the configured UI font. If the requested family is
     * unavailable on the current system, Java will substitute.
     */
    public static java.awt.Font getUiFont() {
        String name = getUiFontName();
        int size = getUiFontSize();
        return new java.awt.Font(name, java.awt.Font.PLAIN, size);
    }

}
