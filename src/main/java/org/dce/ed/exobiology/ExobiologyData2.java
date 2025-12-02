//package org.dce.ed.exobiology;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//
///**
// * Static Vista Genomics values and simple genus prediction helpers
// * for Elite Dangerous Odyssey exobiology.
// *
// * NOTE: The species/value table below is intentionally partial – it
// * covers the high-value / common species you showed (Recepta, Cactoida,
// * Stratum, Frutexa, Tubus, Tussock, Bacterium, Concha, Fungoida, etc).
// *
// * You can extend this table over time without changing any code:
// * everything else keys off the genus/species names.
// */
//public class ExobiologyData {
//
//    /**
//     * Small immutable description of a predicted or known bio.
//     */
//	public static class BioCandidate {
//	    private final String genus;
//	    private final String species;  // may be null for genus-only hints
//	    private final long baseValue;  // Vista base value, one sample
//	    private final double score;    // 0..1 heuristic likelihood
//	    private final String reason;   // human explanation
//
//	    public BioCandidate(String genus,
//	                        String species,
//	                        long baseValue,
//	                        double score,
//	                        String reason) {
//	        this.genus = genus;
//	        this.species = species;
//	        this.baseValue = baseValue;
//	        this.score = score;
//	        this.reason = reason;
//	    }
//
//	    public String getGenus() {
//	        return genus;
//	    }
//
//	    public String getSpecies() {
//	        return species;
//	    }
//
//	    /**
//	     * Genus + species if present, otherwise just genus.
//	     */
//	    public String getDisplayName() {
//	        if (species == null || species.isEmpty()) {
//	            return genus;
//	        }
//	        return genus + " " + species;
//	    }
//
//	    public long getBaseValue() {
//	        return baseValue;
//	    }
//
//	    public double getScore() {
//	        return score;
//	    }
//
//	    public String getReason() {
//	        return reason;
//	    }
//
//	    @Override
//	    public String toString() {
//	        return "BioCandidate{" +
//	                "name='" + getDisplayName() + '\'' +
//	                ", baseValue=" + baseValue +
//	                ", score=" + score +
//	                ", reason='" + reason + '\'' +
//	                '}';
//	    }
//
//	    /**
//	     * Estimated payout for this bio candidate.
//	     *
//	     * For now we use the same behaviour you had before:
//	     * - no first-log bonus: base value
//	     * - first-log bonus: 5 × base value
//	     *
//	     * If you later wire in a more accurate first-discovery multiplier,
//	     * you can just update this method.
//	     */
//	    public long getEstimatedPayout(boolean assumeFirstLogged) {
//	        long base = getBaseValue();
//	        return assumeFirstLogged ? base * 5L : base;
//	    }
//	}
//
//    /**
//     * Small immutable view of a body's attributes used for prediction.
//     */
//    public static class BodyAttributes {
//        public final String planetClass;
//        public final double gravityG;
//        public final String atmosphere;
//
//        /** Mean surface temperature in Kelvin (NaN if unknown). */
//        public final double temperatureK;
//
//        /** Whether the body has any volcanism. */
//        public final boolean hasVolcanism;
//
//        /** e.g. "Water", "Silicate", etc., may be null. */
//        public final String volcanismType;
//
//        public BodyAttributes(String planetClass,
//                              double gravityG,
//                              String atmosphere,
//                              double temperatureK,
//                              boolean hasVolcanism,
//                              String volcanismType) {
//            this.planetClass = planetClass;
//            this.gravityG = gravityG;
//            this.atmosphere = atmosphere;
//            this.temperatureK = temperatureK;
//            this.hasVolcanism = hasVolcanism;
//            this.volcanismType = volcanismType;
//        }
//    }
//
//    /**
//     * Map of full species name ("Genus Species") to base value (Cr)
//     * as paid by Vista Genomics for a single, non-first-logged sample.
//     *
//     * This is a partial table – extend as you like.
//     */
//    public static final Map<String, Long> SPECIES_BASE_VALUES;
//
//    static {
//        Map<String, Long> m = new LinkedHashMap<>();
//
//        // Recepta (very high value)
//        m.put("Recepta Deltahedronix", 16_202_800L);
//        m.put("Recepta Conditivus",    14_313_700L);
//        m.put("Recepta Umbrux",        12_934_900L);
//
//        // Electricae  (thin He/Ne/Ar atmospheres, icy, g < 0.27)
//        m.put("Electricae Pluma",      6_284_600L);
//        m.put("Electricae Radialem",   6_284_600L);
//
//        // Fonticulua  (Icy / rocky-ice with noble/light gas atmospheres)
//        m.put("Fonticulua Campestris",  1_000_000L);
//        m.put("Fonticulua Digitos",     1_804_100L);
//        m.put("Fonticulua Fluctus",    20_000_000L);
//        m.put("Fonticulua Lapida",      3_111_000L);
//        m.put("Fonticulua Segmentatus",19_010_800L);
//        m.put("Fonticulua Upupam",      5_727_600L);
//
//        // Bacterium
//        m.put("Bacterium Tela",         1_949_000L);
//        m.put("Bacterium Aurasus",      1_000_000L);
//        m.put("Bacterium Vesicula",     1_000_000L);
//        m.put("Bacterium Alcyoneum",    1_658_500L);
//
//        // Cactoida
//        m.put("Cactoida Cortexum",      3_667_600L);
//        m.put("Cactoida Lapis",         2_483_600L);
//        m.put("Cactoida Peperatis",     2_483_600L);
//        m.put("Cactoida Pullulanta",    3_667_600L);
//        m.put("Cactoida Vermis",       16_202_800L);
//
//        // Stratum
//        m.put("Stratum Paleas",         1_362_000L);
//        m.put("Stratum Tectonicas",    19_010_800L);
//        m.put("Stratum Cucumisis",     16_202_800L);
//
//        // Frutexa
//        m.put("Frutexa Acus",           7_774_700L);
//        m.put("Frutexa Collum",         1_639_800L);
//        m.put("Frutexa Fera",           1_632_500L);
//        m.put("Frutexa Flabellum",      1_808_900L);
//        m.put("Frutexa Flammasis",     10_326_000L);
//        m.put("Frutexa Metallicum",    1_632_500L);
//        m.put("Frutexa Sponsae",       5_988_000L);
//
//        // Tubus
//        m.put("Tubus Cavas",           11_873_200L);
//        m.put("Tubus Compagibus",      7_774_700L);
//        m.put("Tubus Conifer",         2_415_500L);
//        m.put("Tubus Rosarium",        2_637_500L);
//        m.put("Tubus Sororibus",       5_727_600L);
//
//        // Tussock
//        m.put("Tussock Ventusa",        3_227_700L);
//        m.put("Tussock Ignis",          1_849_000L);
//        m.put("Tussock Virgam",        14_313_700L);
//        m.put("Tussock Stigmasis",     19_010_800L);
//        m.put("Tussock Pennata",        5_853_800L);
//        m.put("Tussock Pennatis",       1_000_000L);
//        m.put("Tussock Propagito",      1_000_000L);
//        m.put("Tussock Albata",         3_252_500L);
//        m.put("Tussock Capillum",       7_025_800L);
//        m.put("Tussock Caputus",        3_472_400L);
//        m.put("Tussock Triticum",       7_774_700L);
//        m.put("Tussock Serrati",        4_447_100L);
//        m.put("Tussock Catena",         1_766_600L);
//        m.put("Tussock Cultro",         1_766_600L);
//        m.put("Tussock Divisa",         1_766_600L);
//
//        // Concha
//        m.put("Concha Aureolas",        7_774_700L);
//        m.put("Concha Biconcavis",     19_010_800L);
//        m.put("Concha Labiata",         2_352_400L);
//        m.put("Concha Renibus",         4_572_400L);
//
//        // Fungoida
//        m.put("Fungoida Bullarum",      3_703_200L);
//        m.put("Fungoida Gelata",        3_330_300L);
//        m.put("Fungoida Stabitis",      2_680_300L);
//        m.put("Fungoida Setisis",       1_670_100L);
//
//        // Bark / Tubers (single-species handled via addSingleSpecies)
//        m.put("Bark Mounds",                1_471_900L);
//        m.put("Roseum Sinuous Tubers",      6_000_000L); // placeholder
//        m.put("Prasinum Sinuous Tubers",    6_000_000L); // placeholder
//
//        SPECIES_BASE_VALUES = Collections.unmodifiableMap(m);
//    }
//
//    public static Long getBaseValue(String fullSpeciesName) {
//        return SPECIES_BASE_VALUES.get(fullSpeciesName);
//    }
//
//    // ---------------------------------------------------------------------
//    //  Simple genus-level prediction
//    // ---------------------------------------------------------------------
//
//    /**
//     * Heuristic "what genera/species might appear here?" based on basic body
//     * attributes.
//     *
//     * This is intentionally simple and conservative – its job is to give
//     * you a short list for a single world, not to be an exhaustive Codex.
//     */
//    public static List<BioCandidate> predictGenera(BodyAttributes body) {
//        List<BioCandidate> result = new ArrayList<>();
//
//        String planet = safe(body.planetClass).toLowerCase(Locale.ROOT);
//        String atmo   = safe(body.atmosphere).toLowerCase(Locale.ROOT);
//        double g      = body.gravityG;
//        double t      = body.temperatureK;
//        boolean hasTemp = !Double.isNaN(t);
//
//        boolean rockyLike =
//                planet.contains("rocky") ||
//                planet.contains("metal") ||
//                planet.contains("high metal");
//
//        boolean icyLike =
//                planet.contains("icy");
//
//        // Atmosphere classification – tuned to ED strings
//        boolean co2Like =
//                atmo.contains("carbon dioxide") || atmo.contains("co2");
//
//        boolean ammoniaLike =
//                atmo.contains("ammonia");
//
//        boolean waterLike =
//                atmo.contains("water");
//
//        boolean sulphurDioxide =
//                atmo.contains("sulphur dioxide") || atmo.contains("sulfur dioxide");
//
//        boolean neonLike =
//                atmo.contains("neon");
//
//        boolean argonLike =
//                atmo.contains("argon");
//
//        boolean methaneLike =
//                atmo.contains("methane");
//
//        boolean nitrogenLike =
//                atmo.contains("nitrogen");
//
//        boolean oxygenLike =
//                atmo.contains("oxygen");
//
//        boolean nobleGasLike = argonLike || neonLike;
//
//        boolean waterVolc =
//                safe(body.volcanismType).toLowerCase(Locale.ROOT).contains("water");
//
//        boolean silicateVolc =
//                safe(body.volcanismType).toLowerCase(Locale.ROOT).contains("silicate");
//
//        // Recepta: sulphur dioxide atmospheres with volcanism,
//        //        icy/rocky ice or rocky depending on specific species.
//        if (sulphurDioxide && body.hasVolcanism && hasTemp && t >= 160 && t <= 210) {
//            if (icyLike) {
//                addSingleSpecies(result,
//                        "Recepta Umbrux",
//                        0.9,
//                        "SO2 atmosphere + volcanism on icy/rocky ice – classic Recepta Umbrux conditions");
//                addSingleSpecies(result,
//                        "Recepta Conditivus",
//                        0.7,
//                        "SO2 atmosphere + volcanism on icy/rocky ice – possible Recepta Conditivus");
//            }
//            if (rockyLike) {
//                addSingleSpecies(result,
//                        "Recepta Deltahedronix",
//                        0.9,
//                        "SO2 atmosphere + volcanism on rocky body – classic Recepta Deltahedronix conditions");
//                addSingleSpecies(result,
//                        "Recepta Umbrux",
//                        0.75,
//                        "SO2 atmosphere + volcanism on rocky body – possible Recepta Umbrux");
//            }
//        }
//
//        // Fumerola: any atmosphere but requires volcanism; species depend on
//        // the volcanism chemistry.
//        if (body.hasVolcanism) {
//            String volc = safe(body.volcanismType).toLowerCase(Locale.ROOT);
//            if (volc.contains("water")) {
//                addSingleSpecies(result,
//                        "Fumerola Aquatis",
//                        0.5,
//                        "Water volcanism – classic Fumerola Aquatis");
//            } else if (volc.contains("carbon") || volc.contains("methane")) {
//                addSingleSpecies(result,
//                        "Fumerola Carbosis",
//                        0.5,
//                        "Carbon/methane volcanism – Fumerola Carbosis conditions");
//            } else if (volc.contains("nitrogen") || volc.contains("ammonia")) {
//                addSingleSpecies(result,
//                        "Fumerola Nitris",
//                        0.5,
//                        "Nitrogen/ammonia volcanism – Fumerola Nitris");
//            } else if (volc.contains("silicate") || volc.contains("iron") || volc.contains("rocky")) {
//                addSingleSpecies(result,
//                        "Fumerola Extremus",
//                        0.7,
//                        "Silicate/iron/rocky volcanism – Fumerola Extremus habitat");
//            }
//        }
//
//        // Bark Mounds: CO2 atmosphere, fairly broad cool-ish temps, any solid surface
//        if (co2Like && hasTemp && t >= 150 && t <= 450) {
//            addSingleSpecies(result,
//                    "Bark Mounds",
//                    0.5,
//                    "CO2 atmosphere with temperature suitable for Bark Mounds");
//        }
//
//        // Sinuous Tubers: thick temperate atmospheres on rocky/HMC worlds
//        if (rockyLike && !atmo.isEmpty() && !atmo.equals("none")
//                && hasTemp && t >= 200 && t <= 500) {
//            addSingleSpecies(result,
//                    "Roseum Sinuous Tubers",
//                    0.65,
//                    "Thick temperate atmosphere – generic Sinuous Tubers habitat");
//            addSingleSpecies(result,
//                    "Prasinum Sinuous Tubers",
//                    0.65,
//                    "Thick temperate atmosphere – generic Sinuous Tubers habitat");
//        }
//
//        // Cactoida: cool CO2 rocky/HMC worlds (180–195 K) with some atmosphere
//        if (rockyLike && co2Like && hasTemp && t >= 180 && t <= 195 && g > 0 && g < 0.6) {
//            addAllSpeciesForGenus(result, "Cactoida",
//                    0.85,
//                    "Cool CO2 rocky/HMC world in Cactoida band");
//        }
//
//        // Aleoida: similar band to mid-range Cactoida, prefers CO2/water atmos
//        if (rockyLike && (co2Like || waterLike) && hasTemp && t >= 170 && t <= 210
//                && g > 0 && g < 0.8) {
//            addAllSpeciesForGenus(result, "Aleoida",
//                    0.7,
//                    "Cool rocky world with CO2/water matching Aleoida conditions");
//        }
//
//        // Tubus: low-g worlds (rocky or icy) with CO2 or SO2 atmospheres
//        if ((rockyLike || icyLike) && (co2Like || sulphurDioxide) && g > 0 && g < 0.25) {
//            addAllSpeciesForGenus(result, "Tubus",
//                    0.8,
//                    "Low-gravity CO2/SO2 world typical for Tubus");
//        }
//
//        // Tussock: very common on temperate rocky/icy bodies.
//        // On ammonia worlds, only Catena / Cultro / Divisa are expected.
//        if ((rockyLike || icyLike) && ammoniaLike && hasTemp && t > 120 && t < 280) {
//            addSingleSpecies(result, "Tussock Catena",
//                    0.8,
//                    "Ammonia world – Tussock Catena/Cultro/Divisa band");
//            addSingleSpecies(result, "Tussock Cultro",
//                    0.8,
//                    "Ammonia world – Tussock Catena/Cultro/Divisa band");
//            addSingleSpecies(result, "Tussock Divisa",
//                    0.8,
//                    "Ammonia world – Tussock Catena/Cultro/Divisa band");
//        } else if ((rockyLike || icyLike) && !atmo.isEmpty() && !atmo.equals("none")
//                && hasTemp && t > 120 && t < 280) {
//            addAllSpeciesForGenus(result, "Tussock",
//                    0.7,
//                    "Generic temperate rocky/icy body – typical Tussock habitat");
//        }
//
//        // Concha: water / water-rich atmospheres on rocky or icy worlds in 170–200 K range
//        if ((rockyLike || icyLike) && waterLike && hasTemp && t >= 165 && t <= 205) {
//            addAllSpeciesForGenus(result, "Concha",
//                    0.8,
//                    "Cool water-bearing world, good for Concha");
//        }
//
//        // Fungoida: usually on cold rocky/icy worlds with water/ammonia and often volcanism
//        if ((rockyLike || icyLike) && (waterLike || ammoniaLike)
//                && hasTemp && t >= 150 && t <= 210) {
//            double score = (waterVolc || silicateVolc) ? 0.85 : 0.65;
//            String reason = "Cold water/ammonia world"
//                    + ((waterVolc || silicateVolc) ? " with volcanism" : "");
//            addAllSpeciesForGenus(result, "Fungoida", score, reason);
//        }
//
//        // Fonticulua: usually on icy / rocky-ice worlds with noble-gas or light-gas atmospheres
//        if (icyLike && (argonLike || neonLike || methaneLike || oxygenLike || nitrogenLike)) {
//            addAllSpeciesForGenus(result, "Fonticulua",
//                    0.75,
//                    "Icy world with noble-gas / light-gas atmosphere – typical Fonticulua conditions");
//        }
//
//        // Clypeus: high-value flora on CO2/water worlds at >190 K
//        if (rockyLike && (co2Like || waterLike) && hasTemp && t > 190 && g >= 0.3 && g <= 1.3) {
//            addAllSpeciesForGenus(result, "Clypeus",
//                    0.8,
//                    "Warm CO2/water world in Clypeus band");
//        }
//
//        // Osseus: wide range, but favour CO2/water/noble atmospheres with modest temps
//        if ((rockyLike || icyLike) && (co2Like || waterLike || nobleGasLike)
//                && hasTemp && t > 160 && t < 230) {
//            addAllSpeciesForGenus(result, "Osseus",
//                    0.6,
//                    "Moderate temperature world with CO2/water/noble gases – Osseus possible");
//        }
//
//        // Stratum: patchy, usually on CO2 or ammonia atmospheres above ~165 K
//        if ((co2Like || ammoniaLike) && hasTemp && t > 165 && t < 260) {
//            addAllSpeciesForGenus(result, "Stratum",
//                    0.7,
//                    "CO2/ammonia atmosphere with temperatures in Stratum band");
//        }
//
//        // Frutexa: dense atmospheres on rocky/HMC worlds
//        if (rockyLike && (co2Like || waterLike || ammoniaLike) && g >= 0.3 && g <= 1.3) {
//            addAllSpeciesForGenus(result, "Frutexa",
//                    0.75,
//                    "Rocky/HMC world with dense atmosphere and moderate gravity");
//        }
//
//        // Bacterium: per-species rules based on DSN/Canonn occurrence tables.
//        // We avoid dumping every Bacterium everywhere and instead key off atmosphere
//        // and volcanism to get much closer to Exploration Buddy's behaviour.
//        if (ammoniaLike) {
//            // Ammonia atmospheres strongly favour Bacterium alcyoneum.
//            addSingleSpecies(result, "Bacterium Alcyoneum",
//                    0.6,
//                    "Ammonia atmosphere – typical for Bacterium Alcyoneum");
//
//            // Bacterium Tela is seen with various volcanic worlds, including ammonia.
//            if (body.hasVolcanism) {
//                addSingleSpecies(result, "Bacterium Tela",
//                        0.7,
//                        "Volcanic world with ammonia-compatible chemistry – often Bacterium Tela");
//            }
//        } else {
//            // Argon/nitrogen atmospheres commonly host Bacterium Vesicula.
//            if (argonLike || nitrogenLike) {
//                addSingleSpecies(result, "Bacterium Vesicula",
//                        0.5,
//                        "Argon/nitrogen atmosphere – typical for Bacterium Vesicula");
//            }
//            // CO2 / water / sulphur-dioxide worlds sometimes host Bacterium Aurasus.
//            if (co2Like || waterLike || sulphurDioxide) {
//                addSingleSpecies(result, "Bacterium Aurasus",
//                        0.4,
//                        "CO2 / water / SO2 atmosphere – matches Bacterium Aurasus conditions");
//            }
//            // Bacterium Tela shows up broadly on volcanic worlds of many compositions.
//            if (body.hasVolcanism && (co2Like || sulphurDioxide || waterLike
//                    || ammoniaLike || argonLike || neonLike || methaneLike
//                    || nitrogenLike || oxygenLike)) {
//                addSingleSpecies(result, "Bacterium Tela",
//                        0.6,
//                        "Volcanic world – conditions where Bacterium Tela is often found");
//            }
//        }
//
//        // Electricae: icy, low-g worlds with noble-gas atmospheres (Ar/Ne)
//        if (icyLike && (neonLike || argonLike) && g > 0 && g < 0.3) {
//            addAllSpeciesForGenus(result, "Electricae",
//                    0.5,
//                    "Icy low-g world with noble-gas atmosphere");
//        }
//
//        return result;
//    }
//
//    private static String safe(String s) {
//        return s == null ? "" : s;
//    }
//
//    private static void addAllSpeciesForGenus(List<BioCandidate> out,
//                                              String genus,
//                                              double score,
//                                              String reason) {
//
//        String prefix = genus + " ";
//        for (Map.Entry<String, Long> e : SPECIES_BASE_VALUES.entrySet()) {
//            String name = e.getKey();
//            if (name.startsWith(prefix)) {
//                String species = name.substring(prefix.length());
//                out.add(new BioCandidate(genus, species, e.getValue(), score, reason));
//            }
//        }
//    }
//
//    /**
//     * Add a single species by full name ("Genus Species").
//     */
//    private static void addSingleSpecies(List<BioCandidate> out,
//                                         String fullName,
//                                         double score,
//                                         String reason) {
//        Long value = SPECIES_BASE_VALUES.get(fullName);
//        long v = value != null ? value : 0L;
//
//        String genus;
//        String species;
//
//        int idx = fullName.indexOf(' ');
//        if (idx > 0) {
//            genus = fullName.substring(0, idx);
//            species = fullName.substring(idx + 1);
//        } else {
//            genus = fullName;
//            species = "";
//        }
//
//        out.add(new BioCandidate(genus, species, v, score, reason));
//    }
//}
package org.dce.ed.exobiology;


