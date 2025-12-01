package org.dce.ed.exobiology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static Vista Genomics values and simple genus prediction helpers
 * for Elite Dangerous Odyssey exobiology.
 *
 * NOTE: The species/value table below is intentionally partial – it
 * covers the high-value / common species you showed (Recepta, Cactoida,
 * Stratum, Frutexa, Tubus, Tussock, Bacterium, Concha, Fungoida, etc).
 * You can extend SPECIES_BASE_VALUES with more entries as needed.
 */
public final class ExobiologyData {

    private ExobiologyData() {
    }

    /**
     * Small immutable description of a predicted or known bio.
     */
    public static final class BioCandidate {

        private final String genus;
        private final String species;  // may be null for genus-only hints
        private final long baseValue;  // Vista base value, one sample
        private final double score;    // 0..1 heuristic likelihood
        private final String reason;   // human explanation

        public BioCandidate(String genus,
                            String species,
                            long baseValue,
                            double score,
                            String reason) {

            this.genus = genus;
            this.species = species;
            this.baseValue = baseValue;
            this.score = score;
            this.reason = reason;
        }

        public String getGenus() {
            return genus;
        }

        public String getSpecies() {
            return species;
        }

        /** Genus + species if present, otherwise just genus. */
        public String getDisplayName() {
            if (species == null || species.isEmpty()) {
                return genus;
            }
            return genus + " " + species;
        }

        public long getBaseValue() {
            return baseValue;
        }

        public double getScore() {
            return score;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return getDisplayName()
                    + " (" + String.format(Locale.US, "%,d Cr", baseValue)
                    + ", score=" + score + ")";
        }
    }

    /**
     * Minimal set of body attributes we use to guess what genera
     * might appear on a body before DSS / organic scans.
     */
    public static final class BodyAttributes {

        /** e.g. "Rocky", "High metal content", "Icy", etc. */
        public final String planetClass;

        /** Surface gravity in g (not m/s^2). NaN if unknown. */
        public final double gravityG;

        /** e.g. "None", "CO2", "Ammonia", "Water", "Neon rich", etc. */
        public final String atmosphere;

        /** Mean surface temperature in Kelvin (NaN if unknown). */
        public final double temperatureK;

        /** Whether the body has any volcanism. */
        public final boolean hasVolcanism;

        /** e.g. "Water", "Silicate", etc., may be null. */
        public final String volcanismType;

        public BodyAttributes(String planetClass,
                              double gravityG,
                              String atmosphere,
                              double temperatureK,
                              boolean hasVolcanism,
                              String volcanismType) {

            this.planetClass = planetClass;
            this.gravityG = gravityG;
            this.atmosphere = atmosphere;
            this.temperatureK = temperatureK;
            this.hasVolcanism = hasVolcanism;
            this.volcanismType = volcanismType;
        }
    }

    /**
     * Map of full species name ("Genus Species") to base value (Cr)
     * as paid by Vista Genomics for a single, non-first-logged sample.
     *
     * This is a partial table – extend as you like.
     */
    public static final Map<String, Long> SPECIES_BASE_VALUES;

    static {
        Map<String, Long> m = new LinkedHashMap<>();

        // Recepta (very high value)
        m.put("Recepta Deltahedronix", 16_202_800L);
        m.put("Recepta Conditivus",    14_313_700L);
        m.put("Recepta Umbrux",        12_934_900L);

        // Cactoida
        m.put("Cactoida Cortexum",     3_667_600L);
        m.put("Cactoida Lapis",        2_483_600L);
        m.put("Cactoida Peperatis",    2_483_600L);
        m.put("Cactoida Pullulanta",   3_667_600L);
        m.put("Cactoida Vermis",      16_202_800L);

        // Stratum
        m.put("Stratum Paleas",        1_362_000L);
        m.put("Stratum Tectonicas",   19_010_800L);
        m.put("Stratum Cucumisis",    16_202_800L);

        // Frutexa
        m.put("Frutexa Acus",          7_774_700L);
        m.put("Frutexa Fera",          1_632_500L);
        m.put("Frutexa Flabellum",     1_808_900L);
        m.put("Frutexa Sponsae",       5_988_000L);

        // Tubus
        m.put("Tubus Compagibus",      7_774_700L);
        m.put("Tubus Cavas",          11_873_200L);
        m.put("Tubus Rosarium",        2_637_500L);
        m.put("Tubus Conifer",         2_415_500L);

        // Tussock
        m.put("Tussock Ventusa",       3_277_700L);
        m.put("Tussock Ignis",         1_849_000L);
        m.put("Tussock Virgam",       14_313_700L);
        m.put("Tussock Stigmasis",    19_010_800L);
        m.put("Tussock Pennatis",      1_000_000L);
        m.put("Tussock Propagito",     1_000_000L);

        // Bacterium
        m.put("Bacterium Tela",        1_949_000L);
        m.put("Bacterium Aurasus",     1_000_000L);

        // Concha
        m.put("Concha Renibus",        4_572_400L);
        m.put("Concha Labiata",        2_352_400L);

        // Fungoida
        m.put("Fungoida Gelata",       3_330_300L);
        m.put("Fungoida Stabitis",     2_680_300L);

        SPECIES_BASE_VALUES = Collections.unmodifiableMap(m);
    }

    public static Long getBaseValue(String fullSpeciesName) {
        return SPECIES_BASE_VALUES.get(fullSpeciesName);
    }

    // ---------------------------------------------------------------------
    //  Simple genus-level prediction
    // ---------------------------------------------------------------------

    /**
     * Heuristic "what genera might appear here?" based on basic body
     * attributes.
     *
     * This is intentionally simple and conservative – it returns
     * BioCandidates for all species in any genus that looks plausible
     * for the body. You’ll usually call this after a Scan (but before
     * DSS) to populate “possible occurrences” rows under a body.
     */
    public static List<BioCandidate> predictGenera(BodyAttributes body) {
        List<BioCandidate> result = new ArrayList<>();

        String planet = safe(body.planetClass).toLowerCase(Locale.ROOT);
        String atmo   = safe(body.atmosphere).toLowerCase(Locale.ROOT);
        double g      = body.gravityG;
        double t      = body.temperatureK;
        boolean hasTemp = !Double.isNaN(t);

        boolean rockyLike =
                planet.contains("rocky") ||
                planet.contains("metal") ||
                planet.contains("high metal");

            boolean icyLike =
                planet.contains("icy");

        boolean co2Like =
                atmo.contains("co2");

        boolean ammoniaLike =
                atmo.contains("ammonia");

        boolean waterLike =
                atmo.contains("water");

        boolean sulphurDioxide =
                atmo.contains("sulphur");

        boolean neonLike =
                atmo.contains("neon");

        boolean argonLike =
                atmo.contains("argon");

        boolean methaneLike =
                atmo.contains("methane");

        boolean nitrogenLike =
                atmo.contains("nitrogen");

        boolean oxygenLike =
                atmo.contains("oxygen");

        boolean hasVolcanism = body.hasVolcanism;
        String volc = safe(body.volcanismType).toLowerCase(Locale.ROOT);
        boolean silicateVolc = hasVolcanism && volc.contains("silicate");
        boolean waterVolc    = hasVolcanism && volc.contains("water");

        // Recepta: SO2 atmosphere, low gravity, rocky/HMC or icy
        if (sulphurDioxide && (rockyLike || icyLike) && g > 0 && g < 0.28) {
            addAllSpeciesForGenus(result, "Recepta",
                    0.95,
                    "SO2 atmosphere + low gravity on rocky/icy world");
        }

        // Stratum: CO2 atmosphere on rocky/HMC with moderate gravity and temperature
        if (co2Like && rockyLike && hasTemp && t > 150 && t < 250) {
            double score = silicateVolc ? 0.95 : 0.8;
            String reason = "CO2 atmosphere on rocky/HMC world"
                    + (silicateVolc ? " with silicate volcanism" : "");
            addAllSpeciesForGenus(result, "Stratum", score, reason);
        }

        // Cactoida: CO2-rich rocky/HMC worlds in the 180–200 K band, low–moderate gravity
        if (co2Like && rockyLike && hasTemp && t >= 175 && t <= 205 && g > 0 && g < 0.6) {
            addAllSpeciesForGenus(result, "Cactoida",
                    0.85,
                    "Cool CO2 rocky/HMC world in Cactoida temperature band");
        }

        // Frutexa: rocky/HMC worlds with CO2 / water / ammonia at moderate gravity
        if (rockyLike && (co2Like || waterLike || ammoniaLike) && g >= 0.3 && g <= 1.3) {
            addAllSpeciesForGenus(result, "Frutexa",
                    0.75,
                    "Rocky/HMC world with dense atmosphere and moderate gravity");
        }

        // Tubus: low-g worlds (rocky or icy) with CO2 or SO2 atmospheres
        if ((rockyLike || icyLike) && (co2Like || sulphurDioxide) && g > 0 && g < 0.25) {
            addAllSpeciesForGenus(result, "Tubus",
                    0.8,
                    "Low-gravity CO2/SO2 world typical for Tubus");
        }

        // Tussock: very common on temperate rocky/icy bodies with almost any atmosphere
        if ((rockyLike || icyLike) && !atmo.isEmpty() && !atmo.equals("none")
                && hasTemp && t > 120 && t < 280) {
            addAllSpeciesForGenus(result, "Tussock",
                    0.7,
                    "Generic temperate rocky/icy body – typical Tussock habitat");
        }

        // Concha: water / water-rich atmospheres on rocky or icy worlds in 170–200 K range
        if ((rockyLike || icyLike) && waterLike && hasTemp && t >= 165 && t <= 205) {
            addAllSpeciesForGenus(result, "Concha",
                    0.8,
                    "Cool water-bearing world, good for Concha");
        }

        // Fungoida: usually on cold rocky/icy worlds with water/ammonia and often volcanism
        if ((rockyLike || icyLike) && (waterLike || ammoniaLike)
                && hasTemp && t >= 150 && t <= 210) {
            double score = (waterVolc || silicateVolc) ? 0.85 : 0.65;
            String reason = "Cold water/ammonia world"
                    + ((waterVolc || silicateVolc) ? " with volcanism" : "");
            addAllSpeciesForGenus(result, "Fungoida", score, reason);
        }

        // Fonticulua: usually on icy / rocky-ice worlds with noble-gas or light-gas atmospheres
        if (icyLike && (argonLike || neonLike || methaneLike || oxygenLike || nitrogenLike)) {
            addAllSpeciesForGenus(result, "Fonticulua",
                    0.75,
                    "Icy world with noble-gas / light-gas atmosphere – typical Fonticulua conditions");
        }

        // Clypeus: high-value genus on cool rocky/HMC with noble-gas atmospheres
        if (rockyLike && (argonLike || neonLike) && hasTemp && t >= 180 && t <= 210) {
            addAllSpeciesForGenus(result, "Clypeus",
                    0.8,
                    "Rocky/HMC body with noble-gas atmosphere in Clypeus temp range");
        }

        // Osseus: often shares conditions with Fonticulua / Tubus on cold, thin-atmo worlds
        if ((rockyLike || icyLike)
                && (co2Like || waterLike || argonLike || neonLike)
                && hasTemp && t >= 160 && t <= 210) {
            addAllSpeciesForGenus(result, "Osseus",
                    0.7,
                    "Cold world with thin atmosphere suitable for Osseus");
        }

        // Aleoida: often on cool rocky bodies with CO2 / water and mild gravity
        if (rockyLike && (co2Like || waterLike)
                && hasTemp && t >= 170 && t <= 210
                && g > 0 && g < 0.8) {
            addAllSpeciesForGenus(result, "Aleoida",
                    0.7,
                    "Cool rocky world with CO2/water matching Aleoida conditions");
        }

        // Generic Bacterium hint for any non-vacuum atmosphere
        if (!atmo.isEmpty() && !atmo.equals("none")) {
            addAllSpeciesForGenus(result, "Bacterium",
                    0.4,
                    "Non-vacuum atmosphere supports various Bacterium species");
        }

        // Electricae: icy, low-g worlds with noble-gas atmospheres
        if (icyLike && (neonLike || argonLike) && g > 0 && g < 0.3) {
            addAllSpeciesForGenus(result, "Electricae",
                    0.5,
                    "Icy low-g world with noble-gas atmosphere");
        }

        return result;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static void addAllSpeciesForGenus(List<BioCandidate> out,
                                              String genus,
                                              double score,
                                              String reason) {

        String prefix = genus + " ";
        for (Map.Entry<String, Long> e : SPECIES_BASE_VALUES.entrySet()) {
            String name = e.getKey();
            if (name.startsWith(prefix)) {
                String species = name.substring(prefix.length());
                out.add(new BioCandidate(genus, species, e.getValue(), score, reason));
            }
        }
    }
}
