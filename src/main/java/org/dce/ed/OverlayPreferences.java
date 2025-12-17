package org.dce.ed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import org.dce.ed.logreader.EliteLogFileLocator;

/**
 * Centralized preferences for the overlay, including log directory selection.
 */
public final class OverlayPreferences {

    private static final String KEY_IS_OVERLAY_TRANSPARENT = "overlay.transparent";
    private static final String KEY_LOG_AUTO = "log.autoDetect";
    private static final String KEY_LOG_CUSTOM_DIR = "log.customDir";

    // --- Speech / Polly (new) ---
    private static final String KEY_SPEECH_ENABLED = "speech.enabled";
    private static final String KEY_SPEECH_ENGINE = "speech.engine"; // "standard" or "neural" (we'll default to standard)
    private static final String KEY_SPEECH_VOICE = "speech.voiceId"; // e.g. "Joanna"
    private static final String KEY_SPEECH_REGION = "speech.awsRegion"; // e.g. "us-east-1"
    private static final String KEY_SPEECH_AWS_PROFILE = "speech.awsProfile"; // optional, blank means default chain
    private static final String KEY_SPEECH_CACHE_DIR = "speech.cacheDir";
    private static final String KEY_SPEECH_SAMPLE_RATE = "speech.sampleRate"; // PCM sample rate in Hz (as string)

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

    // ----------------------------
    // Speech / Polly getters/setters
    // ----------------------------

    public static boolean isSpeechEnabled() {
        return PREFS.getBoolean(KEY_SPEECH_ENABLED, false);
    }

    public static void setSpeechEnabled(boolean enabled) {
        PREFS.putBoolean(KEY_SPEECH_ENABLED, enabled);
    }

    public static String getSpeechEngine() {
        // Defaults: as you requested, keep sane hardcoded defaults.
        // Start with "standard" to avoid Neural costs.
        return PREFS.get(KEY_SPEECH_ENGINE, "standard");
    }

    public static void setSpeechEngine(String engine) {
        if (engine == null || engine.isBlank()) {
            engine = "standard";
        }
        PREFS.put(KEY_SPEECH_ENGINE, engine.trim().toLowerCase());
    }

    public static String getSpeechVoiceId() {
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
}
