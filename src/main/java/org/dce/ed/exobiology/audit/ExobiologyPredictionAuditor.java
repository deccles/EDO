package org.dce.ed.exobiology.audit;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.exobiology.ExobiologyData.BodyAttributes;
import org.dce.ed.state.BodyInfo;

/**
 * Utility to audit exobiology predictions vs. observed species.
 *
 * Usage (pseudo code):
 *
 *   // You already have this from your existing pipeline:
 *   String systemName = "Ploea Eurl AJ-L c23-1";
 *   List<BodyInfo> bodies = systemState.getBodiesForSystem(systemName);
 *
 *   ExobiologyPredictionAuditor auditor = new ExobiologyPredictionAuditor();
 *   List<ExobiologyPredictionAuditor.ExobiologyAuditCase> cases =
 *           auditor.auditSystemBodies(systemName, bodies);
 *
 *   // Only fail cases are returned. You can then write them as JSON:
 *   auditor.writeCasesAsJson(cases, Path.of("exobio_audit_cases.json"));
 */
public class ExobiologyPredictionAuditor {

    private final Gson gson;

    public ExobiologyPredictionAuditor() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Audit all bodies in a system and return only the "fail" cases:
     * at least one observed species was not predicted.
     *
     * @param systemName Human-readable system name
     * @param bodies     BodyInfo list for that system
     * @return list of audit cases (only failing bodies)
     */
    public List<ExobiologyAuditCase> auditSystemBodies(String systemName,
                                                       List<BodyInfo> bodies) {
        List<ExobiologyAuditCase> result = new ArrayList<>();
        if (bodies == null) {
            return result;
        }

        for (BodyInfo body : bodies) {
            ExobiologyAuditCase c = auditSingleBody(systemName, body);
            if (c != null && c.isFail()) {
                result.add(c);
            }
        }

        return result;
    }

    /**
     * Audit a single body.
     *
     * Returns null if:
     *   - the body has no observed biology, or
     *   - no BodyAttributes can be built (insufficient physical data), or
     *   - prediction list comes back empty.
     */
    public ExobiologyAuditCase auditSingleBody(String systemName,
                                               BodyInfo body) {
        if (body == null) {
            return null;
        }

        // Observed species names, as recorded from DSS / organic scans
        Set<String> observedDisplayNames = body.getObservedBioDisplayNames();
        if (observedDisplayNames == null || observedDisplayNames.isEmpty()) {
            // No biology recorded; nothing to audit
            return null;
        }

        BodyAttributes attrs = body.buildBodyAttributes();
        if (attrs == null) {
            // Not enough physical info (planetClass/atmo/gravity/temp/volc) to run prediction
            return null;
        }

        List<BioCandidate> predicted = ExobiologyData.predict(attrs);
        if (predicted == null || predicted.isEmpty()) {
            // Prediction could not suggest anything; you *could* consider this a hard fail,
            // but for now we skip these bodies so we focus on "rule misses"
            return null;
        }

        // Build normalized sets
        Set<String> normalizedObserved = new LinkedHashSet<>();
        for (String raw : observedDisplayNames) {
            String norm = normalizeSpeciesName(raw);
            if (!norm.isEmpty()) {
                normalizedObserved.add(norm);
            }
        }

        Set<String> normalizedPredicted = new LinkedHashSet<>();
        List<PredictedSpecies> predictedDetails = new ArrayList<>();

        for (BioCandidate bc : predicted) {
            String display = bc.getDisplayName(); // e.g. "Stratum Paleas"
            String norm = normalizeSpeciesName(display);
            if (!norm.isEmpty()) {
                normalizedPredicted.add(norm);
            }

            predictedDetails.add(new PredictedSpecies(
                    bc.getGenus(),
                    bc.getSpecies(),
                    display,
                    bc.getScore(),
                    bc.getBaseValue(),
                    bc.getReason()
            ));
        }

        if (normalizedObserved.isEmpty()) {
            // Nothing usable in the observed list after normalization
            return null;
        }

        // Compute missing species: observed - predicted
        List<String> missingNormalized = new ArrayList<>();
        List<String> missingOriginal = new ArrayList<>();

        for (String obsNorm : normalizedObserved) {
            if (!normalizedPredicted.contains(obsNorm)) {
                missingNormalized.add(obsNorm);

                // Try to keep a nicer version (original spelling) if available
                String pretty = findOriginal(observedDisplayNames, obsNorm);
                if (pretty == null) {
                    pretty = obsNorm;
                }
                missingOriginal.add(pretty);
            }
        }

        // Build a snapshot of planet attributes for debugging/refinement
        PlanetSnapshot planet = new PlanetSnapshot(
                body.getPlanetClass(),
                body.getAtmosphere(),
                body.getSurfaceTempK(),
                body.getGravityMS(),
                body.getVolcanism()
        );

        return new ExobiologyAuditCase(
                systemName,
                body.getBodyName(),
                body.getBodyId(),
                planet,
                new ArrayList<>(observedDisplayNames),
                new ArrayList<>(normalizedObserved),
                predictedDetails,
                missingOriginal,
                missingNormalized
        );
    }

    /**
     * Write a list of audit cases as pretty-printed JSON.
     * You can send me the resulting file (or a subset) for rule refinement.
     */
    public void writeCasesAsJson(List<ExobiologyAuditCase> cases,
                                 Path outputFile) throws IOException {
        try (Writer out = Files.newBufferedWriter(outputFile)) {
            gson.toJson(cases, out);
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static String normalizeSpeciesName(String s) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        // Collapse multiple spaces, lowercase for robust matching
        String collapsed = trimmed.replaceAll("\\s+", " ");
        return collapsed.toLowerCase(Locale.ROOT);
    }

    private static String findOriginal(Set<String> originals, String normalized) {
        if (originals == null || originals.isEmpty()) {
            return null;
        }
        for (String s : originals) {
            if (normalizeSpeciesName(s).equals(normalized)) {
                return s;
            }
        }
        return null;
    }

    // ------------------------------------------------------------
    // DTOs for JSON output
    // ------------------------------------------------------------

    /**
     * One failing body (at least one observed species was not predicted).
     */
    public static final class ExobiologyAuditCase {

        private final String systemName;
        private final String bodyName;
        private final int bodyId;

        private final PlanetSnapshot planet;

        // Truth data
        private final List<String> observedDisplayNames;      // original strings
        private final List<String> observedNormalizedNames;   // normalized "genus species"

        // Predictions
        private final List<PredictedSpecies> predictedSpecies;

        // Observed-but-not-predicted (fail details)
        private final List<String> missingObservedDisplayNames;
        private final List<String> missingObservedNormalizedNames;

        public ExobiologyAuditCase(String systemName,
                                   String bodyName,
                                   int bodyId,
                                   PlanetSnapshot planet,
                                   List<String> observedDisplayNames,
                                   List<String> observedNormalizedNames,
                                   List<PredictedSpecies> predictedSpecies,
                                   List<String> missingObservedDisplayNames,
                                   List<String> missingObservedNormalizedNames) {
            this.systemName = systemName;
            this.bodyName = bodyName;
            this.bodyId = bodyId;
            this.planet = planet;
            this.observedDisplayNames = observedDisplayNames;
            this.observedNormalizedNames = observedNormalizedNames;
            this.predictedSpecies = predictedSpecies;
            this.missingObservedDisplayNames = missingObservedDisplayNames;
            this.missingObservedNormalizedNames = missingObservedNormalizedNames;
        }

        public String getSystemName() {
            return systemName;
        }

        public String getBodyName() {
            return bodyName;
        }

        public int getBodyId() {
            return bodyId;
        }

        public PlanetSnapshot getPlanet() {
            return planet;
        }

        public List<String> getObservedDisplayNames() {
            return observedDisplayNames;
        }

        public List<String> getObservedNormalizedNames() {
            return observedNormalizedNames;
        }

        public List<PredictedSpecies> getPredictedSpecies() {
            return predictedSpecies;
        }

        public List<String> getMissingObservedDisplayNames() {
            return missingObservedDisplayNames;
        }

        public List<String> getMissingObservedNormalizedNames() {
            return missingObservedNormalizedNames;
        }

        /**
         * A case is considered a fail if there is at least one observed
         * species that was not in the predicted set.
         */
        public boolean isFail() {
            return missingObservedNormalizedNames != null
                    && !missingObservedNormalizedNames.isEmpty();
        }
    }

    /**
     * Flattened snapshot of planetary attributes at the time of audit.
     * (These are mostly for debugging constraint rules.)
     */
    public static final class PlanetSnapshot {

        private final String planetClass;
        private final String atmosphere;
        private final Double surfaceTempK;
        private final Double gravityMS;
        private final String volcanism;

        public PlanetSnapshot(String planetClass,
                              String atmosphere,
                              Double surfaceTempK,
                              Double gravityMS,
                              String volcanism) {
            this.planetClass = planetClass;
            this.atmosphere = atmosphere;
            this.surfaceTempK = surfaceTempK;
            this.gravityMS = gravityMS;
            this.volcanism = volcanism;
        }

        public String getPlanetClass() {
            return planetClass;
        }

        public String getAtmosphere() {
            return atmosphere;
        }

        public Double getSurfaceTempK() {
            return surfaceTempK;
        }

        public Double getGravityMS() {
            return gravityMS;
        }

        public String getVolcanism() {
            return volcanism;
        }
    }

    /**
     * Flattened prediction info for JSON output.
     */
    public static final class PredictedSpecies {

        private final String genus;
        private final String species;
        private final String displayName;
        private final double score;
        private final long baseValue;
        private final String reason;

        public PredictedSpecies(String genus,
                                String species,
                                String displayName,
                                double score,
                                long baseValue,
                                String reason) {
            this.genus = genus;
            this.species = species;
            this.displayName = displayName;
            this.score = score;
            this.baseValue = baseValue;
            this.reason = reason;
        }

        public String getGenus() {
            return genus;
        }

        public String getSpecies() {
            return species;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getScore() {
            return score;
        }

        public long getBaseValue() {
            return baseValue;
        }

        public String getReason() {
            return reason;
        }
    }
}
