package org.dce.ed;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimum distance (meters) required between exobiology samples (CCR) per genus.
 *
 * Keyed by genus (lower-cased). We also include a few aliases to handle different naming
 * sources (e.g., "tubas" vs "tubus").
 */
final class BioColonyDistance {

    private static final Map<String, Integer> METERS_BY_GENUS = new HashMap<>();

    static {
        // Ground exobiology (Vista Genomics) CCRs.
        put("aleoida", 150);
        put("bacterium", 500);
        put("cactoida", 300);
        put("clypeus", 150);
        put("concha", 150);
        put("electricae", 1000);
        put("fonticulua", 500);
        put("frutexa", 150);
        put("fumerola", 100);
        put("fungoida", 300);
        put("osseus", 800);
        put("recepta", 150);
        put("stratum", 500);
        put("tussock", 200);
        put("tubus", 800);

        // Other Odyssey bio categories that show CCR in common lists.
        put("amphora", 100);
        put("anemone", 100);
        put("tuber", 100);

        // Crystalline Shards show up in the same CCR tables; keep supported.
        put("crystalline", 100);
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
