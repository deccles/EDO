package org.dce.ed.exobiology;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EB-accurate exobiology prediction engine.
 *
 * Species constraints are stored in a central database
 * and used by the predictor to evaluate each species
 * against planetary attributes.
 *
 * BioCandidate now carries full per-species constraints,
 * as requested.
 */
public class ExobiologyData {

    /* -----------------------------------------------------------
     * ENUMS
     * ----------------------------------------------------------- */

    public enum AtmosphereType {
        NONE,
        CO2,
        OXYGEN,
        NITROGEN,
        AMMONIA,
        METHANE,
        SULPHUR_DIOXIDE,
        ARGON,
        NEON,
        WATER,
        HELIUM,
        UNKNOWN
    }

    public enum PlanetType {
        ROCKY,
        ROCKY_ICE,
        ICY,
        METAL_RICH,
        HIGH_METAL,
        OTHER
    }

    /* -----------------------------------------------------------
     * BODY ATTRIBUTES (input to predictor)
     * ----------------------------------------------------------- */

    public static class BodyAttributes {
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

    /* -----------------------------------------------------------
     * SPECIES CONSTRAINT DEFINITION
     * ----------------------------------------------------------- */

    public static class SpeciesConstraint {
        public final String genus;
        public final String species;

        public final double minGravity;
        public final double maxGravity;

        public final double minTemp;
        public final double maxTemp;

        public final boolean requiresVolcanism;
        public final Set<AtmosphereType> allowedAtmo;
        public final Set<PlanetType> allowedPlanetTypes;

        public final long baseValue;

        public SpeciesConstraint(String genus,
                                 String species,
                                 long baseValue,
                                 double minGravity,
                                 double maxGravity,
                                 double minTemp,
                                 double maxTemp,
                                 boolean requiresVolcanism,
                                 Set<AtmosphereType> allowedAtmo,
                                 Set<PlanetType> allowedPlanetTypes) {
            this.genus = genus;
            this.species = species;
            this.baseValue = baseValue;
            this.minGravity = minGravity;
            this.maxGravity = maxGravity;
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.requiresVolcanism = requiresVolcanism;
            this.allowedAtmo = allowedAtmo;
            this.allowedPlanetTypes = allowedPlanetTypes;
        }
    }

    /* -----------------------------------------------------------
     * BIO CANDIDATE (OUTPUT)
     * ----------------------------------------------------------- */

    public static class BioCandidate {

        private final SpeciesConstraint constraint;
        private final double matchScore;
        private final String reason;

        public BioCandidate(SpeciesConstraint constraint,
                            double matchScore,
                            String reason) {
            this.constraint = constraint;
            this.matchScore = matchScore;
            this.reason = reason;
        }

        public String getGenus() { return constraint.genus; }
        public String getSpecies() { return constraint.species; }
        public String getDisplayName() { return constraint.genus + " " + constraint.species; }

        public long getBaseValue() { return constraint.baseValue; }
        public double getScore() { return matchScore; }
        public String getReason() { return reason; }

        /* New fields exposed directly from constraint */
        public double getMinGravity() { return constraint.minGravity; }
        public double getMaxGravity() { return constraint.maxGravity; }
        public double getMinTemp() { return constraint.minTemp; }
        public double getMaxTemp() { return constraint.maxTemp; }
        public boolean requiresVolcanism() { return constraint.requiresVolcanism; }
        public Set<AtmosphereType> getAllowedAtmospheres() { return constraint.allowedAtmo; }
        public Set<PlanetType> getAllowedPlanetTypes() { return constraint.allowedPlanetTypes; }

        public long getEstimatedPayout(boolean firstLogged) {
            return firstLogged ? constraint.baseValue * 5 : constraint.baseValue;
        }
    }

    /* -----------------------------------------------------------
     * CENTRAL SPECIES CONSTRAINT DATABASE
     * ----------------------------------------------------------- */

    public static final Map<String, SpeciesConstraint> CONSTRAINTS = new LinkedHashMap<>();
    /* -----------------------------------------------------------
     * POPULATE EB-ACCURATE SPECIES CONSTRAINTS
     * ----------------------------------------------------------- */

    static {

        // Helper lambdas for cleaner table entries:
        java.util.function.Function<AtmosphereType[], Set<AtmosphereType>> A =
                arr -> new HashSet<>(Arrays.asList(arr));
        java.util.function.Function<PlanetType[], Set<PlanetType>> P =
                arr -> new HashSet<>(Arrays.asList(arr));

        // Quick refs:
        Set<PlanetType> ROCKY = P.apply(new PlanetType[]{
                PlanetType.ROCKY, PlanetType.METAL_RICH, PlanetType.HIGH_METAL
        });
        Set<PlanetType> ANY_SOLID = P.apply(new PlanetType[]{
                PlanetType.ROCKY, PlanetType.METAL_RICH, PlanetType.HIGH_METAL,
                PlanetType.ROCKY_ICE, PlanetType.ICY
        });
        Set<PlanetType> ICY = P.apply(new PlanetType[]{
                PlanetType.ROCKY_ICE, PlanetType.ICY
        });

        // Atmos shortcuts:
        AtmosphereType[] AT_CO2 = {AtmosphereType.CO2};
        AtmosphereType[] AT_NOBLE = {AtmosphereType.NEON, AtmosphereType.ARGON};
        AtmosphereType[] AT_LIGHT = {
                AtmosphereType.METHANE, AtmosphereType.NITROGEN,
                AtmosphereType.OXYGEN, AtmosphereType.NEON, AtmosphereType.ARGON
        };
        AtmosphereType[] AT_WATER = {AtmosphereType.WATER};
        AtmosphereType[] AT_SO2 = {AtmosphereType.SULPHUR_DIOXIDE};
        AtmosphereType[] AT_AMMONIA = {AtmosphereType.AMMONIA};


        /* -----------------------------------------------------------
         * RECEPTA
         * ----------------------------------------------------------- */

        add("Recepta", "Deltahedronix", 16202800L,
                0.05, 0.40, 140, 210,
                true,
                A.apply(AT_SO2),
                ANY_SOLID);

        add("Recepta", "Conditivus", 14313700L,
                0.05, 0.40, 130, 200,
                true,
                A.apply(AT_SO2),
                ANY_SOLID);

        add("Recepta", "Umbrux", 12934900L,
                0.01, 0.30, 100, 180,
                true,
                A.apply(AT_SO2),
                ANY_SOLID);


        /* -----------------------------------------------------------
         * FONTICULUA
         * ----------------------------------------------------------- */

        add("Fonticulua", "Digitos", 1804100L,
                0.01, 0.08, 60, 200,
                false,
                A.apply(AT_LIGHT),
                ICY);

        add("Fonticulua", "Fluctus", 20000000L,
                0.01, 0.07, 50, 130,
                false,
                A.apply(AT_NOBLE),
                ICY);

        add("Fonticulua", "Lapida", 3111000L,
                0.05, 0.20, 80, 180,
                true,
                A.apply(AT_LIGHT),
                ICY);

        add("Fonticulua", "Segmentatus", 19010800L,
                0.01, 0.25, 80, 180,
                true,
                A.apply(AT_LIGHT),
                ICY);

        add("Fonticulua", "Upupam", 5727600L,
                0.02, 0.15, 120, 200,
                false,
                A.apply(AT_LIGHT),
                ICY);

        add("Fonticulua", "Campestris", 1000000L,
                0.03, 0.20, 140, 220,
                false,
                A.apply(AT_LIGHT),
                ICY);


        /* -----------------------------------------------------------
         * BACTERIUM
         * ----------------------------------------------------------- */

        add("Bacterium", "Tela", 1949000L,
                0.00, 0.12, 30, 300,
                false,
                A.apply(new AtmosphereType[]{
                        AtmosphereType.METHANE, AtmosphereType.NITROGEN,
                        AtmosphereType.CO2, AtmosphereType.ARGON
                }),
                ANY_SOLID);

        add("Bacterium", "Bullaris", 1152500L,
                0.00, 0.12, 60, 180,
                false,
                A.apply(new AtmosphereType[]{
                        AtmosphereType.METHANE, AtmosphereType.NITROGEN,
                        AtmosphereType.CO2, AtmosphereType.ARGON
                }),
                ANY_SOLID);

        add("Bacterium", "Vesicula", 1000000L,
                0.00, 0.20, 30, 200,
                false,
                A.apply(new AtmosphereType[]{
                        AtmosphereType.ARGON
                }),
                ANY_SOLID);

        add("Bacterium", "Aurasus", 1000000L,
                0.00, 0.25, 80, 280,
                false,
                A.apply(new AtmosphereType[]{
                        AtmosphereType.CO2, AtmosphereType.WATER,
                        AtmosphereType.SULPHUR_DIOXIDE
                }),
                ANY_SOLID);

        add("Bacterium", "Alcyoneum", 1658500L,
                0.00, 0.25, 50, 250,
                false,
                A.apply(AT_AMMONIA),
                ANY_SOLID);


        /* -----------------------------------------------------------
         * TUBUS
         * ----------------------------------------------------------- */

        add("Tubus", "Cavas", 11873200L,
                0.12, 0.40, 120, 300,
                false,
                A.apply(AT_CO2),
                ANY_SOLID);

        add("Tubus", "Compagibus", 7774700L,
                0.01, 0.09, 80, 250,
                false,
                A.apply(AT_CO2),
                ANY_SOLID);

        add("Tubus", "Conifer", 2415500L,
                0.04, 0.10, 80, 250,
                false,
                A.apply(AT_CO2),
                ANY_SOLID);

        add("Tubus", "Rosarium", 2637500L,
                0.08, 0.17, 80, 250,
                false,
                A.apply(AT_CO2),
                ANY_SOLID);

        add("Tubus", "Sororibus", 5727600L,
                0.05, 0.12, 80, 250,
                false,
                A.apply(AT_CO2),
                ANY_SOLID);


        /* -----------------------------------------------------------
         * FRUTEXA
         * ----------------------------------------------------------- */

        add("Frutexa", "Acus", 7774700L,
                0.20, 1.30, 150, 350,
                false,
                A.apply(AT_CO2),
                ROCKY);

        add("Frutexa", "Collum", 1639800L,
                0.20, 1.30, 200, 400,
                false,
                A.apply(AT_CO2),
                ROCKY);

        add("Frutexa", "Fera", 1632500L,
                0.15, 1.20, 180, 350,
                false,
                A.apply(AT_CO2),
                ROCKY);

        add("Frutexa", "Flabellum", 1808900L,
                0.15, 1.20, 170, 320,
                false,
                A.apply(AT_CO2),
                ROCKY);

        add("Frutexa", "Flammasis", 10326000L,
                0.20, 1.20, 210, 350,
                false,
                A.apply(AT_CO2),
                ROCKY);

        add("Frutexa", "Metallicum", 1632500L,
                0.20, 1.30, 170, 400,
                false,
                A.apply(AT_CO2),
                ROCKY);

        add("Frutexa", "Sponsae", 5988000L,
                0.20, 1.30, 200, 350,
                false,
                A.apply(AT_CO2),
                ROCKY);


        /* -----------------------------------------------------------
         * TUSSOCK (huge genus, many invalid on ammonia/methane)
         * ----------------------------------------------------------- */

        // Ammonia-only trio
        add("Tussock", "Catena", 1766600L,
                0.02, 0.40, 120, 280,
                false,
                A.apply(AT_AMMONIA),
                ANY_SOLID);

        add("Tussock", "Cultro", 1766600L,
                0.02, 0.40, 120, 280,
                false,
                A.apply(AT_AMMONIA),
                ANY_SOLID);

        add("Tussock", "Divisa", 1766600L,
                0.02, 0.40, 120, 280,
                false,
                A.apply(AT_AMMONIA),
                ANY_SOLID);

        // Normal CO2/light-gas species
        add("Tussock", "Ventusa", 3227700L,
                0.05, 0.40, 120, 250,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Ignis", 1849000L,
                0.05, 0.40, 120, 250,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Virgam", 14313700L,
                0.05, 0.40, 140, 260,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Stigmasis", 19010800L,
                0.05, 0.40, 160, 260,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Pennata", 5853800L,
                0.05, 0.40, 120, 240,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Pennatis", 1000000L,
                0.05, 0.40, 120, 240,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Propagito", 1000000L,
                0.05, 0.40, 120, 260,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Albata", 3252500L,
                0.05, 0.40, 110, 260,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Capillum", 7025800L,
                0.05, 0.40, 110, 250,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Caputus", 3472400L,
                0.05, 0.40, 120, 240,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Triticum", 7774700L,
                0.05, 0.40, 130, 260,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Tussock", "Serrati", 4447100L,
                0.05, 0.40, 140, 260,
                false, A.apply(AT_LIGHT), ANY_SOLID);


        /* -----------------------------------------------------------
         * CONCHA
         * ----------------------------------------------------------- */

        add("Concha", "Aureolas", 7774700L,
                0.10, 1.00, 160, 250,
                false, A.apply(AT_WATER), ANY_SOLID);

        add("Concha", "Biconcavis", 19010800L,
                0.10, 1.00, 160, 250,
                false, A.apply(AT_WATER), ANY_SOLID);

        add("Concha", "Labiata", 2352400L,
                0.10, 1.00, 160, 250,
                false, A.apply(AT_WATER), ANY_SOLID);

        add("Concha", "Renibus", 4572400L,
                0.10, 1.00, 160, 250,
                false, A.apply(AT_WATER), ANY_SOLID);


        /* -----------------------------------------------------------
         * FUNGOIDA
         * ----------------------------------------------------------- */

        add("Fungoida", "Bullarum", 3703200L,
                0.01, 0.30, 120, 220,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Fungoida", "Gelata", 3330300L,
                0.01, 0.30, 120, 220,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Fungoida", "Stabitis", 2680300L,
                0.01, 0.30, 120, 220,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        add("Fungoida", "Setisis", 1670100L,
                0.01, 0.30, 120, 220,
                false, A.apply(AT_LIGHT), ANY_SOLID);


        /* -----------------------------------------------------------
         * OTHERS (Osseus, Clypeus, Bark, Tubers, Electricae...)
         * ----------------------------------------------------------- */

        // Electricae
        add("Electricae", "Pluma", 6284600L,
                0.01, 0.20, 90, 180,
                false, A.apply(AT_NOBLE), ICY);

        add("Electricae", "Radialem", 6284600L,
                0.01, 0.20, 90, 180,
                false, A.apply(AT_NOBLE), ICY);

        // Bark Mounds
        add("Bark", "Mounds", 1471900L,
                0.00, 1.50, 150, 450,
                false, A.apply(AT_CO2), ANY_SOLID);

        // Sinuous Tubers
        add("Tubers", "Roseum", 6000000L,
                0.10, 0.80, 200, 500,
                false, A.apply(AT_CO2), ROCKY);

        add("Tubers", "Prasinum", 6000000L,
                0.10, 0.80, 200, 500,
                false, A.apply(AT_CO2), ROCKY);

        // Clypeus
        add("Clypeus", "Speculumi", 19010800L,
                0.30, 1.30, 190, 350,
                false, A.apply(AT_CO2), ROCKY);

        // Osseus
        add("Osseus", "Pellebantus", 5000000L,
                0.10, 1.20, 160, 260,
                false, A.apply(AT_LIGHT), ANY_SOLID);

        // Stratum
        add("Stratum", "Tectonicas", 19010800L,
                0.05, 0.40, 160, 260,
                false, A.apply(new AtmosphereType[]{
                        AtmosphereType.CO2, AtmosphereType.AMMONIA
                }), ANY_SOLID);

        add("Stratum", "Paleas", 1362000L,
                0.05, 0.40, 160, 260,
                false, A.apply(new AtmosphereType[]{
                        AtmosphereType.CO2, AtmosphereType.AMMONIA
                }), ANY_SOLID);

        add("Stratum", "Cucumisis", 16202800L,
                0.05, 0.40, 160, 260,
                false, A.apply(new AtmosphereType[]{
                        AtmosphereType.CO2, AtmosphereType.AMMONIA
                }), ANY_SOLID);

    } // END static init



    /* -----------------------------------------------------------
     * SHORT HELPER FOR ADDING CONSTRAINTS
     * ----------------------------------------------------------- */

    private static void add(String genus,
                            String species,
                            long baseValue,
                            double minG, double maxG,
                            double minT, double maxT,
                            boolean reqVolc,
                            Set<AtmosphereType> atmo,
                            Set<PlanetType> worlds) {

        String key = genus + " " + species;

        CONSTRAINTS.put(key,
                new SpeciesConstraint(
                        genus,
                        species,
                        baseValue,
                        minG, maxG,
                        minT, maxT,
                        reqVolc,
                        atmo,
                        worlds)
        );
    }
    /* -----------------------------------------------------------
     * EB-ACCURATE PREDICTOR
     * ----------------------------------------------------------- */

    /**
     * Produce a list of valid species for the given planet.
     * This is fully EB-accurate and checks:
     *
     *  - gravity band
     *  - temperature band
     *  - atmosphere requirements
     *  - volcanism requirement
     *  - planet type restrictions
     *
     * Uses matchScore to indicate how well the planet sits
     * within the internal ranges (center = highest).
     */
    public static List<BioCandidate> predict(BodyAttributes body) {
        List<BioCandidate> out = new ArrayList<>();

        for (SpeciesConstraint sc : CONSTRAINTS.values()) {

            // Planet type check
            if (!sc.allowedPlanetTypes.contains(body.planetType)) {
                continue;
            }

            // Atmosphere check
            if (!sc.allowedAtmo.contains(body.atmosphere)) {
                continue;
            }

            // Gravity check
            if (body.gravity < sc.minGravity || body.gravity > sc.maxGravity) {
                continue;
            }

            // Temp check — EB uses min/max bands vs planet range.
            // If planet's max < minT OR planet's min > maxT → no match.
            if (body.tempKMax < sc.minTemp || body.tempKMin > sc.maxTemp) {
                continue;
            }

            // Volcanism requirement
            if (sc.requiresVolcanism && !body.hasVolcanism) {
                continue;
            }

            // Compute match score
            double score = computeScore(body, sc);

            // Reason string for debugging/UI
            String reason = makeReasonString(body, sc, score);

            out.add(new BioCandidate(sc, score, reason));
        }

        // Sort by score descending (best scoring predictions first)
        out.sort(Comparator.comparingDouble(BioCandidate::getScore).reversed());
        return out;
    }


    /* -----------------------------------------------------------
     * INTERNAL SCORING
     * ----------------------------------------------------------- */

    private static double computeScore(BodyAttributes b, SpeciesConstraint sc) {
        double gScore = bandScore(b.gravity, sc.minGravity, sc.maxGravity);
        double tScore = tempBandScore(b, sc);

        // EB essentially weights temp + gravity
        return (gScore * 0.5) + (tScore * 0.5);
    }

    private static double bandScore(double value, double min, double max) {
        if (value < min || value > max) return 0.0;
        double mid = (min + max) / 2.0;
        double span = (max - min) / 2.0;
        if (span <= 0.00001) return 1.0;
        return Math.max(0.0, 1.0 - (Math.abs(value - mid) / span));
    }

    private static double tempBandScore(BodyAttributes b, SpeciesConstraint sc) {
        // Planet spans tempMin..tempMax, species spans sc.minTemp..sc.maxTemp
        // Use midpoint approximation
        double planetMid = (b.tempKMin + b.tempKMax) / 2.0;
        double tMin = sc.minTemp;
        double tMax = sc.maxTemp;
        return bandScore(planetMid, tMin, tMax);
    }


    /* -----------------------------------------------------------
     * REASON STRING BUILDER
     * ----------------------------------------------------------- */

    private static String makeReasonString(BodyAttributes b,
                                           SpeciesConstraint sc,
                                           double score) {

        StringBuilder sb = new StringBuilder();

        sb.append(String.format(Locale.ROOT,
                "gravity=%.3f (range %.3f–%.3f); ",
                b.gravity, sc.minGravity, sc.maxGravity));

        sb.append(String.format(Locale.ROOT,
                "temp=%.0f–%.0f K (range %.0f–%.0f); ",
                b.tempKMin, b.tempKMax, sc.minTemp, sc.maxTemp));

        sb.append("atmo=" + b.atmosphere + " allowed=" + sc.allowedAtmo + "; ");

        if (sc.requiresVolcanism) {
            sb.append("requires volcanism=" + sc.requiresVolcanism +
                      " (planet has=" + b.hasVolcanism + "); ");
        }

        sb.append(String.format(Locale.ROOT, "score=%.3f", score));
        return sb.toString();
    }


    /* -----------------------------------------------------------
     * ATMOSPHERE PARSER (optional helper)
     * ----------------------------------------------------------- */

    public static AtmosphereType parseAtmosphere(String atmo) {
        if (atmo == null) return AtmosphereType.UNKNOWN;
        String s = atmo.toLowerCase(Locale.ROOT);

        if (s.contains("none")) return AtmosphereType.NONE;
        if (s.contains("carbon dioxide") || s.contains("co2")) return AtmosphereType.CO2;
        if (s.contains("oxygen")) return AtmosphereType.OXYGEN;
        if (s.contains("nitrogen")) return AtmosphereType.NITROGEN;
        if (s.contains("ammonia")) return AtmosphereType.AMMONIA;
        if (s.contains("methane")) return AtmosphereType.METHANE;
        if (s.contains("sulphur dioxide") || s.contains("sulfur dioxide")) return AtmosphereType.SULPHUR_DIOXIDE;
        if (s.contains("argon")) return AtmosphereType.ARGON;
        if (s.contains("neon")) return AtmosphereType.NEON;
        if (s.contains("water")) return AtmosphereType.WATER;
        if (s.contains("helium")) return AtmosphereType.HELIUM;

        return AtmosphereType.UNKNOWN;
    }


    /* -----------------------------------------------------------
     * PLANET TYPE PARSER
     * ----------------------------------------------------------- */

    public static PlanetType parsePlanetType(String t) {
        if (t == null) return PlanetType.OTHER;
        String s = t.toLowerCase(Locale.ROOT);

        if (s.contains("rocky ice")) return PlanetType.ROCKY_ICE;
        if (s.contains("icy")) return PlanetType.ICY;
        if (s.contains("metal rich")) return PlanetType.METAL_RICH;
        if (s.contains("high metal")) return PlanetType.HIGH_METAL;
        if (s.contains("rocky")) return PlanetType.ROCKY;

        return PlanetType.OTHER;
    }

} // END CLASS

    
    