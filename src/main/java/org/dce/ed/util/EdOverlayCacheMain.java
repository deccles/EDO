package org.dce.ed.util;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Tiny cache inspector for .edOverlaySystems.json
 *
 * Usage:
 *   java -cp .;gson-2.10.1.jar EdOverlayCacheMain <cacheJson> <systemNameOrAddress> <bodySpec>
 *
 * Examples:
 *   java -cp .;gson-2.10.1.jar EdOverlayCacheMain .edOverlaySystems.json "Sifi DM-A c28-7" "1"
 *   java -cp .;gson-2.10.1.jar EdOverlayCacheMain .edOverlaySystems.json "2005447971826" "1"
 *   java -cp .;gson-2.10.1.jar EdOverlayCacheMain .edOverlaySystems.json "S171 9" "A 2"
 *
 * bodySpec can be:
 *   - full body name ("Sifi DM-A c28-7 1")
 *   - suffix ("1", "A 2", "B 3")
 *   - bodyId ("8")
 */
public class EdOverlayCacheMain {

    // ---- Cache POJOs matching .edOverlaySystems.json ----

    public static class CacheSystem {
        String systemName;
        long systemAddress;
        double[] starPos;
        List<CacheBody> bodies;
    }

    public static class CacheBody {
        String name;
        int bodyId;

        Double distanceLs;
        Boolean landable;

        String atmoOrType;

        Integer parentStarBodyId; // may be null
        String parentStar;        // may be null

        String starType;          // set for stars when known
        Double surfaceTempK;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println("  java -cp .;gson.jar EdOverlayCacheMain <cacheJson> <systemNameOrAddress> <bodySpec>");
            return;
        }

        Path cachePath = Path.of(args[0]);
        String systemArg = args[1];
        String bodySpec = args[2];

        List<CacheSystem> systems = loadCache(cachePath);

        CacheSystem sys = findSystem(systems, systemArg);
        if (sys == null) {
            System.out.println("System not found: " + systemArg);
            printClosestSystems(systems, systemArg);
            return;
        }

        if (sys.bodies == null || sys.bodies.isEmpty()) {
            System.out.println("System has no bodies in cache: " + sys.systemName + " (" + sys.systemAddress + ")");
            return;
        }

        CacheBody body = findBody(sys, bodySpec);
        if (body == null) {
            System.out.println("Body not found in system.");
            System.out.println("  system: " + sys.systemName + " (" + sys.systemAddress + ")");
            System.out.println("  bodySpec: " + bodySpec);
            System.out.println();
            printBodySuggestions(sys, bodySpec);
            return;
        }

        printSystemHeader(sys);
        printStars(sys);

        System.out.println();
        System.out.println("Body matched:");
        System.out.println("  name:             " + body.name);
        System.out.println("  bodyId:           " + body.bodyId);
        System.out.println("  distanceLs:       " + body.distanceLs);
        System.out.println("  landable:         " + body.landable);
        System.out.println("  atmoOrType:       " + body.atmoOrType);
        System.out.println("  parentStar:       " + body.parentStar);
        System.out.println("  parentStarBodyId: " + body.parentStarBodyId);
        System.out.println("  starType(on body):" + body.starType);

        StarResolution res = resolveHostStar(sys, body);

        System.out.println();
        System.out.println("Resolved host star:");
        System.out.println("  method:     " + res.method);
        System.out.println("  starName:   " + (res.star != null ? res.star.name : null));
        System.out.println("  starBodyId: " + (res.star != null ? res.star.bodyId : null));
        System.out.println("  starType:   " + res.starType);

        // Extra: if parentStarBodyId points to a missing body, call it out explicitly.
        if (body.parentStarBodyId != null) {
            CacheBody parent = findBodyById(sys, body.parentStarBodyId);
            if (parent == null) {
                System.out.println();
                System.out.println("NOTE: parentStarBodyId=" + body.parentStarBodyId + " does not exist in this cached system.");
                System.out.println("      This is a cache completeness problem (star body missing), not a resolver problem.");
            }
        }
    }

    // ---- Resolution: mimic what your prediction pipeline wants ----

    private static class StarResolution {
        String method;
        CacheBody star;
        String starType;
    }

    private static StarResolution resolveHostStar(CacheSystem sys, CacheBody body) {
        StarResolution r = new StarResolution();

        // 1) If the body itself is a star (starType present), it's the host
        if (body.starType != null && !body.starType.isBlank()) {
            r.method = "body-is-star";
            r.star = body;
            r.starType = body.starType;
            return r;
        }

        // 2) If parentStarBodyId points to a star body, use that
        if (body.parentStarBodyId != null) {
            CacheBody star = findBodyById(sys, body.parentStarBodyId);
            if (star != null && star.starType != null && !star.starType.isBlank()) {
                r.method = "parentStarBodyId";
                r.star = star;
                r.starType = star.starType;
                return r;
            }
            // If it exists but no starType, still report what we found
            if (star != null) {
                r.method = "parentStarBodyId-but-missing-starType";
                r.star = star;
                r.starType = star.starType;
                return r;
            }
        }

        // 3) Fallback: primary by exact system name
        CacheBody bySystemName = findBodyByExactName(sys, sys.systemName);
        if (bySystemName != null && bySystemName.starType != null && !bySystemName.starType.isBlank()) {
            r.method = "fallback-systemName";
            r.star = bySystemName;
            r.starType = bySystemName.starType;
            return r;
        }

        // 4) Fallback: first body with starType set
        CacheBody anyStar = sys.bodies.stream()
                .filter(Objects::nonNull)
                .filter(b -> b.starType != null && !b.starType.isBlank())
                .findFirst()
                .orElse(null);

        r.method = "fallback-first-star";
        r.star = anyStar;
        r.starType = anyStar != null ? anyStar.starType : null;
        return r;
    }

    // ---- Find system/body helpers ----

    private static CacheSystem findSystem(List<CacheSystem> systems, String systemArg) {
        if (systems == null || systemArg == null) {
            return null;
        }

        // If numeric, treat as systemAddress
        Long addr = tryParseLong(systemArg.trim());
        if (addr != null) {
            for (CacheSystem s : systems) {
                if (s != null && s.systemAddress == addr.longValue()) {
                    return s;
                }
            }
        }

        // Else, treat as systemName
        for (CacheSystem s : systems) {
            if (s != null && s.systemName != null && s.systemName.equals(systemArg)) {
                return s;
            }
        }

        return null;
    }

    private static CacheBody findBody(CacheSystem sys, String bodySpec) {
        if (sys == null || sys.bodies == null || bodySpec == null) {
            return null;
        }

        String spec = bodySpec.trim();
        if (spec.isEmpty()) {
            return null;
        }

        // 1) If numeric, try bodyId match
        Integer bodyId = tryParseInt(spec);
        if (bodyId != null) {
            CacheBody byId = findBodyById(sys, bodyId);
            if (byId != null) {
                return byId;
            }
        }

        // 2) Exact full-name match
        CacheBody exact = findBodyByExactName(sys, spec);
        if (exact != null) {
            return exact;
        }

        // 3) If it's a suffix like "A 2" or "1", try systemName + " " + suffix
        String full = sys.systemName + " " + spec;
        CacheBody byFull = findBodyByExactName(sys, full);
        if (byFull != null) {
            return byFull;
        }

        // 4) Suffix match
        String suffix = " " + spec;
        for (CacheBody b : sys.bodies) {
            if (b == null || b.name == null) {
                continue;
            }
            if (b.name.endsWith(suffix)) {
                return b;
            }
        }

        // 5) Contains match (last resort)
        String needle = spec.toLowerCase(Locale.ROOT);
        for (CacheBody b : sys.bodies) {
            if (b == null || b.name == null) {
                continue;
            }
            if (b.name.toLowerCase(Locale.ROOT).contains(needle)) {
                return b;
            }
        }

        return null;
    }

    private static CacheBody findBodyById(CacheSystem sys, int bodyId) {
        for (CacheBody b : sys.bodies) {
            if (b != null && b.bodyId == bodyId) {
                return b;
            }
        }
        return null;
    }

    private static CacheBody findBodyByExactName(CacheSystem sys, String name) {
        for (CacheBody b : sys.bodies) {
            if (b != null && b.name != null && b.name.equals(name)) {
                return b;
            }
        }
        return null;
    }

    // ---- Pretty printing ----

    private static void printSystemHeader(CacheSystem sys) {
        System.out.println("System: " + sys.systemName + " (address=" + sys.systemAddress + ")");
        System.out.println("Bodies: " + sys.bodies.size());
    }

    private static void printStars(CacheSystem sys) {
        List<CacheBody> stars = new ArrayList<>();
        for (CacheBody b : sys.bodies) {
            if (b == null) {
                continue;
            }
            if (b.starType != null && !b.starType.isBlank()) {
                stars.add(b);
            }
        }

        stars.sort(Comparator.comparingInt(a -> a.bodyId));

        System.out.println();
        System.out.println("Stars in cache (starType != null):");
        if (stars.isEmpty()) {
            System.out.println("  (none)");
            return;
        }

        for (CacheBody s : stars) {
            System.out.println("  bodyId=" + s.bodyId
                    + "  name=" + s.name
                    + "  starType=" + s.starType
                    + "  distanceLs=" + s.distanceLs);
        }
    }

    private static void printBodySuggestions(CacheSystem sys, String bodySpec) {
        String needle = bodySpec.toLowerCase(Locale.ROOT);
        int count = 0;

        System.out.println("Closest body name matches:");
        for (CacheBody b : sys.bodies) {
            if (b == null || b.name == null) {
                continue;
            }
            String n = b.name.toLowerCase(Locale.ROOT);
            if (n.contains(needle) || n.endsWith(" " + needle)) {
                System.out.println("  bodyId=" + b.bodyId + "  " + b.name
                        + (b.starType != null ? ("  (starType=" + b.starType + ")") : ""));
                count++;
                if (count >= 30) {
                    break;
                }
            }
        }

        if (count == 0) {
            System.out.println("  (none)");
        }
    }

    private static void printClosestSystems(List<CacheSystem> systems, String systemArg) {
        if (systems == null || systemArg == null) {
            return;
        }

        String needle = systemArg.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();

        for (CacheSystem s : systems) {
            if (s == null || s.systemName == null) {
                continue;
            }
            String n = s.systemName.toLowerCase(Locale.ROOT);
            if (n.contains(needle)) {
                names.add(s.systemName + " (" + s.systemAddress + ")");
            }
        }

        names.stream().sorted().limit(20).forEach(x -> System.out.println("  " + x));
    }

    // ---- Load ----

    private static List<CacheSystem> loadCache(Path path) throws IOException {
        Gson gson = new GsonBuilder().create();
        try (Reader r = Files.newBufferedReader(path)) {
            CacheSystem[] arr = gson.fromJson(r, CacheSystem[].class);
            if (arr == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(arr);
        }
    }

    // ---- Parse helpers ----

    private static Integer tryParseInt(String s) {
        try {
            return Integer.valueOf(Integer.parseInt(s));
        } catch (Exception ex) {
            return null;
        }
    }

    private static Long tryParseLong(String s) {
        try {
            return Long.valueOf(Long.parseLong(s));
        } catch (Exception ex) {
            return null;
        }
    }
}
