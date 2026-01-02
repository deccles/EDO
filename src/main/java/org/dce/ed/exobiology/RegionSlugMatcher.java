package org.dce.ed.exobiology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Region slug matcher.
 *
 * BioScan supports region constraints like:
 *   "regions": ["orion-cygnus-core"]
 * and negated region constraints like:
 *   "regions": ["!scutum-centaurus"]
 *
 * A positive slug matches if the current regionId is in that slug's allowed list.
 * A negated slug matches if the current regionId is NOT in that slug's allowed list.
 */
public final class RegionSlugMatcher {

    private static final Map<String, int[]> REGION_IDS_BY_SLUG = build();

    private RegionSlugMatcher() {}

    /**
     * Returns true if the given regionId satisfies ANY of the provided slugs.
     * Supports BioScan-style negation via leading '!'.
     */
    public static boolean matchesAnySlug(int regionId, List<String> slugs) {
        if (regionId <= 0 || slugs == null || slugs.isEmpty()) {
            return false;
        }

        for (String slugRaw : slugs) {
            if (slugRaw == null) {
                continue;
            }

            String slug = slugRaw.trim();
            if (slug.isEmpty()) {
                continue;
            }

            boolean negated = slug.startsWith("!");
            String key = negated ? slug.substring(1).trim() : slug;

            if (key.isEmpty()) {
                continue;
            }

            int[] allowed = REGION_IDS_BY_SLUG.get(key);

            // Back-compat: if someone accidentally built the map with the '!' included.
            if (allowed == null) {
                allowed = REGION_IDS_BY_SLUG.get(slug);
            }

            if (allowed == null) {
                continue;
            }

            boolean inAllowed = contains(allowed, regionId);

            if (!negated) {
                if (inAllowed) {
                    return true;
                }
            } else {
                if (!inAllowed) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * For debugging: returns the list of slugs that match this regionId.
     * (Includes the original slug strings, including leading '!' when applicable.)
     */
    public static List<String> matchingSlugs(int regionId, List<String> slugs) {
        if (regionId <= 0 || slugs == null || slugs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> matches = new ArrayList<>();
        for (String slugRaw : slugs) {
            if (slugRaw == null) {
                continue;
            }

            String slug = slugRaw.trim();
            if (slug.isEmpty()) {
                continue;
            }

            boolean negated = slug.startsWith("!");
            String key = negated ? slug.substring(1).trim() : slug;

            if (key.isEmpty()) {
                continue;
            }

            int[] allowed = REGION_IDS_BY_SLUG.get(key);
            if (allowed == null) {
                allowed = REGION_IDS_BY_SLUG.get(slug);
            }
            if (allowed == null) {
                continue;
            }

            boolean inAllowed = contains(allowed, regionId);
            if (!negated) {
                if (inAllowed) {
                    matches.add(slugRaw);
                }
            } else {
                if (!inAllowed) {
                    matches.add(slugRaw);
                }
            }
        }

        return matches;
    }

    /**
     * Optional: dump all non-negated slugs that contain a regionId (useful sanity check).
     */
    public static Set<String> allSlugsContaining(int regionId) {
        if (regionId <= 0) {
            return Collections.emptySet();
        }

        Set<String> out = new HashSet<>();
        for (Map.Entry<String, int[]> e : REGION_IDS_BY_SLUG.entrySet()) {
            String slug = e.getKey();
            int[] allowed = e.getValue();
            if (allowed == null) {
                continue;
            }
            if (contains(allowed, regionId)) {
                out.add(slug);
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

        // Ported from BioScan: src/bio_scan/bio_data/regions.py (region_map)
        m.put("orion-cygnus", new int[] {1, 4, 7, 8, 16, 17, 18, 35});
        m.put("orion-cygnus-1", new int[] {4, 7, 8, 16, 17, 18, 35});
        m.put("orion-cygnus-core", new int[] {7, 8, 16, 17, 18, 35});
        m.put("sagittarius-carina", new int[] {1, 4, 9, 18, 19, 20, 21, 22, 23, 40});
        m.put("sagittarius-carina-core", new int[] {9, 18, 19, 20, 21, 22, 23, 40});
        m.put("sagittarius-carina-core-9", new int[] {18, 19, 20, 21, 22, 23, 40});
        m.put("scutum-centaurus", new int[] {1, 4, 9, 10, 11, 12, 24, 25, 26, 42, 28});
        m.put("scutum-centaurus-core", new int[] {9, 10, 11, 12, 24, 25, 26, 42, 28});
        m.put("outer", new int[] {1, 2, 5, 6, 13, 14, 27, 29, 31, 41, 37});
        m.put("perseus", new int[] {1, 3, 7, 15, 30, 32, 33, 34, 36, 38, 39});
        m.put("perseus-core", new int[] {3, 7, 15, 30, 32, 33, 34, 36, 38, 39});
        m.put("exterior", new int[] {14, 21, 22, 23, 24, 25, 26, 27, 28, 29, 31, 34, 36, 37, 38, 39, 40, 41, 42});
        m.put("anemone-a", new int[] {7, 8, 13, 14, 15, 16, 17, 18, 27, 32});
        m.put("amphora", new int[] {10, 19, 20, 21, 22});
        m.put("brain-tree", new int[] {2, 9, 10, 17, 18, 35});
        m.put("empyrean-straits", new int[] {2});
        m.put("center", new int[] {1, 2, 3});

        return Collections.unmodifiableMap(m);
    }
}
