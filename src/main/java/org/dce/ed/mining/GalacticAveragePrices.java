package org.dce.ed.mining;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;

/**
 * A small, static "galactic average" price lookup.
 *
 * This is intentionally offline (no network calls). You can refresh the bundled
 * resource file whenever you want, but for "is this valuable?" decisions a
 * periodically updated static snapshot is good enough.
 */
public final class GalacticAveragePrices {

    private static final String RESOURCE_PATH = "/market/inara_avg_sell.properties";

    private final Map<String, Integer> avgSellCrPerTonByNormalizedName;

    private GalacticAveragePrices(Map<String, Integer> avgSellCrPerTonByNormalizedName) {
        this.avgSellCrPerTonByNormalizedName = Collections.unmodifiableMap(new HashMap<>(avgSellCrPerTonByNormalizedName));
    }

    public static GalacticAveragePrices loadDefault() {
        Properties p = new Properties();
        try (InputStream in = GalacticAveragePrices.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in != null) {
                p.load(in);
            }
        } catch (Exception e) {
            // Ignore and fall back to empty map.
        }

        Map<String, Integer> map = new HashMap<>();
        for (String key : p.stringPropertyNames()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String rawVal = p.getProperty(key);
            if (rawVal == null) {
                continue;
            }
            try {
                int v = Integer.parseInt(rawVal.trim());
                if (v > 0) {
                    map.put(normalizeMaterialName(key), v);
                }
            } catch (Exception e) {
                // Ignore bad values.
            }
        }

        return new GalacticAveragePrices(map);
    }

    public OptionalInt getAvgSellCrPerTon(String journalMaterialName) {
        if (journalMaterialName == null || journalMaterialName.isBlank()) {
            return OptionalInt.empty();
        }
        Integer v = avgSellCrPerTonByNormalizedName.get(normalizeMaterialName(journalMaterialName));
        if (v == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(v);
    }

    /**
     * Normalize material/commodity names so user-friendly keys like
     * "Low Temperature Diamonds" match journal names like "$LowTemperatureDiamonds_Name;".
     */
    public static String normalizeMaterialName(String s) {
        if (s == null) {
            return "";
        }

        String t = s.trim();
        if (t.startsWith("$")) {
            t = t.substring(1);
        }
        t = t.replace("_name", "");
        t = t.replace("_Name", "");
        t = t.replace(";", "");

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }
}
