package org.dce.ed.util;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ExoStarTypeInspector {

    // ---- Cache POJOs that match .edOverlaySystems.json ----

    public static class CacheSystem {
        String systemName;
        long systemAddress;
        double[] starPos;
        List<CacheBody> bodies;
    }

    public static class CacheBody {
        String name;
        int bodyId;
        String starSystem;
        double[] starPos;

        Double distanceLs;
        Boolean landable;
        Boolean hasBio;
        Boolean hasGeo;
        Boolean highValue;

        String atmoOrType;
        Double surfaceTempK;

        Integer parentStarBodyId; // can be null
        String starType;          // only set for stars (usually)
        Integer numberOfBioSignals;
    }

    // ---- Main ----

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println("  java ExoStarTypeInspector <pathToCacheJson> <systemName> <bodySpec>");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("  java ExoStarTypeInspector .edOverlaySystems.json \"S171 9\" \"A 2\"");
            System.out.println("  java ExoStarTypeInspector .edOverlaySystems.json \"S171 9\" \"S171 9 A 2\"");
            System.out.println("  java ExoStarTypeInspector .edOverlaySystems.json \"PARNUT\" \"2\"");
            return;
        }

        Path cachePath = Path.of(args[0]);
        String systemName = args[1];
        String bodySpec = args[2];

        List<CacheSystem> systems = loadCache(cachePath);

        CacheSystem system = systems.stream()
                .filter(s -> s != null && s.systemName != null && s.systemName.equals(systemName))
                .findFirst()
                .orElse(null);

        if (system == null) {
            System.out.println("System not found: " + systemName);
            printClosestSystemNames(systems, systemName);
            return;
        }

        if (system.bodies == null || system.bodies.isEmpty()) {
            System.out.println("System has no bodies in cache: " + systemName);
            return;
        }

        String fullBodyName = normalizeBodyName(systemName, bodySpec);

        CacheBody body = findBody(system, fullBodyName, bodySpec);

        if (body == null) {
            System.out.println("Body not found.");
            System.out.println("  systemName: " + systemName);
            System.out.println("  bodySpec:   " + bodySpec);
            System.out.println("  tried full: " + fullBodyName);
            System.out.println();
            printBodyNameSuggestions(system, bodySpec);
            return;
        }

        // Resolve star type using the same concept your prediction code should use:
        // 1) If body is a star and has starType, that's the answer.
        // 2) Else use parentStarBodyId to locate the star body and take its starType.
        // 3) Else fallback by name conventions.
        StarResolution r = resolveHostStar(system, body);

        printReport(system, body, r);
    }

    // ---- Report printing ----

    private static void printReport(CacheSystem system, CacheBody body, StarResolution r) {
        System.out.println("System: " + system.systemName + " (address=" + system.systemAddress + ")");
        System.out.println("Body matched:");
        System.out.println("  name:              " + body.name);
        System.out.println("  bodyId:            " + body.bodyId);
        System.out.println("  landable:          " + body.landable);
        System.out.println("  distanceLs:        " + body.distanceLs);
        System.out.println("  atmoOrType:        " + body.atmoOrType);
        System.out.println("  parentStarBodyId:  " + body.parentStarBodyId);
        System.out.println("  starType(on body): " + body.starType);
        System.out.println();

        System.out.println("Resolved host star:");
        System.out.println("  method:            " + r.method);
        System.out.println("  starName:          " + (r.star != null ? r.star.name : null));
        System.out.println("  starBodyId:        " + (r.star != null ? r.star.bodyId : null));
        System.out.println("  starType:          " + r.starType);
    }

    // ---- Resolution logic ----

    private static class StarResolution {
        String method;
        CacheBody star;
        String starType;
    }

    private static StarResolution resolveHostStar(CacheSystem system, CacheBody body) {
        StarResolution r = new StarResolution();

        // Case 1: body is a star
        if (body.starType != null && !body.starType.isBlank()) {
            r.method = "body-is-star";
            r.star = body;
            r.starType = body.starType;
            return r;
        }

        // Case 2: parentStarBodyId points to a star body
        Integer psid = body.parentStarBodyId;
        if (psid != null) {
            CacheBody star = findBodyById(system, psid);
            if (star != null && star.starType != null && !star.starType.isBlank()) {
                r.method = "parentStarBodyId";
                r.star = star;
                r.starType = star.starType;
                return r;
            }
        }

        // Case 3: fallback by naming convention
        // Prefer:
        //  - exact system name star
        //  - else systemName + " A"
        //  - else any star with distanceLs==0
        CacheBody bySystemName = findBodyByExactName(system, system.systemName);
        if (bySystemName != null && bySystemName.starType != null && !bySystemName.starType.isBlank()) {
            r.method = "fallback-name-systemName";
            r.star = bySystemName;
            r.starType = bySystemName.starType;
            return r;
        }

        CacheBody byA = findBodyByExactName(system, system.systemName + " A");
        if (byA != null && byA.starType != null && !byA.starType.isBlank()) {
            r.method = "fallback-name-systemName-A";
            r.star = byA;
            r.starType = byA.starType;
            return r;
        }

        CacheBody byDistance = system.bodies.stream()
                .filter(b -> b != null)
                .filter(b -> b.starType != null && !b.starType.isBlank())
                .filter(b -> b.distanceLs != null && b.distanceLs.doubleValue() == 0.0)
                .findFirst()
                .orElse(null);

        r.method = "fallback-first-star-distanceLs==0";
        r.star = byDistance;
        r.starType = byDistance != null ? byDistance.starType : null;
        return r;
    }

    // ---- Find helpers ----

    private static CacheBody findBodyById(CacheSystem system, int bodyId) {
        if (system.bodies == null) {
            return null;
        }
        for (CacheBody b : system.bodies) {
            if (b == null) {
                continue;
            }
            if (b.bodyId == bodyId) {
                return b;
            }
        }
        return null;
    }

    private static CacheBody findBodyByExactName(CacheSystem system, String name) {
        if (name == null || system.bodies == null) {
            return null;
        }
        for (CacheBody b : system.bodies) {
            if (b == null || b.name == null) {
                continue;
            }
            if (b.name.equals(name)) {
                return b;
            }
        }
        return null;
    }

    private static CacheBody findBody(CacheSystem system, String fullBodyName, String bodySpec) {
        // 1) exact by full name
        CacheBody exact = findBodyByExactName(system, fullBodyName);
        if (exact != null) {
            return exact;
        }

        // 2) exact by bodySpec as-is (if they provided full name already)
        CacheBody exactSpec = findBodyByExactName(system, bodySpec);
        if (exactSpec != null) {
            return exactSpec;
        }

        // 3) suffix match: ends with " " + bodySpec (so "S171 9 A 2" matches bodySpec "A 2")
        String suffix = " " + bodySpec;
        CacheBody suffixMatch = system.bodies.stream()
                .filter(b -> b != null && b.name != null)
                .filter(b -> b.name.endsWith(suffix))
                .findFirst()
                .orElse(null);

        if (suffixMatch != null) {
            return suffixMatch;
        }

        // 4) loose contains match
        String needle = bodySpec.toLowerCase(Locale.ROOT);
        return system.bodies.stream()
                .filter(b -> b != null && b.name != null)
                .filter(b -> b.name.toLowerCase(Locale.ROOT).contains(needle))
                .findFirst()
                .orElse(null);
    }

    private static void printBodyNameSuggestions(CacheSystem system, String bodySpec) {
        String needle = bodySpec.toLowerCase(Locale.ROOT);

        List<String> matches = new ArrayList<>();
        for (CacheBody b : system.bodies) {
            if (b == null || b.name == null) {
                continue;
            }
            String n = b.name.toLowerCase(Locale.ROOT);
            if (n.contains(needle) || n.endsWith(" " + needle)) {
                matches.add(b.name);
            }
        }

        matches.stream()
                .sorted()
                .limit(30)
                .forEach(n -> System.out.println("  " + n));

        if (matches.isEmpty()) {
            System.out.println("  (no close name matches found)");
        }
    }

    private static void printClosestSystemNames(List<CacheSystem> systems, String systemName) {
        String needle = systemName.toLowerCase(Locale.ROOT);

        List<String> candidates = systems.stream()
                .filter(Objects::nonNull)
                .map(s -> s.systemName)
                .filter(Objects::nonNull)
                .filter(n -> n.toLowerCase(Locale.ROOT).contains(needle) || needle.contains(n.toLowerCase(Locale.ROOT)))
                .distinct()
                .sorted()
                .limit(20)
                .toList();

        if (candidates.isEmpty()) {
            System.out.println("No similar system names found (simple contains match).");
            return;
        }

        System.out.println("Similar system names:");
        for (String n : candidates) {
            System.out.println("  " + n);
        }
    }

    private static String normalizeBodyName(String systemName, String bodySpec) {
        if (bodySpec == null) {
            return systemName;
        }
        String s = bodySpec.trim();
        if (s.isEmpty()) {
            return systemName;
        }
        if (s.startsWith(systemName)) {
            return s;
        }
        return systemName + " " + s;
    }

    // ---- Load ----

    private static List<CacheSystem> loadCache(Path path) throws IOException {
        Gson gson = new GsonBuilder().create();
        try (Reader r = Files.newBufferedReader(path)) {
            CacheSystem[] arr = gson.fromJson(r, CacheSystem[].class);
            List<CacheSystem> out = new ArrayList<>();
            if (arr != null) {
                for (CacheSystem s : arr) {
                    out.add(s);
                }
            }
            return out;
        }
    }
}
