package org.dce.ed;

import java.awt.Color;
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

    private static final String KEY_IS_OVERLAY_TRANSPARENT = "overlay.transparent"; // legacy
    private static final String KEY_OVERLAY_BG_RGB = "overlay.bg.rgb";
    private static final String KEY_OVERLAY_TRANSPARENCY_PERCENT = "overlay.transparency.percent";
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
    private static final String KEY_MINING_PROSPECTOR_MIN_AVG_VALUE = "mining.prospector.minAvgValueCrPerTon"; // credits/ton

    // Mining value estimation (Mining tab)
    private static final String KEY_MINING_EST_TONS_LOW = "mining.estimate.tons.low";
    private static final String KEY_MINING_EST_TONS_MED = "mining.estimate.tons.medium";
    private static final String KEY_MINING_EST_TONS_HIGH = "mining.estimate.tons.high";
    private static final String KEY_MINING_EST_TONS_CORE = "mining.estimate.tons.core";

    // Reuse the same prefs node as OverlayFrame so everything is in one place.
    private static final Preferences PREFS = Preferences.userNodeForPackage(OverlayFrame.class);

    private OverlayPreferences() {
    }

    /**
     * Legacy helper (kept so older callers compile).
     * True when the overlay transparency is set to 100% (fully transparent).
     */
    public static boolean isOverlayTransparent() {
        return getOverlayTransparencyPercent() >= 100;
    }

    /**
     * Legacy helper (kept so older callers compile).
     * True => 100% transparent, False => 0% transparent (fully opaque).
     */
    public static void setOverlayTransparent(boolean transparent) {
        setOverlayTransparencyPercent(transparent ? 100 : 0);
    }

    public static Color getOverlayBackgroundColor() {
        // Default: black (RGB only).
        String raw = PREFS.get(KEY_OVERLAY_BG_RGB, null);
        int rgb;
        if (raw == null) {
            rgb = 0x000000;
        } else {
            try {
                rgb = Integer.parseInt(raw);
            } catch (Exception e) {
                rgb = 0x000000;
            }
        }
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static void setOverlayBackgroundColor(Color color) {
        if (color == null) {
            color = Color.black;
        }
        int rgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        PREFS.putInt(KEY_OVERLAY_BG_RGB, rgb);
    }

    /**
     * 0% means fully opaque, 100% means fully transparent.
     */
    public static int getOverlayTransparencyPercent() {
        // Migration: if the new key is not present, fall back to the old boolean.
        String raw = PREFS.get(KEY_OVERLAY_TRANSPARENCY_PERCENT, null);
        int p;
        if (raw == null) {
            boolean legacy = PREFS.getBoolean(KEY_IS_OVERLAY_TRANSPARENT, true);
            p = legacy ? 100 : 0;
        } else {
            try {
                p = Integer.parseInt(raw.trim());
            } catch (Exception e) {
                p = 100;
            }
        }
        if (p < 0) {
            p = 0;
        }
        if (p > 100) {
            p = 100;
        }
        return p;
    }

    public static void setOverlayTransparencyPercent(int transparencyPercent) {
        int p = transparencyPercent;
        if (p < 0) {
            p = 0;
        }
        if (p > 100) {
            p = 100;
        }
        PREFS.putInt(KEY_OVERLAY_TRANSPARENCY_PERCENT, p);
    }

    public static Color buildOverlayBackgroundColor(Color baseRgb, int transparencyPercent) {
        if (baseRgb == null) {
            baseRgb = Color.black;
        }
        int p = transparencyPercent;
        if (p < 0) {
            p = 0;
        }
        if (p > 100) {
            p = 100;
        }
        int alpha = (int) Math.round(255.0 * (100.0 - p) / 100.0);
        if (alpha < 0) {
            alpha = 0;
        }
        if (alpha > 255) {
            alpha = 255;
        }
        return new Color(baseRgb.getRed(), baseRgb.getGreen(), baseRgb.getBlue(), alpha);
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

    /**
     * Minimum "galactic average" value (credits/ton) required for a material to count as "valuable".
     *
     * This is used for ProspectedAsteroid announcements when enabled.
     */
    public static int getProspectorMinAvgValueCrPerTon() {
        String s = PREFS.get(KEY_MINING_PROSPECTOR_MIN_AVG_VALUE, "28000");
        try {
            int v = Integer.parseInt(s.trim());
            if (v < 0) {
                v = 0;
            }
            return v;
        } catch (Exception e) {
            return 28000;
        }
    }

    public static void setProspectorMinAvgValueCrPerTon(int creditsPerTon) {
        if (creditsPerTon < 0) {
            creditsPerTon = 0;
        }
        PREFS.put(KEY_MINING_PROSPECTOR_MIN_AVG_VALUE, Integer.toString(creditsPerTon));
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

    // ----------------------------
    // Mining value estimation (Mining tab)
    // ----------------------------

    /**
     * Estimated total collectible tons for a prospected asteroid with Content=Low.
     */
    public static double getMiningEstimateTonsLow() {
        return getDoubleClamped(KEY_MINING_EST_TONS_LOW, 8.0, 0.0, 200.0);
    }

    public static void setMiningEstimateTonsLow(double tons) {
        putDoubleClamped(KEY_MINING_EST_TONS_LOW, tons, 0.0, 200.0);
    }

    /**
     * Estimated total collectible tons for a prospected asteroid with Content=Medium.
     */
    public static double getMiningEstimateTonsMedium() {
        return getDoubleClamped(KEY_MINING_EST_TONS_MED, 16.0, 0.0, 200.0);
    }

    public static void setMiningEstimateTonsMedium(double tons) {
        putDoubleClamped(KEY_MINING_EST_TONS_MED, tons, 0.0, 200.0);
    }

    /**
     * Estimated total collectible tons for a prospected asteroid with Content=High.
     */
    public static double getMiningEstimateTonsHigh() {
        return getDoubleClamped(KEY_MINING_EST_TONS_HIGH, 25.0, 0.0, 200.0);
    }

    public static void setMiningEstimateTonsHigh(double tons) {
        putDoubleClamped(KEY_MINING_EST_TONS_HIGH, tons, 0.0, 200.0);
    }

    /**
     * Estimated tons yielded by a core (MotherlodeMaterial) asteroid.
     */
    public static double getMiningEstimateTonsCore() {
        return getDoubleClamped(KEY_MINING_EST_TONS_CORE, 12.0, 0.0, 200.0);
    }

    public static void setMiningEstimateTonsCore(double tons) {
        putDoubleClamped(KEY_MINING_EST_TONS_CORE, tons, 0.0, 200.0);
    }

    private static double getDoubleClamped(String key, double def, double min, double max) {
        String s = PREFS.get(key, Double.toString(def));
        try {
            double v = Double.parseDouble(s.trim());
            if (v < min) {
                v = min;
            }
            if (v > max) {
                v = max;
            }
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    private static void putDoubleClamped(String key, double v, double min, double max) {
        if (v < min) {
            v = min;
        }
        if (v > max) {
            v = max;
        }
        PREFS.put(key, Double.toString(v));
    }

}
