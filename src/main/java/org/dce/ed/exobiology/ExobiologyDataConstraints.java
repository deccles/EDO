package org.dce.ed.exobiology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.dce.ed.exobiology.ExobiologyData.AtmosphereType;
import org.dce.ed.exobiology.ExobiologyData.PlanetType;

public final class ExobiologyDataConstraints {

    private ExobiologyDataConstraints() {
    }

    /**
     * Generated from rulesets/*.py by generate_exobio_java_constraints.py
     */
    public static void initConstraints(Map<String, ExobiologyData.SpeciesConstraint> CONSTRAINTS) {
        ExobiologyData.SpeciesConstraint sc;

        sc = new ExobiologyData.SpeciesConstraint("Albidum", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), 86400.0, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Inner S-C Arm B 2", "Inner S-C Arm D", "Trojan Belt"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Aleoida", "Arcus", 7252500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 175.0, 180.0, 0.0161, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Aleoida", "Coronamus", 6284600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 180.0, 190.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Aleoida", "Gravis", 12934900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 190.0, 197.0, 0.054, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Aleoida", "Laminiae", 3385200, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus", "sagittarius-carina"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Aleoida", "Spica", 3385200, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 170.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("outer", "perseus", "scutum-centaurus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Aureum", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 2.9, 300.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Acies", 1000000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.255, 0.61, 20.0, 61.0, 0.0, 0.01, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Alcyoneum", 1658500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.376, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Aurasus", 1000000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.039, 0.608, 145.0, 400.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Bullaris", 1152500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0245, 0.35, 67.0, 109.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.44, 0.6, 74.0, 141.0, 0.01, 0.05, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Cerbrus", 1689800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.042, 0.605, 132.0, 500.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.04, 0.064, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.064, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.4, 0.5, 240.0, 320.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Informem", 8418000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.05, 0.6, 42.5, 151.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.17, 0.63, 50.0, 90.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Nebulus", 5289900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.4, 0.55, 20.0, 21.0, 0.067, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.4, 0.7, 20.0, 21.0, 0.067, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Omentum", 4638900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.045, 0.45, 50.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.23, 0.45, 80.0, 90.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.4, 0.51, 20.0, 21.0, 0.065, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.0265, 0.0455, 84.0, 108.0, 0.035, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.31, 0.6, 20.0, 61.0, 0.0, 0.0065, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.27, 0.61, 20.0, 93.0, 0.0027, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.2, 0.26, 60.0, 80.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.38, 0.45, 190.0, 320.0, 0.07, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Scopulum", 4934500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.15, 0.26, 56.0, 150.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.48, 0.51, 20.0, 21.0, 0.075, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.047, 84.0, 110.0, 0.03, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.61, 20.0, 65.0, 0.0, 0.008, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.61, 20.0, 65.0, 0.005, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.2, 0.3, 60.0, 70.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.27, 0.4, 150.0, 220.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Tela", 1949000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.045, 0.45, 50.0, 200.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.24, 0.45, 50.0, 150.0, 0.0, 0.05, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.23, 165.0, 177.0, 0.0025, 0.02, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.45, 0.61, 300.0, 500.0, 0.006, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.26, 0.57, 167.0, 300.0, 0.006, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2, AtmosphereType.CO2)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.61, 20.0, 21.0, 0.067, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.026, 0.126, 80.0, 109.0, 0.012, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.27, 0.61, 20.0, 95.0, 0.0, 0.008, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.27, 0.61, 20.0, 95.0, 0.003, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.21, 0.35, 55.0, 80.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.23, 0.5, 150.0, 240.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.18, 0.61, 148.0, 550.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.18, 0.61, 300.0, 550.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.5, 0.55, 500.0, 650.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.063, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.315, 0.44, 220.0, 330.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Verrata", 3897000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.03, 0.09, 160.0, 180.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.165, 0.33, 57.5, 145.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.08, 80.0, 90.0, 0.0, 0.01, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.25, 0.32, 167.0, 240.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2, AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.49, 0.53, 20.0, 21.0, 0.065, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.HELIUM)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.29, 0.61, 20.0, 51.0, 0.0, 0.075, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.43, 0.61, 20.0, 65.0, 0.005, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.205, 0.241, 60.0, 80.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.24, 0.35, 154.0, 220.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE, PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.054, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Vesicula", 1000000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.027, 0.51, 50.0, 245.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Bacterium", "Volu", 7774700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.239, 0.61, 143.5, 246.0, 0.013, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Blatteum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 220.0, 1000000.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("anemone-a"), Arrays.asList("B IV", "B V"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Blatteum", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Arcadian Stream", "Inner Orion Spur", "Inner S-C Arm B 2", "Hawking A"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Cactoida", "Cortexum", 3667600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 180.0, 197.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Cactoida", "Lapis", 2483600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 160.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Cactoida", "Peperatis", 2483600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 160.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("scutum-centaurus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Cactoida", "Pullulanta", 3667600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 180.0, 197.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("perseus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Cactoida", "Vermis", 16202800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.265, 0.276, 160.0, 210.0, 0.0, 0.005, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Caeruleum", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), 86400.0, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Galactic Center", "Inner S-C Arm D", "Norma Arm A"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("empyrean-straits"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Clypeus", "Lacrimam", 8418000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 190.0, 197.0, 0.054, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Clypeus", "Margaritus", 11873200, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 190.0, 197.0, 0.054, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Clypeus", "Speculumi", 16202800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 190.0, 197.0, 0.055, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, 2000.0, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, 2000.0, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, 2000.0, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Concha", "Aureolas", 7774700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Concha", "Biconcavis", 16777215, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.053, 0.275, 42.0, 52.0, 0.0, 0.0047, 
            		new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), 
            		Collections.<String>emptyList(), 
            		Collections.<String>emptyList(), 
            		Collections.<String>emptyList(), 
            		ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Concha", "Labiata", 2352400, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 150.0, 200.0, 0.002, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Concha", "Renibus", 4572400, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.045, 176.0, 177.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 180.0, 197.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.15, 78.0, 100.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.65, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.65, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Croceum", "Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.047, 0.37, 200.0, 440.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("anemone-a"), Arrays.asList("B V", "B VI", "A III"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Crystalline", "Shards", 1628800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 2.0, 0.0, 273.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NONE, AtmosphereType.ARGON, AtmosphereType.ARGON, AtmosphereType.CO2, AtmosphereType.CO2, AtmosphereType.HELIUM, AtmosphereType.METHANE, AtmosphereType.NEON, AtmosphereType.NEON)), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Arrays.asList("Earthlike body", "Ammonia world", "Water world", "Gas giant with water based life", "Gas giant with ammonia based life", "Water giant"), null, 12000.0, null, null, Collections.<String>emptyList(), Arrays.asList("exterior"), Arrays.asList("A", "F", "G", "K", "MS", "S"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Electricae", "Pluma", 6284600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.025, 0.276, 50.0, 150.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Arrays.asList("A", "N", "D", "H", "AeBe"), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.26, 0.276, 20.0, 70.0, 0.0, 0.005, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON, AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Arrays.asList("A", "N", "D", "H", "AeBe"), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Electricae", "Radialem", 6284600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.025, 0.276, 50.0, 150.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, "all", Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.026, 0.276, 20.0, 70.0, 0.0, 0.005, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON, AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, "all", Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fonticulua", "Campestris", 1000000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.027, 0.276, 50.0, 150.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fonticulua", "Digitos", 1804100, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.025, 0.07, 83.0, 109.0, 0.03, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fonticulua", "Fluctus", 20000000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.235, 0.276, 143.0, 200.0, 0.012, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fonticulua", "Lapida", 3111000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.19, 0.276, 50.0, 81.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fonticulua", "Segmentatus", 19010800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.25, 0.276, 50.0, 75.0, 0.0, 0.006, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON, AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fonticulua", "Upupam", 5727600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.209, 0.276, 61.0, 125.0, 0.0175, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Frutexa", "Acus", 7774700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.237, 146.0, 197.0, 0.0029, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Frutexa", "Collum", 1639800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 132.0, 215.0, 0.0, 0.004, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.265, 0.276, 132.0, 135.0, 0.0, 0.004, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Frutexa", "Fera", 1632500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 146.0, 197.0, 0.003, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("outer"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Frutexa", "Flabellum", 1808900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!scutum-centaurus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Frutexa", "Flammasis", 10326000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("scutum-centaurus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Frutexa", "Metallicum", 1632500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 176.0, 0.0, 0.01, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 146.0, 197.0, 0.002, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.05, 0.1, 100.0, 300.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.04, 0.07, 0.0, 400.0, 0.0, 0.07, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Frutexa", "Sponsae", 5988000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.056, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.056, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fumerola", "Aquatis", 6284600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.028, 0.276, 161.0, 177.0, 0.002, 0.02, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE, PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.166, 0.276, 57.0, 150.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.25, 0.276, 160.0, 180.0, 0.01, 0.03, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 80.0, 100.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.26, 0.276, 20.0, 60.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.195, 0.245, 56.0, 80.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.23, 0.276, 153.0, 190.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.18, 0.276, 150.0, 270.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE, PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.06, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fumerola", "Carbosis", 6284600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.168, 0.276, 57.0, 150.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.047, 84.0, 110.0, 0.03, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.26, 0.276, 40.0, 60.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.2, 0.276, 57.0, 70.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.26, 0.276, 160.0, 180.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.185, 0.276, 149.0, 272.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.0, 0.276, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA, AtmosphereType.ARGON, AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fumerola", "Extremus", 16202800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.09, 161.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.07, 0.276, 50.0, 121.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.127, 77.0, 109.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.07, 0.276, 54.0, 210.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fumerola", "Nitris", 7500900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 30.0, 129.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.044, 0.276, 50.0, 141.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON, AtmosphereType.NEON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.025, 0.1, 83.0, 109.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.21, 0.276, 60.0, 81.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.0, 0.276, 150.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.21, 0.276, 160.0, 250.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fungoida", "Bullarum", 3703200, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.058, 0.276, 50.0, 129.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.155, 0.276, 50.0, 70.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fungoida", "Gelata", 3330300, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.041, 0.276, 160.0, 180.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.042, 0.071, 160.0, 180.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.042, 0.071, 160.0, 180.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.041, 0.276, 180.0, 200.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.044, 0.125, 80.0, 110.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.039, 0.063, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fungoida", "Setisis", 1670100, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.033, 0.276, 68.0, 109.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.033, 0.276, 67.0, 109.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Fungoida", "Stabitis", 2680300, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.045, 172.0, 177.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.2, 0.23, 60.0, 90.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.3, 0.5, 60.0, 90.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.0405, 0.27, 180.0, 197.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.043, 0.126, 78.5, 109.0, 0.012, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.039, 0.064, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Gypseeum", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 0.42, 200.0, 400.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Arrays.asList("Earthlike body", "Gas giant with water based life", "Water giant"), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Lindigoticum", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 2.7, 300.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Arrays.asList("Earthlike body", "Gas giant with water based life", "Water giant"), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Lindigoticum", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), 86400.0, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Inner S-C Arm A", "Inner S-C Arm C", "Hawking B", "Norma Expanse A", "Odin B"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Lividum", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 0.5, 300.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Luteolum", "Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.044, 1.28, 200.0, 440.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("anemone-a"), Arrays.asList("B IV", "B V"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Osseus", "Cornibus", 1483000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0405, 0.276, 180.0, 197.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("perseus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Osseus", "Discus", 12934900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.088, 161.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.2, 0.276, 65.0, 120.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.127, 80.0, 110.0, 0.012, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.055, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Osseus", "Fractus", 4027800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 180.0, 190.0, 0.025, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!perseus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Osseus", "Pellebantus", 9739000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0405, 0.276, 191.0, 197.0, 0.057, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("!perseus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Osseus", "Pumice", 3156300, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.059, 0.276, 50.0, 135.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.059, 0.276, 50.0, 135.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.035, 0.276, 60.0, 80.5, 0.03, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.033, 0.276, 67.0, 109.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.05, 0.276, 42.0, 70.1, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.NITROGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Osseus", "Spiralis", 2404700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 160.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Ostrinum", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 0.0, 1000000.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Prasinum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.036, 100.0, 110.0, 3050.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("O"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Prasinum", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL, PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Inner S-C Arm B 1"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Inner S-C Arm D", "Norma Expanse B", "Odin B"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("empyrean-straits"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Puniceum", "Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.17, 2.52, 65.0, 800.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("anemone-a"), Arrays.asList("O"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.17, 2.52, 65.0, 800.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("anemone-a"), Arrays.asList("O"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Puniceum", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 0.0, 1000000.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Arrays.asList("Earthlike body", "Gas giant with water based life", "Water giant"), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Recepta", "Conditivus", 14313700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 150.0, 195.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2, AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.23, 0.276, 154.0, 175.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.23, 0.276, 154.0, 175.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 132.0, 275.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.<PlanetType>emptySet(), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Recepta", "Deltahedronix", 16202800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 150.0, 195.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), Collections.<PlanetType>emptySet(), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 150.0, 195.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY, PlanetType.ROCKY_ICE)), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 132.0, 272.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.<PlanetType>emptySet(), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Recepta", "Umbrux", 12934900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 151.0, 200.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), Collections.<PlanetType>emptySet(), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.23, 0.276, 154.0, 175.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.23, 0.276, 154.0, 175.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ICY)), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.04, 0.276, 132.0, 273.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), Collections.<PlanetType>emptySet(), Collections.singletonMap("SulphurDioxide", 1.05), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Roseum", "Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.045, 0.37, 200.0, 440.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("anemone-a"), Arrays.asList("B I", "B II", "B III", "B IV"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Roseum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.036, 4.61, 400.0, 1000000.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("B I", "B II", "B III"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Roseum", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), Collections.<PlanetType>emptySet(), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Roseum", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Galactic Center", "Odin A", "Ryker B"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Rubeum", "Bioluminescent Anemone", 1499900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.036, 4.61, 160.0, 1800.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("B VI", "A I", "A II", "A III", "N"), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Aranaemus", 2448900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(

        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Araneamus", 2448900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.26, 0.57, 165.0, 373.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Cucumisis", 16202800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.6, 191.0, 371.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.44, 0.56, 210.0, 246.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.4, 0.6, 200.0, 250.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.26, 0.55, 191.0, 373.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Excutitus", 2448900, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.48, 165.0, 190.0, 0.0035, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.27, 0.4, 165.0, 190.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Frigus", 2637500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.043, 0.54, 191.0, 365.0, 0.001, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("perseus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.45, 0.56, 200.0, 250.0, 0.01, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("perseus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.29, 0.52, 191.0, 369.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("perseus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Laminamus", 2788300, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.34, 165.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Limaxus", 1362000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.03, 0.4, 165.0, 190.0, 0.05, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("scutum-centaurus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.27, 0.4, 165.0, 190.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("scutum-centaurus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Paleas", 1362000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.35, 165.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.04, 0.585, 165.0, 395.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.43, 0.585, 185.0, 260.0, 0.015, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.056, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.056, 0.0, 1000000.0, 0.065, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.39, 0.59, 165.0, 250.0, 0.022, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Stratum", "Tectonicas", 19010800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.045, 0.38, 165.0, 177.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.485, 0.54, 167.0, 199.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON, AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.045, 0.61, 165.0, 430.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.035, 0.61, 165.0, 260.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.4, 0.52, 165.0, 246.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.OXYGEN)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.29, 0.62, 165.0, 450.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.045, 0.063, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tubus", "Cavas", 11873200, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.152, 160.0, 197.0, 0.003, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("scutum-centaurus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tubus", "Compagibus", 7774700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.153, 160.0, 197.0, 0.003, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tubus", "Conifer", 2415500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.041, 0.153, 160.0, 197.0, 0.003, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("perseus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tubus", "Rosarium", 2637500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.153, 160.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tubus", "Sororibus", 5727600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.045, 0.152, 160.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.045, 0.152, 160.0, 195.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Albata", 3252500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.042, 0.276, 175.0, 180.0, 0.016, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina-core-9", "perseus-core", "orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Capillum", 7025800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.22, 0.276, 80.0, 129.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.ARGON)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY),
            new ExobiologyData.SpeciesRule(0.033, 0.276, 80.0, 110.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.METHANE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Caputus", 3472400, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.041, 0.27, 181.0, 190.0, 0.0275, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina-core-9", "perseus-core", "orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Catena", 1766600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("scutum-centaurus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Cultro", 1766600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("orion-cygnus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Divisa", 1766600, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.042, 0.276, 152.0, 177.0, 0.0, 0.0135, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.AMMONIA)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("perseus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Ignis", 1849000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.2, 161.0, 170.0, 0.00289, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina-core-9", "perseus-core", "orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Pennata", 5853800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.09, 146.0, 154.0, 0.00289, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina-core-9", "perseus-core", "orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Pennatis", 1000000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 147.0, 197.0, 0.00289, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("outer"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Propagito", 1000000, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 145.0, 197.0, 0.00289, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("scutum-centaurus"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Serrati", 4447100, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.042, 0.23, 171.0, 174.0, 0.01, 0.071, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina-core-9", "perseus-core", "orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Stigmasis", 19010800, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 132.0, 180.0, 0.0, 0.01, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.SULPHUR_DIOXIDE)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.ANY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Triticum", 7774700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.276, 191.0, 197.0, 0.058, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina-core-9", "perseus-core", "orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Ventusa", 3227700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.13, 155.0, 160.0, 0.00289, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.CO2)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Arrays.asList("sagittarius-carina-core-9", "perseus-core", "orion-cygnus-core"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Tussock", "Virgam", 14313700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.04, 0.065, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.NO_VOLCANISM),
            new ExobiologyData.SpeciesRule(0.04, 0.065, 0.0, 1000000.0, 0.0, 1000000.0, new HashSet<AtmosphereType>(Arrays.asList(AtmosphereType.WATER)), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Violaceum", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.METAL_RICH, PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Arcadian Stream", "Empyrean Straits", "Norma Arm B"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Viride", "Brain Tree", 1593700, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 0.4, 100.0, 270.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY_ICE)), Collections.<String, Double>emptyMap(), Arrays.asList("Earthlike body", "Gas giant with water based life", "Water giant"), null, null, Boolean.TRUE, null, Collections.<String>emptyList(), Arrays.asList("['brain-tree']"), Collections.<String>emptyList(), Collections.<String>emptyList(), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

        sc = new ExobiologyData.SpeciesConstraint("Viride", "Sinuous Tubers", 1514500, new ArrayList<>());
        sc.getRules().addAll(Arrays.asList(
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.HIGH_METAL)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), null, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Inner O-P Conflux", "Izanami", "Ryker A"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY),
            new ExobiologyData.SpeciesRule(0.0, 100.0, 200.0, 500.0, 0.0, 1000000.0, Collections.<AtmosphereType>emptySet(), new HashSet<PlanetType>(Arrays.asList(PlanetType.ROCKY)), Collections.<String, Double>emptyMap(), Collections.<String>emptyList(), 86400.0, null, null, null, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(), Arrays.asList("Inner O-P Conflux", "Izanami", "Ryker A"), ExobiologyData.VolcanismRequirement.VOLCANIC_ONLY)
        ));
        CONSTRAINTS.put(sc.key(), sc);

    }
}
