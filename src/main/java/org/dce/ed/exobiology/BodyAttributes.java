package org.dce.ed.exobiology;

import java.util.Collections;
import java.util.Map;

import org.dce.ed.exobiology.ExobiologyData.AtmosphereType;
import org.dce.ed.exobiology.ExobiologyData.PlanetType;

/**
 * Attributes of a body required for prediction.
 * This should match what your overlay already builds.
 */
public final class BodyAttributes {

    /** Optional body name (for rulesets that restrict to certain bodies). */
    public final String bodyName;
    public final String starSystem;
    public final double starPos[];
    public final PlanetType planetType;
    public final double gravity;
    public final AtmosphereType atmosphere;

    /**
     * Temperature in Kelvin. BioScan uses a single value; we keep min/max to avoid churn,
     * but in most cases tempKMin == tempKMax.
     */
    public final double tempKMin;
    public final double tempKMax;

    /** Surface pressure (atm). Use NaN if unknown. */
    public Double pressure;

    /** True if volcanism is present. */
    public final boolean hasVolcanism;

    /** Raw volcanism string (e.g. "Water geysers"). Null if none/unknown. */
    public final String volcanismType;

    /**
     * Optional atmosphere composition, keyed by gas name as used by the BioScan ruleset
     * (e.g. "CO2" -> 0.85). Use empty map if unknown.
     */
    public final Map<String, Double> atmosphereComponents;

    /** Optional orbital period (same units as ruleset). Null if unknown. */
    public final Double orbitalPeriod;

    /** Optional distance (same units as ruleset). Null if unknown. */
    public final Double distance;

    /** Optional guardian flag. Null if unknown. */
    public final Boolean guardian;

    /** Optional nebula string. Null if unknown. */
    public final String nebula;

    /** Optional parent star class/name. Null if unknown. */
    public final String parentStar;

    /** Optional region name. Null if unknown. */
    public final String region;

    /** Optional star class for system. Null if unknown. */
    public final String starClass;

    public BodyAttributes(String bodyName,
    		              String starSystem,
    		              double starPos[],
                          PlanetType planetType,
                          double gravity,
                          AtmosphereType atmosphere,
                          double tempKMin,
                          double tempKMax,
                          Double pressure,
                          boolean hasVolcanism,
                          String volcanismType,
                          Map<String, Double> atmosphereComponents,
                          Double orbitalPeriod,
                          Double distance,
                          Boolean guardian,
                          String nebula,
                          String parentStar,
                          String region,
                          String starClass) {

        this.bodyName = bodyName;
        this.starSystem = starSystem;
        this.starPos = starPos;
        this.planetType = planetType;
        this.gravity = gravity;
        this.atmosphere = atmosphere;
        this.tempKMin = tempKMin;
        this.tempKMax = tempKMax;
        this.pressure = pressure;
        this.hasVolcanism = hasVolcanism;
        this.volcanismType = volcanismType;
        this.atmosphereComponents = atmosphereComponents != null ? atmosphereComponents : Collections.emptyMap();
        this.orbitalPeriod = orbitalPeriod;
        this.distance = distance;
        this.guardian = guardian;
        this.nebula = nebula;
        this.parentStar = parentStar;
        this.region = region;
        this.starClass = starClass;
    }

    /** Back-compat: pressure included. */
    public BodyAttributes(
    		String bodyName,
    		String starSystem,
    		double starPos[],
    		PlanetType planetType,
                          double gravity,
                          AtmosphereType atmosphere,
                          double tempKMin,
                          double tempKMax,
                          Double pressure,
                          boolean hasVolcanism,
                          String volcanismType) {

        this(bodyName,
        		starSystem,
        		starPos,
             planetType,
             gravity,
             atmosphere,
             tempKMin,
             tempKMax,
             pressure,
             hasVolcanism,
             volcanismType,
             Collections.emptyMap(),
             null,
             null,
             null,
             null,
             null,
             null,
             null);
    }
}