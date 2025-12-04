//    private static void initConstraints() {
//        SpeciesConstraint sc;
//        sc = new SpeciesConstraint("Albidum", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Aleoida", "Arcus", 7252500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 175.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Aleoida", "Coronamus", 6284600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 180.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Aleoida", "Gravis", 12934900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 190.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Aleoida", "Laminiae", 3385200, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Aleoida", "Spica", 3385200, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 170.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Aureum", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 2.9, 300.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Acies", 1000000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.255, 0.61, 20.0, 61.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Alcyoneum", 1658500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.376, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Aurasus", 1000000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.039, 0.608, 145.0, 400.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL, PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Bullaris", 1152500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0245, 0.35, 67.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), Collections.emptySet(), VolcanismRequirement.ANY),
//            new SpeciesRule(0.44, 0.6, 74.0, 141.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Cerbrus", 1689800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.042, 0.605, 132.0, 500.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.04, 0.064, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.064, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.4, 0.5, 240.0, 320.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Informem", 8418000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.05, 0.6, 42.5, 151.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.17, 0.63, 50.0, 90.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Nebulus", 5289900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.4, 0.55, 20.0, 21.0, new HashSet<>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.4, 0.7, 20.0, 21.0, new HashSet<>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Omentum", 4638900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.045, 0.45, 50.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.23, 0.45, 80.0, 90.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.4, 0.51, 20.0, 21.0, new HashSet<>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.0265, 0.0455, 84.0, 108.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.31, 0.6, 20.0, 61.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.27, 0.61, 20.0, 93.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.2, 0.26, 60.0, 80.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.38, 0.45, 190.0, 320.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Scopulum", 4934500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.15, 0.26, 56, 150, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.48, 0.51, 20, 21, new HashSet<>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.047, 84, 110, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.61, 20, 65, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.61, 20, 65, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.2, 0.3, 60, 70, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.27, 0.4, 150, 220, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Tela", 1949000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.045, 0.45, 50.0, 200.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.24, 0.45, 50.0, 150.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), Collections.emptySet(), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.23, 165.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), Collections.emptySet(), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.45, 0.61, 300.0, 500.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), Collections.emptySet(), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.26, 0.57, 167.0, 300.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2, AtmosphereType.CO2)), Collections.emptySet(), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.61, 20.0, 21.0, new HashSet<>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.026, 0.126, 80.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.27, 0.61, 20.0, 95.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.27, 0.61, 20.0, 95.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.21, 0.35, 55.0, 80.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), Collections.emptySet(), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.23, 0.5, 150.0, 240.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), Collections.emptySet(), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.18, 0.61, 148.0, 550.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.emptySet(), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.18, 0.61, 300.0, 550.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.emptySet(), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.5, 0.55, 500.0, 650.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.063, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.315, 0.44, 220.0, 330.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Verrata", 3897000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.03, 0.09, 160.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.165, 0.33, 57.5, 145.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.08, 80.0, 90.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.25, 0.32, 167.0, 240.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2, AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.49, 0.53, 20.0, 21.0, new HashSet<>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.29, 0.61, 20.0, 51.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.43, 0.61, 20.0, 65.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.205, 0.241, 60.0, 80.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.24, 0.35, 154.0, 220.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.054, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Vesicula", 1000000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.027, 0.51, 50.0, 245.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), Collections.emptySet(), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Bacterium", "Volu", 7774700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.239, 0.61, 143.5, 246.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), Collections.emptySet(), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Blatteum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 220.0, 1000000.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Blatteum", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Cactoida", "Cortexum", 3667600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 180.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Cactoida", "Lapis", 2483600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 160.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Cactoida", "Peperatis", 2483600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 160.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Cactoida", "Pullulanta", 3667600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 180.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Cactoida", "Vermis", 16202800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.265, 0.276, 160.0, 210.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Caeruleum", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Clypeus", "Lacrimam", 8418000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 190.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Clypeus", "Margaritus", 11873200, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 190.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Clypeus", "Speculumi", 16202800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 190.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Concha", "Aureolas", 7774700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Concha", "Biconcavis", 16777215, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.053, 0.275, 42.0, 52.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Concha", "Labiata", 2352400, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 150.0, 200.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Concha", "Renibus", 4572400, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.045, 176.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.276, 180.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.15, 78.0, 100.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.65, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.65, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Croceum", "Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.047, 0.37, 200.0, 440.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Crystalline", "Shards", 1628800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 2.0, 0.0, 273.0, new HashSet<>(Arrays.asList(AtmosphereType.NONE, AtmosphereType.ARGON, AtmosphereType.ARGON, AtmosphereType.CO2, AtmosphereType.CO2, AtmosphereType.HELIUM, AtmosphereType.METHANE, AtmosphereType.NEON, AtmosphereType.NEON)), Collections.emptySet(), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Electricae", "Pluma", 6284600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.025, 0.276, 50.0, 150.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.26, 0.276, 20.0, 70.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON, AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Electricae", "Radialem", 6284600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.025, 0.276, 50.0, 150.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.026, 0.276, 20.0, 70.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON, AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fonticulua", "Campestris", 1000000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.027, 0.276, 50.0, 150.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fonticulua", "Digitos", 1804100, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.025, 0.07, 83.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fonticulua", "Fluctus", 20000000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.235, 0.276, 143.0, 200.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fonticulua", "Lapida", 3111000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.19, 0.276, 50.0, 81.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fonticulua", "Segmentatus", 19010800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.25, 0.276, 50.0, 75.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON, AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fonticulua", "Upupam", 5727600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.209, 0.276, 61.0, 125.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Frutexa", "Acus", 7774700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.237, 146.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Frutexa", "Collum", 1639800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 132.0, 215.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.265, 0.276, 132.0, 135.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Frutexa", "Fera", 1632500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 146.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Frutexa", "Flabellum", 1808900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Frutexa", "Flammasis", 10326000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Frutexa", "Metallicum", 1632500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 176.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 146.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.05, 0.1, 100.0, 300.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.04, 0.07, 0.0, 400.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Frutexa", "Sponsae", 5988000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.056, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.056, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fumerola", "Aquatis", 6284600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.028, 0.276, 161.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE, PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.166, 0.276, 57.0, 150.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.25, 0.276, 160.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.276, 80.0, 100.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.26, 0.276, 20.0, 60.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.195, 0.245, 56.0, 80.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.23, 0.276, 153.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.18, 0.276, 150.0, 270.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE, PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.06, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fumerola", "Carbosis", 6284600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.168, 0.276, 57.0, 150.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.047, 84.0, 110.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.26, 0.276, 40.0, 60.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.2, 0.276, 57.0, 70.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.26, 0.276, 160.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.185, 0.276, 149.0, 272.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.0, 0.276, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA, AtmosphereType.ARGON, AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fumerola", "Extremus", 16202800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.09, 161.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.07, 0.276, 50.0, 121.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.127, 77.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.07, 0.276, 54.0, 210.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fumerola", "Nitris", 7500900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 30.0, 129.0, new HashSet<>(Arrays.asList(AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.044, 0.276, 50.0, 141.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON, AtmosphereType.NEON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.025, 0.1, 83.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.21, 0.276, 60.0, 81.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.0, 0.276, 150.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.21, 0.276, 160.0, 250.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fungoida", "Bullarum", 3703200, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.058, 0.276, 50.0, 129.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.155, 0.276, 50.0, 70.0, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fungoida", "Gelata", 3330300, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.041, 0.276, 160.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.042, 0.071, 160.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.042, 0.071, 160.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.041, 0.276, 180.0, 200.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.044, 0.125, 80.0, 110.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.039, 0.063, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fungoida", "Setisis", 1670100, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.033, 0.276, 68.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.033, 0.276, 67.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Fungoida", "Stabitis", 2680300, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.045, 172.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.2, 0.23, 60.0, 90.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.3, 0.5, 60.0, 90.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.0405, 0.27, 180.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.043, 0.126, 78.5, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.039, 0.064, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Gypseeum", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 0.42, 200.0, 400.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Lindigoticum", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 2.7, 300.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Lindigoticum", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Lividum", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 0.5, 300.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Luteolum", "Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.044, 1.28, 200.0, 440.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Osseus", "Cornibus", 1483000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0405, 0.276, 180.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Osseus", "Discus", 12934900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.088, 161.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.2, 0.276, 65.0, 120.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.127, 80.0, 110.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.055, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Osseus", "Fractus", 4027800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 180.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Osseus", "Pellebantus", 9739000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0405, 0.276, 191.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Osseus", "Pumice", 3156300, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.059, 0.276, 50.0, 135.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.059, 0.276, 50.0, 135.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.035, 0.276, 60.0, 80.5, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.033, 0.276, 67.0, 109.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.05, 0.276, 42.0, 70.1, new HashSet<>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Osseus", "Spiralis", 2404700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 160.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Ostrinum", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 0.0, 1000000.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Prasinum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.036, 100.0, 110.0, 3050.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Prasinum", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL, PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Puniceum", "Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.17, 2.52, 65.0, 800.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.17, 2.52, 65.0, 800.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Puniceum", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 0.0, 1000000.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Recepta", "Conditivus", 14313700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 150.0, 195.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2, AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.23, 0.276, 154.0, 175.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.23, 0.276, 154.0, 175.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.276, 132.0, 275.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.emptySet(), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Recepta", "Deltahedronix", 16202800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 150.0, 195.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), Collections.emptySet(), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.276, 150.0, 195.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.276, 132.0, 272.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.emptySet(), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Recepta", "Umbrux", 12934900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 151.0, 200.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), Collections.emptySet(), VolcanismRequirement.ANY),
//            new SpeciesRule(0.23, 0.276, 154.0, 175.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.23, 0.276, 154.0, 175.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ICY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.04, 0.276, 132.0, 273.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.emptySet(), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Roseum", "Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.045, 0.37, 200.0, 440.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Roseum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.036, 4.61, 400.0, 1000000.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Roseum", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), Collections.emptySet(), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Roseum", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Rubeum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.036, 4.61, 160.0, 1800.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Aranaemus", 2448900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Araneamus", 2448900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.26, 0.57, 165.0, 373.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Cucumisis", 16202800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.6, 191.0, 371.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.44, 0.56, 210.0, 246.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.4, 0.6, 200.0, 250.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.26, 0.55, 191.0, 373.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Excutitus", 2448900, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.48, 165.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.27, 0.4, 165.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Frigus", 2637500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.043, 0.54, 191.0, 365.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.45, 0.56, 200.0, 250.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.29, 0.52, 191.0, 369.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Laminamus", 2788300, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.34, 165.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Limaxus", 1362000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.03, 0.4, 165.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.27, 0.4, 165.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Paleas", 1362000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.35, 165.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.04, 0.585, 165.0, 395.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.43, 0.585, 185.0, 260.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.056, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.056, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.39, 0.59, 165.0, 250.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Stratum", "Tectonicas", 19010800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.045, 0.38, 165.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.485, 0.54, 167.0, 199.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.045, 0.61, 165.0, 430.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.035, 0.61, 165.0, 260.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.4, 0.52, 165.0, 246.0, new HashSet<>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.29, 0.62, 165.0, 450.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.045, 0.063, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tubus", "Cavas", 11873200, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.152, 160.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tubus", "Compagibus", 7774700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.153, 160.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tubus", "Conifer", 2415500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.041, 0.153, 160.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tubus", "Rosarium", 2637500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.153, 160.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tubus", "Sororibus", 5727600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.045, 0.152, 160.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.045, 0.152, 160.0, 195.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Albata", 3252500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.042, 0.276, 175.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Capillum", 7025800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.22, 0.276, 80.0, 129.0, new HashSet<>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY),
//            new SpeciesRule(0.033, 0.276, 80.0, 110.0, new HashSet<>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Caputus", 3472400, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.041, 0.27, 181.0, 190.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Catena", 1766600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Cultro", 1766600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Divisa", 1766600, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.042, 0.276, 152.0, 177.0, new HashSet<>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Ignis", 1849000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.2, 161.0, 170.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Pennata", 5853800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.09, 146.0, 154.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Pennatis", 1000000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 147.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Propagito", 1000000, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 145.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Serrati", 4447100, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.042, 0.23, 171.0, 174.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Stigmasis", 19010800, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 132.0, 180.0, new HashSet<>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.ANY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Triticum", 7774700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.276, 191.0, 197.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Ventusa", 3227700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.13, 155.0, 160.0, new HashSet<>(Arrays.asList(AtmosphereType.CO2)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Tussock", "Virgam", 14313700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.04, 0.065, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.NO_VOLCANISM),
//            new SpeciesRule(0.04, 0.065, 0.0, 1000000.0, new HashSet<>(Arrays.asList(AtmosphereType.WATER)), new HashSet<>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Violaceum", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Viride", "Brain Tree", 1593700, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 0.4, 100.0, 270.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY_ICE)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//        sc = new SpeciesConstraint("Viride", "Sinuous Tubers", 1514500, new ArrayList<>());
//        sc.getRules().addAll(Arrays.asList(
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.HIGH_METAL)), VolcanismRequirement.VOLCANIC_ONLY),
//            new SpeciesRule(0.0, 100.0, 200.0, 500.0, Collections.emptySet(), new HashSet<>(Arrays.asList(PlanetType.ROCKY)), VolcanismRequirement.VOLCANIC_ONLY)
//        ));
//        CONSTRAINTS.put(sc.key(), sc);
//
//    }
