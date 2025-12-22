package org.dce.ed.exobiology;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.dce.ed.cache.CachedBody;
import org.dce.ed.cache.CachedSystem;
import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.state.BodyInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public final class BioPredictFromCacheMain {

    private static final String CACHE_FILE_NAME = ".edOverlaySystems.json";

    public static void main(String[] args) throws Exception {

        String defaultBodyName = "Sifi WK-C c27-5 C 5";

        String bodyName = promptForBodyName(defaultBodyName);
        if (bodyName == null || bodyName.trim().isEmpty()) {
            System.out.println("No body name entered. Exiting.");
            return;
        }
        bodyName = bodyName.trim();

        Path cachePath = defaultCachePath();
        System.out.println("Using cache: " + cachePath.toAbsolutePath());

        if (!Files.isRegularFile(cachePath)) {
            System.out.println("Cache file not found: " + cachePath.toAbsolutePath());
            return;
        }

        CachedBodyMatch match = findBodyInCache(cachePath, bodyName);
        if (match == null) {
            System.out.println("Body not found in cache: '" + bodyName + "'");
            return;
        }

        BodyInfo info = toBodyInfo(match.system, match.body);

        System.out.println();
        System.out.println("Found body:");
        System.out.println("  System: " + info.getStarSystem());
        System.out.println("  Body:   " + info.getBodyName());
        System.out.println("  BodyId: " + info.getBodyId());
        System.out.println("  HasBio: " + info.hasBio());
        System.out.println("  Planet: " + nullToEmpty(info.getPlanetClass()));
        System.out.println("  Atmo:   " + nullToEmpty(info.getAtmosphere()));
        System.out.println("  Atmo2:  " + nullToEmpty(info.getAtmoOrType()));
        System.out.println("  TempK:  " + (info.getSurfaceTempK() == null ? "" : String.format(Locale.US, "%.3f", info.getSurfaceTempK())));
        System.out.println("  Press:  " + (info.getSurfacePressure() == null ? "" : String.format(Locale.US, "%.6f", info.getSurfacePressure())));
        System.out.println("  GravMS: " + (info.getGravityMS() == null ? "" : String.format(Locale.US, "%.6f", info.getGravityMS())));
        System.out.println("  StarPos:" + (info.getStarPos() == null ? " <null>" :
                String.format(Locale.US, " [%.3f, %.3f, %.3f]", info.getStarPos()[0], info.getStarPos()[1], info.getStarPos()[2])));
        System.out.println();

        BodyAttributes attrs = info.buildBodyAttributes();
        if (attrs == null) {
            System.out.println("BodyInfo.buildBodyAttributes() returned null (insufficient data).");
            return;
        }

        List<BioCandidate> candidates = ExobiologyData.predict(attrs);

        System.out.println("Predictions: " + (candidates == null ? 0 : candidates.size()));
        if (candidates == null || candidates.isEmpty()) {
            System.out.println("  <none>");
            return;
        }

        for (int i = 0; i < candidates.size(); i++) {
            BioCandidate c = candidates.get(i);
            System.out.println(String.format(Locale.US,
                    "  %2d) %-12s %-16s  score=%.6f  baseValue=%d",
                    i + 1,
                    safe(c.genus),
                    safe(c.species),
                    c.getScore(),
                    c.baseValue));
        }
    }

    private static String promptForBodyName(String defaultValue) throws Exception {
        final String[] result = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            result[0] = (String) JOptionPane.showInputDialog(
                    null,
                    "Enter body name (exact match from cache):",
                    "Predict Exobiology From Cache",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    defaultValue
            );
        });

        return result[0];
    }

    private static Path defaultCachePath() {
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            home = ".";
        }
        return Paths.get(home, CACHE_FILE_NAME);
    }

    private static CachedBodyMatch findBodyInCache(Path cachePath, String bodyName) throws IOException {

        Gson gson = new GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .create();

        Type type = new TypeToken<List<CachedSystem>>() {}.getType();

        try (BufferedReader reader = Files.newBufferedReader(cachePath, StandardCharsets.UTF_8)) {

            List<CachedSystem> systems = gson.fromJson(reader, type);
            if (systems == null || systems.isEmpty()) {
                return null;
            }

            for (CachedSystem cs : systems) {
                if (cs == null || cs.bodies == null) {
                    continue;
                }

                for (CachedBody cb : cs.bodies) {
                    if (cb == null || cb.name == null) {
                        continue;
                    }

                    if (cb.name.equalsIgnoreCase(bodyName)) {
                        return new CachedBodyMatch(cs, cb);
                    }
                }
            }
        }

        return null;
    }

    private static BodyInfo toBodyInfo(CachedSystem cs, CachedBody cb) {
        BodyInfo info = new BodyInfo();

        info.setBodyName(cb.name);
        info.setStarSystem(cb.starSystem);

        // StarPos is stored at system level in the cache; BodyInfo.buildBodyAttributes() uses it.
        info.setStarPos(cs.starPos);

        info.setBodyId(cb.bodyId);
        info.setDistanceLs(cb.distanceLs);
        info.setGravityMS(cb.gravityMS);
        info.setSurfacePressure(cb.surfacePressure);

        info.setLandable(cb.landable);
        info.setHasBio(cb.hasBio);
        info.setHasGeo(cb.hasGeo);
        info.setHighValue(cb.highValue);

        info.setAtmoOrType(cb.atmoOrType);
        info.setPlanetClass(cb.planetClass);
        info.setAtmosphere(cb.atmosphere);

        info.setSurfaceTempK(cb.surfaceTempK);
        info.setVolcanism(cb.volcanism);

        info.setNumberOfBioSignals(cb.getNumberOfBioSignals());
        info.setDiscoveryCommander(cb.discoveryCommander);

        info.setNebula(cb.nebula);
        info.setParentStar(cb.parentStar);

        if (cb.observedGenusPrefixes != null && !cb.observedGenusPrefixes.isEmpty()) {
            info.setObservedGenusPrefixes(new java.util.HashSet<>(cb.observedGenusPrefixes));
        }
        if (cb.observedBioDisplayNames != null && !cb.observedBioDisplayNames.isEmpty()) {
            info.setObservedBioDisplayNames(new java.util.HashSet<>(cb.observedBioDisplayNames));
        }

        if (cb.predictions != null && !cb.predictions.isEmpty()) {
            info.setPredictions(new java.util.ArrayList<>(cb.predictions));
        }

        return info;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static final class CachedBodyMatch {
        private final CachedSystem system;
        private final CachedBody body;

        private CachedBodyMatch(CachedSystem system, CachedBody body) {
            this.system = system;
            this.body = body;
        }
    }

    private BioPredictFromCacheMain() {
    }
}
