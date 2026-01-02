package org.dce.ed.exobiology;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Port of EDMC BioScan's "nebula" + "guardian" system checks.
 *
 * BioScan logic:
 * - Nebula:
 *   - If system name starts with a known nebula sector prefix => nebula.
 *   - Else find nearest "large" nebula reference star; if within 150 ly => nebula.
 *   - If rule asks for "all" and not found, also check nearest planetary nebula; if within 100 ly => nebula.
 * - Guardian:
 *   - If within a configured radius of certain guardian nebula reference coordinates => guardian system.
 *
 * Data is loaded from JSON resources so we don't have to embed thousands of coordinates in Java source.
 */
public final class NebulaGuardianClassifier {

    private static final Gson GSON = new Gson();

    // Resources generated directly from BioScan's data files.
    private static final String RES_SECTORS = "/exobiology/nebula_sectors.json";
    private static final String RES_LARGE = "/exobiology/nebula_large.json";
    private static final String RES_PLANETARY = "/exobiology/nebula_planetary.json";
    private static final String RES_GUARDIAN = "/exobiology/guardian_nebulae.json";

    private static volatile List<String> NEBULA_SECTORS;
    private static volatile Map<String, double[]> LARGE_NEBULA_COORDS;      // name -> [x,y,z]
    private static volatile Map<String, double[]> PLANETARY_NEBULA_COORDS;  // name -> [x,y,z]
    private static volatile Map<String, GuardianZone> GUARDIAN_ZONES;       // name -> zone

    private NebulaGuardianClassifier() {
        // utility
    }

    public static boolean isGuardianSystem(double[] starPos) {
        if (starPos == null || starPos.length < 3) {
            return false;
        }

        ensureLoaded();

        for (GuardianZone z : GUARDIAN_ZONES.values()) {
            double d = distanceLy(starPos[0], starPos[1], starPos[2], z.x, z.y, z.z);
            if (d < z.maxDistanceLy) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param checkType BioScan supports "large" and "all". (Overlay rules currently only use "all".)
     * @return a non-empty identifier if the system is considered "in a nebula", else "".
     *         The returned value is intended to be stored in BodyAttributes.nebula and can be used for debugging.
     */
    public static String determineNebulaTag(String systemName, double[] starPos, String checkType) {
        if (checkType == null) {
            checkType = "large";
        }
        String ct = checkType.trim().toLowerCase(Locale.ROOT);

        if (systemName == null || systemName.isEmpty()) {
            return "";
        }

        ensureLoaded();

        // 1) Sector-prefix match (fast path)
        for (String sector : NEBULA_SECTORS) {
            if (systemName.startsWith(sector)) {
                // BioScan treats this as "nebula = true"; return something useful.
                return sector;
            }
        }

        // 2) Distance-to-nearest reference nebula
        if (starPos == null || starPos.length < 3) {
            return "";
        }

        Nearest large = nearest(starPos, LARGE_NEBULA_COORDS);
        if (large != null && large.distanceLy < 150.0) {
            return large.name;
        }

        boolean all = "all".equals(ct);
        if (all) {
            Nearest pn = nearest(starPos, PLANETARY_NEBULA_COORDS);
            if (pn != null && pn.distanceLy < 100.0) {
                return pn.name;
            }
        }

        return "";
    }

    // ---------------------------------------------------------------------

    private static Nearest nearest(double[] starPos, Map<String, double[]> coords) {
        if (coords == null || coords.isEmpty()) {
            return null;
        }

        String bestName = null;
        double bestDistSq = Double.POSITIVE_INFINITY;

        double sx = starPos[0];
        double sy = starPos[1];
        double sz = starPos[2];

        for (Map.Entry<String, double[]> e : coords.entrySet()) {
            double[] c = e.getValue();
            if (c == null || c.length < 3) {
                continue;
            }

            double dx = sx - c[0];
            double dy = sy - c[1];
            double dz = sz - c[2];
            double dsq = dx * dx + dy * dy + dz * dz;

            if (dsq < bestDistSq) {
                bestDistSq = dsq;
                bestName = e.getKey();
            }
        }

        if (bestName == null) {
            return null;
        }

        double bestDist = Math.sqrt(bestDistSq);
        return new Nearest(bestName, bestDist);
    }

    private static double distanceLy(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void ensureLoaded() {
        if (NEBULA_SECTORS != null && LARGE_NEBULA_COORDS != null && PLANETARY_NEBULA_COORDS != null && GUARDIAN_ZONES != null) {
            return;
        }
        synchronized (NebulaGuardianClassifier.class) {
            if (NEBULA_SECTORS == null) {
                Type t = new TypeToken<List<String>>() {}.getType();
                NEBULA_SECTORS = loadJson(RES_SECTORS, t, Collections.emptyList());
            }
            if (LARGE_NEBULA_COORDS == null) {
                Type t = new TypeToken<Map<String, double[]>>() {}.getType();
                LARGE_NEBULA_COORDS = loadJson(RES_LARGE, t, Collections.emptyMap());
            }
            if (PLANETARY_NEBULA_COORDS == null) {
                Type t = new TypeToken<Map<String, double[]>>() {}.getType();
                PLANETARY_NEBULA_COORDS = loadJson(RES_PLANETARY, t, Collections.emptyMap());
            }
            if (GUARDIAN_ZONES == null) {
                Type t = new TypeToken<Map<String, GuardianZone>>() {}.getType();
                GUARDIAN_ZONES = loadJson(RES_GUARDIAN, t, Collections.emptyMap());
            }
        }
    }

    private static <T> T loadJson(String resourcePath, Type type, T fallback) {
        try (InputStream in = NebulaGuardianClassifier.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return fallback;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return GSON.fromJson(br, type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return fallback;
        }
    }

    private static final class Nearest {
        final String name;
        final double distanceLy;

        Nearest(String name, double distanceLy) {
            this.name = name;
            this.distanceLy = distanceLy;
        }
    }

    private static final class GuardianZone {
        double maxDistanceLy;
        double x;
        double y;
        double z;
    }
}
