package org.dce.ed;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimum distance (meters) required between exobiology samples per genus.
 *
 * Keyed by Genus (first token in the display name).
 */
final class BioColonyDistance {

    private static final Map<String, Integer> METERS_BY_GENUS = new HashMap<>();

    static {
        // Common Odyssey exobiology distances (meters).
        // Keep genus keys lower-cased.
        put("bacterium", 500);
        put("stratum", 500);
        put("tussock", 200);
        put("fungoida", 300);
        put("cactoida", 300);
        put("frutexa", 300);
        put("tubus", 300);
        put("osseo", 300);
        put("concha", 300);
        put("recepta", 300);
        put("aleoida", 300);
        put("electricae", 1000);
        put("fonticulua", 500);
        put("fumerola", 100);
        put("crystalline", 1000);
    }

    private static void put(String genus, int meters) {
        METERS_BY_GENUS.put(genus, Integer.valueOf(meters));
    }

    private BioColonyDistance() {
        // static
    }

    public static int metersForBio(String displayNameOrKey) {
        if (displayNameOrKey == null || displayNameOrKey.isBlank()) {
            return 0;
        }

        String s = displayNameOrKey.trim();
        int sp = s.indexOf(' ');
        String genus = (sp > 0) ? s.substring(0, sp) : s;
        genus = genus.toLowerCase(Locale.ROOT);

        Integer v = METERS_BY_GENUS.get(genus);
        return (v == null) ? 0 : v.intValue();
    }
}
