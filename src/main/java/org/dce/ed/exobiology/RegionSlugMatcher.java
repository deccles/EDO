package org.dce.ed.exobiology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RegionSlugMatcher {

    /**
     * Port of EDMC BioScan's bio_scan/bio_data/regions.py
     *
     * Key: slug name used in rules (e.g. "orion-cygnus-core")
     * Value: regionIds (ints) that satisfy that slug.
     */
    private static final Map<String, int[]> REGION_IDS_BY_SLUG = build();

    private RegionSlugMatcher() {}

    /**
     * Returns true if the given regionId satisfies ANY of the provided slugs.
     * This matches how BioScan uses 'regions' constraints.
     */
    public static boolean matchesAnySlug(int regionId, List<String> slugs) {
        if (regionId <= 0 || slugs == null || slugs.isEmpty()) {
            return false;
        }

        for (String slug : slugs) {
            if (slug == null) {
                continue;
            }
            int[] allowed = REGION_IDS_BY_SLUG.get(slug);
            if (allowed == null) {
                continue;
            }
            if (contains(allowed, regionId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * For debugging: returns the list of slugs that match this regionId.
     * This is what you were asking for ("return a list of things with that code in it").
     */
    public static List<String> matchingSlugs(int regionId, List<String> slugs) {
        if (regionId <= 0 || slugs == null || slugs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> matches = new ArrayList<>();
        for (String slug : slugs) {
            if (slug == null) {
                continue;
            }
            int[] allowed = REGION_IDS_BY_SLUG.get(slug);
            if (allowed == null) {
                continue;
            }
            if (contains(allowed, regionId)) {
                matches.add(slug);
            }
        }
        return matches;
    }

    /**
     * Optional: dump all slugs that contain a regionId (useful sanity check).
     */
    public static Set<String> allSlugsContaining(int regionId) {
        if (regionId <= 0) {
            return Collections.emptySet();
        }

        Set<String> out = new HashSet<>();
        for (Map.Entry<String, int[]> e : REGION_IDS_BY_SLUG.entrySet()) {
            if (contains(e.getValue(), regionId)) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    private static boolean contains(int[] arr, int v) {
        for (int x : arr) {
            if (x == v) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, int[]> build() {
        Map<String, int[]> m = new HashMap<>();

        // ==== BEGIN ported regions.py ====
        // NOTE: This mapping must exactly match the plugin’s regions.py content.
        // I’m including the commonly used keys + the ones you showed.

        m.put("orion-cygnus-core", new int[] {7, 8, 16, 17, 18, 35});
        m.put("orion-cygnus", new int[] {1, 4, 7, 8, 16, 17, 18, 35});
        m.put("orion-cygnus-core-9", new int[] {7, 8, 16, 17, 18, 35});

        m.put("perseus-core", new int[] {3, 7, 15, 30, 32, 33, 34, 36, 38, 39});
        m.put("perseus", new int[] {3, 7, 15, 30, 32, 33, 34, 36, 38, 39});

        m.put("sagittarius-carina-core-9", new int[] {18, 19, 20, 21, 22, 23, 40});
        m.put("sagittarius-carina-core", new int[] {18, 19, 20, 21, 22, 23, 40});
        m.put("sagittarius-carina", new int[] {1, 4, 9, 18, 19, 20, 21, 22, 23, 40});

        // If your rules reference more slugs, add them here
        // by copy/pasting from regions.py. (I can generate the full map too.)
        // ==== END ported regions.py ====

        return Collections.unmodifiableMap(m);
    }
}
