package org.dce.ed.exobiology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Exobiology prediction helper using rulesets derived from the
 * open-source ruleset catalog.
 *
 * Species rules are stored as a set of SpeciesRule entries per species.
 * A species is considered valid for a body if ANY of its rules match.
 */
public final class ExobiologyData {

    private ExobiologyData() {
    }

    /* =====================================================================
     * Enums
     * ===================================================================== */

    public enum PlanetType {
        ROCKY,
        METAL_RICH,
        HIGH_METAL,
        ROCKY_ICE,
        ICY,
        OTHER,
        UNKNOWN
    }

    public enum AtmosphereType {
        NONE,
        CO2,
        METHANE,
        NITROGEN,
        OXYGEN,
        NEON,
        ARGON,
        WATER,
        SULPHUR_DIOXIDE,
        AMMONIA,
        HELIUM,
        OTHER,
        UNKNOWN
    }

    /**
     * Volcanism constraint used by a single rule.
     */
    public enum VolcanismRequirement {
        ANY,
        NO_VOLCANISM,
        VOLCANIC_ONLY
    }

    /* =====================================================================
     * BodyAttributes
     * ===================================================================== */

    /**
     * Attributes of a body required for prediction.
     * This should match what your overlay already builds.
     */
    public static final class BodyAttributes {

        public final PlanetType planetType;
        public final double gravity;
        public final AtmosphereType atmosphere;
        public final double tempKMin;
        public final double tempKMax;
        public final boolean hasVolcanism;
        public final String volcanismType;

        public BodyAttributes(PlanetType planetType,
                              double gravity,
                              AtmosphereType atmosphere,
                              double tempKMin,
                              double tempKMax,
                              boolean hasVolcanism,
                              String volcanismType) {
            this.planetType = planetType;
            this.gravity = gravity;
            this.atmosphere = atmosphere;
            this.tempKMin = tempKMin;
            this.tempKMax = tempKMax;
            this.hasVolcanism = hasVolcanism;
            this.volcanismType = volcanismType;
        }
    }

    /* =====================================================================
     * Rules and constraints
     * ===================================================================== */

    /**
     * One ruleset row for a given {genus, species}, mapped from the Python
     * rulesets entry.
     *
     * Missing dimensions in the source ruleset are represented as:
     *  - gravity:   [0, 100] (effectively "no constraint")
     *  - temp:      [0, 1_000_000]
     *  - atmospheres: empty set  => no restriction
     *  - bodyTypes:  empty set   => no restriction
     *  - volcanism:  ANY
     */
    /**
     * One ruleset row for a given {genus, species}, mapped from the Python
     * rulesets entry.
     *
     * Missing dimensions in the source ruleset are represented as:
     *  - gravity:   [0, 100] (effectively "no constraint")
     *  - temp:      [0, 1_000_000]
     *  - pressure:  [0, 1_000_000]
     *  - atmospheres: empty set  => no restriction
     *  - bodyTypes:  empty set   => no restriction
     *  - volcanism:  ANY
     *  - lists/maps: empty => no restriction (until you choose to use them)
     */
    public static final class SpeciesRule {

        public final double minGravity;
        public final double maxGravity;
        public final double minTempK;
        public final double maxTempK;

        public final double minPressure;
        public final double maxPressure;

        public final Set<AtmosphereType> atmospheres;
        public final Set<PlanetType> bodyTypes;

        /** Optional detailed atmosphere components (e.g. {"CO2": 0.8}). */
        public final Map<String, Double> atmosphereComponents;

        /** Optional host body types / names (for Brain Trees etc.). */
        public final List<String> bodies;

        /** Optional maximum orbital period constraint. */
        public final Double maxOrbitalPeriod;

        /** Optional distance constraint (meaning as defined in rulesets). */
        public final Double distance;

        /** Optional Guardian-space flag constraint. */
        public final Boolean guardian;

        /** Optional nebula name / flag. */
        public final String nebula;

        /** Optional parent star constraints. */
        public final List<String> parentStars;

        /** Optional galactic region constraints. */
        public final List<String> regions;

        /** Optional host star class constraints (e.g. "B IV"). */
        public final List<String> starClasses;

        /** Optional “tuber” anchor constraints. */
        public final List<String> tuberTargets;

        public final VolcanismRequirement volcanismRequirement;

        public SpeciesRule(double minGravity,
                           double maxGravity,
                           double minTempK,
                           double maxTempK,
                           double minPressure,
                           double maxPressure,
                           Set<AtmosphereType> atmospheres,
                           Set<PlanetType> bodyTypes,
                           Map<String, Double> atmosphereComponents,
                           List<String> bodies,
                           Double maxOrbitalPeriod,
                           Double distance,
                           Boolean guardian,
                           String nebula,
                           List<String> parentStars,
                           List<String> regions,
                           List<String> starClasses,
                           List<String> tuberTargets,
                           VolcanismRequirement volcanismRequirement) {

            this.minGravity = minGravity;
            this.maxGravity = maxGravity;
            this.minTempK = minTempK;
            this.maxTempK = maxTempK;

            this.minPressure = minPressure;
            this.maxPressure = maxPressure;

            this.atmospheres = atmospheres;
            this.bodyTypes = bodyTypes;

            this.atmosphereComponents = atmosphereComponents;
            this.bodies = bodies;
            this.maxOrbitalPeriod = maxOrbitalPeriod;
            this.distance = distance;
            this.guardian = guardian;
            this.nebula = nebula;
            this.parentStars = parentStars;
            this.regions = regions;
            this.starClasses = starClasses;
            this.tuberTargets = tuberTargets;

            this.volcanismRequirement = volcanismRequirement;
        }

        /**
         * Returns true if this rule considers the body a valid habitat.
         *
         * Right now this only checks the dimensions we actually
         * have in BodyAttributes: planetType, atmosphere, gravity,
         * temperature, and coarse volcanism. The extra fields
         * (pressure, regions, guardian, etc.) are stored but ignored
         * until BodyAttributes carries matching data and we decide
         * how to enforce them.
         */
        public boolean matches(BodyAttributes body) {
            if (body == null) {
                return false;
            }

            // Planet type constraint
            if (bodyTypes != null && !bodyTypes.isEmpty()
                    && !bodyTypes.contains(body.planetType)) {
                return false;
            }

            // Atmosphere constraint
            if (atmospheres != null && !atmospheres.isEmpty()
                    && !atmospheres.contains(body.atmosphere)) {
                return false;
            }

            // Gravity constraint
            if (body.gravity < minGravity || body.gravity > maxGravity) {
                return false;
            }

            // Temperature: any overlap between [body.min, body.max] and [rule.min, rule.max]
            if (body.tempKMax < minTempK || body.tempKMin > maxTempK) {
                return false;
            }

            // Volcanism (coarse)
            switch (volcanismRequirement) {
                case NO_VOLCANISM:
                    if (body.hasVolcanism) {
                        return false;
                    }
                    break;
                case VOLCANIC_ONLY:
                    if (!body.hasVolcanism) {
                        return false;
                    }
                    break;
                case ANY:
                default:
                    break;
            }

            // NOTE: pressure / regions / guardian / nebula / starClasses /
            // parentStars / tuberTargets / atmosphereComponents / bodies /
            // maxOrbitalPeriod / distance are not enforced yet.

            return true;
        }
    }

    /**
     * Constraint block for a single {genus, species}.
     * Contains the Vista value and a list of SpeciesRule entries.
     */
    /**
     * Constraint block for a single {genus, species}.
     * Contains the Vista value and a list of SpeciesRule entries.
     */
    public static final class SpeciesConstraint {

        private final String genus;
        private final String species;
        private final long baseValue;
        private final List<SpeciesRule> rules;

        public SpeciesConstraint(String genus,
                                 String species,
                                 long baseValue,
                                 List<SpeciesRule> rules) {
            this.genus = genus;
            this.species = species;
            this.baseValue = baseValue;
            this.rules = rules;
        }

        public String getGenus() {
            return genus;
        }

        public String getSpecies() {
            return species;
        }

        public long getBaseValue() {
            return baseValue;
        }

        public List<SpeciesRule> getRules() {
            return rules;
        }

        /**
         * Key used in the constraints map.
         */
        public String key() {
            return genus + " " + species;
        }
    }

    /* =====================================================================
     * BioCandidate
     * ===================================================================== */

    /**
     * A scored candidate prediction for a given body.
     */
    public static final class BioCandidate {

        private final SpeciesConstraint constraint;
        private final double score;
        private final String reason;

        public BioCandidate(SpeciesConstraint constraint, double score, String reason) {
            this.constraint = constraint;
            this.score = score;
            this.reason = reason;
        }

        public String getGenus() {
            return constraint.getGenus();
        }

        public String getSpecies() {
            return constraint.getSpecies();
        }

        public String getDisplayName() {
            return constraint.getGenus() + " " + constraint.getSpecies();
        }

        public long getBaseValue() {
            return constraint.getBaseValue();
        }

        public double getScore() {
            return score;
        }

        public String getReason() {
            return reason;
        }

        public SpeciesConstraint getConstraint() {
            return constraint;
        }

        /**
         * Simple estimated payout based on Vista base value.
         * If you want a different multiplier for first discovery, change it here.
         */
        public long getEstimatedPayout(boolean firstDiscovery) {
            if (firstDiscovery) {
                return constraint.getBaseValue() * 5L;  // rough ED-style bump
            }
            return constraint.getBaseValue();
        }

        @Override
        public String toString() {
            return "BioCandidate{" +
                    "name='" + getDisplayName() + '\'' +
                    ", score=" + score +
                    ", value=" + constraint.getBaseValue() +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    /* =====================================================================
     * Database and initialization
     * ===================================================================== */

    private static final Map<String, SpeciesConstraint> CONSTRAINTS = new LinkedHashMap<>();

    static {
        initConstraints();
    }

    /**
     * Populates the CONSTRAINTS map.
     *
     * IMPORTANT:
     *  Replace the body of this method with the contents of your
     *  generated file: ExobiologyData_initConstraints.txt
     *  (everything between "private static void initConstraints() {" and
     *  its closing brace).
     */
    private static void initConstraints() {
    	ExobiologyDataConstraints.initConstraints(CONSTRAINTS);
    }

    /* =====================================================================
     * Prediction
     * ===================================================================== */

    /**
     * Predict possible exobiology candidates for the given body.
     * Returns a list sorted by descending score and then baseValue.
     */
    public static List<BioCandidate> predict(BodyAttributes attrs) {
        if (attrs == null) {
            return Collections.emptyList();
        }

        List<BioCandidate> result = new ArrayList<>();

        for (SpeciesConstraint sc : CONSTRAINTS.values()) {
            SpeciesRule bestRule = null;
            double bestScore = 0.0;
            String bestReason = null;

            for (SpeciesRule rule : sc.getRules()) {
                if (!rule.matches(attrs)) {
                    continue;
                }

                double gScore = scoreInRange(attrs.gravity, rule.minGravity, rule.maxGravity);
                double tScore = scoreInRange(
                        0.5 * (attrs.tempKMin + attrs.tempKMax),
                        rule.minTempK,
                        rule.maxTempK
                );

                double score = 0.5 * (gScore + tScore);

                String reason = String.format(
                        Locale.ROOT,
                        "gravity=%.3f (%.3f–%.3f); temp=%.0f–%.0f K (%.0f–%.0f); atmo=%s; score=%.3f",
                        attrs.gravity, rule.minGravity, rule.maxGravity,
                        attrs.tempKMin, attrs.tempKMax,
                        rule.minTempK, rule.maxTempK,
                        attrs.atmosphere,
                        score
                );

                if (bestRule == null || score > bestScore) {
                    bestRule = rule;
                    bestScore = score;
                    bestReason = reason;
                }
            }

            if (bestRule != null) {
                result.add(new BioCandidate(sc, bestScore, bestReason));
            }
        }

        // Sort by score, then by Vista value
        result.sort(
                Comparator.comparingDouble(BioCandidate::getScore).reversed()
                        .thenComparingLong(BioCandidate::getBaseValue).reversed()
        );

        return result;
    }

    /**
     * Simple helper to score how "central" x is within [min,max].
     * 1.0 at the midpoint, 0.0 at or outside the bounds.
     */
    private static double scoreInRange(double x, double min, double max) {
        if (x < min || x > max) {
            return 0.0;
        }
        if (max <= min) {
            return 1.0;
        }
        double mid = 0.5 * (min + max);
        double half = 0.5 * (max - min);
        if (half <= 0.0) {
            return 1.0;
        }
        return 1.0 - Math.abs(x - mid) / half;
    }

    /* =====================================================================
     * Parsing helpers used by BodyInfo / SystemEventProcessor
     * ===================================================================== */

    public static PlanetType parsePlanetType(String planetClassRaw) {
        if (planetClassRaw == null || planetClassRaw.isEmpty()) {
            return PlanetType.UNKNOWN;
        }
        String pc = planetClassRaw.toLowerCase(Locale.ROOT);

        if (pc.contains("rocky ice")) {
            return PlanetType.ROCKY_ICE;
        }
        if (pc.contains("icy body") || pc.contains("icy world")) {
            return PlanetType.ICY;
        }
        if (pc.contains("metal-rich") || pc.contains("metal rich")) {
            return PlanetType.METAL_RICH;
        }
        if (pc.contains("high metal content")) {
            return PlanetType.HIGH_METAL;
        }
        if (pc.contains("rocky body") || pc.contains("rocky world")) {
            return PlanetType.ROCKY;
        }

        return PlanetType.OTHER;
    }

    public static AtmosphereType parseAtmosphere(String atmosphereRaw) {
        if (atmosphereRaw == null) {
            return AtmosphereType.UNKNOWN;
        }
        String at = atmosphereRaw.toLowerCase(Locale.ROOT).trim();
        if (at.isEmpty()
                || at.equals("none")
                || at.contains("no atmosphere")) {
            return AtmosphereType.NONE;
        }

        if (at.contains("carbon dioxide")) {
            return AtmosphereType.CO2;
        }
        if (at.contains("methane")) {
            return AtmosphereType.METHANE;
        }
        if (at.contains("nitrogen")) {
            return AtmosphereType.NITROGEN;
        }
        if (at.contains("oxygen")) {
            return AtmosphereType.OXYGEN;
        }
        if (at.contains("neon")) {
            return AtmosphereType.NEON;
        }
        if (at.contains("argon")) {
            return AtmosphereType.ARGON;
        }
        if (at.contains("water")) {
            return AtmosphereType.WATER;
        }
        if (at.contains("sulphur dioxide") || at.contains("sulfur dioxide")) {
            return AtmosphereType.SULPHUR_DIOXIDE;
        }
        if (at.contains("ammonia")) {
            return AtmosphereType.AMMONIA;
        }
        if (at.contains("helium")) {
            return AtmosphereType.HELIUM;
        }

        return AtmosphereType.OTHER;
    }

    /* =====================================================================
     * Utility lookup (optional)
     * ===================================================================== */

    public static SpeciesConstraint getConstraintFor(String genus, String species) {
        if (genus == null || species == null) {
            return null;
        }
        return CONSTRAINTS.get(genus + " " + species);
    }

    public static Map<String, SpeciesConstraint> getAllConstraints() {
        return Collections.unmodifiableMap(CONSTRAINTS);
    }
}
