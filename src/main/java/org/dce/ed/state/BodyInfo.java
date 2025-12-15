package org.dce.ed.state;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.exobiology.ExobiologyData.AtmosphereType;
import org.dce.ed.exobiology.ExobiologyData.BodyAttributes;
import org.dce.ed.exobiology.ExobiologyData.PlanetType;

/**
 * Pure domain representation of a single stellar body.
 * 
 * NO Swing logic.
 * NO rendering logic.
 * 
 * This class holds:
 *  - Physical attributes
 *  - Biological flags
 *  - Observed genus list (from DSS / ScanOrganic)
 *  - Predicted biological candidates (computed in SystemState)
 */
public class BodyInfo {


    private String name;
    private String shortName;
    private int bodyId = -1;
	private String starSystem;
	double starPos[];
	
    public double[] getStarPos() {
		return starPos;
	}

	public void setStarPos(double[] starPos) {
		this.starPos = starPos;
	}

	private double distanceLs = Double.NaN;
    private Double gravityMS;     // in m/s^2
    private boolean landable;

    private boolean hasBio;
    private boolean hasGeo;
    private boolean highValue;

    private String atmoOrType;    // primarily for display
    private String planetClass;
    private String atmosphere;

    private Double surfaceTempK;
    private String volcanism;

    Double axialTilt;
    Double radius;
    // Derived biological prediction data
    private List<ExobiologyData.BioCandidate> predictions;

    private java.util.Set<String> observedBioDisplayNames;
    
    // Observed genera from DSS or ScanOrganic
    private Set<String> observedGenusPrefixes;
    
    private String discoveryCommander;  // may be null or empty
    
	private Double surfacePressure;
	private String parentStar;
	private String nebula;
    
    // ------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------

	public String getStarSystem() {
		return starSystem;
	}

	public void setParentStar(String parentStar) {
	    this.parentStar = parentStar;
	}

	public String getParentStar() {
	    return parentStar;
	}

	public void setNebula(String nebula) {
	    this.nebula = nebula;
	}

	public String getNebula() {
	    return nebula;
	}
	
    public String getBodyName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public int getBodyId() {
        return bodyId;
    }

    public double getDistanceLs() {
        return distanceLs;
    }

    public Double getGravityMS() {
        return gravityMS;
    }

    public boolean isLandable() {
        return landable;
    }

    public boolean hasBio() {
        return isHasBio();
    }

    public boolean hasGeo() {
        return isHasGeo();
    }

    public boolean isHighValue() {
        return highValue;
    }

    public String getAtmoOrType() {
        return atmoOrType;
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

    public String getVolcanism() {
        return volcanism;
    }

    public List<ExobiologyData.BioCandidate> getPredictions() {
        return predictions;
    }

    public Set<String> getObservedGenusPrefixes() {
        return observedGenusPrefixes;
    }
    
    public java.util.Set<String> getObservedBioDisplayNames() {
        return observedBioDisplayNames;
    }

    public void setObservedBioDisplayNames(java.util.Set<String> observedBioDisplayNames) {
        this.observedBioDisplayNames = observedBioDisplayNames;
    }
    public String getDiscoveryCommander() {
        return discoveryCommander;
    }

    public void setDiscoveryCommander(String discoveryCommander) {
        this.discoveryCommander = discoveryCommander;
    }

    /**
     * Convenience helper: true if EDSM reports a non-empty discovery.commander
     * for this body.
     */
    public boolean hasDiscoveryCommander() {
        return getDiscoveryCommander() != null && !getDiscoveryCommander().isBlank();
    }


    // ------------------------------------------------------------
    // Mutators
    // ------------------------------------------------------------

    public void setBodyName(String name) {
        this.name = name;
    }

    public void setBodyShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setBodyId(int bodyId) {
        this.bodyId = bodyId;
    }

    public void setDistanceLs(double distanceLs) {
        this.distanceLs = distanceLs;
    }

    public void setGravityMS(Double gravityMS) {
        this.gravityMS = gravityMS;
    }

    public void setLandable(boolean landable) {
        this.landable = landable;
    }

    public void setHasBio(boolean hasBio) {
        this.hasBio = hasBio;
    }

    public void setHasGeo(boolean hasGeo) {
        this.hasGeo = hasGeo;
    }

    public void setHighValue(boolean highValue) {
        this.highValue = highValue;
    }

    public void setAtmoOrType(String atmoOrType) {
        this.atmoOrType = atmoOrType;
    }

    public void setPlanetClass(String planetClass) {
        this.planetClass = planetClass;
    }

    public void setAtmosphere(String atmosphere) {
        this.atmosphere = atmosphere;
    }

    public void setSurfaceTempK(Double surfaceTempK) {
        this.surfaceTempK = surfaceTempK;
    }

    public void setVolcanism(String volcanism) {
        this.volcanism = volcanism;
    }

    public void setPredictions(List<ExobiologyData.BioCandidate> predictions) {
        this.predictions = predictions;
    }

    public void clearPredictions() {
        if (getPredictions() != null) {
            getPredictions().clear();
        }
    }

    public void setObservedGenusPrefixes(Set<String> observedGenusPrefixes) {
        this.observedGenusPrefixes = observedGenusPrefixes;
    }
    
    // ------------------------------------------------------------
    // Genus observation handling
    // ------------------------------------------------------------

    public void addObservedGenus(String genusPrefix) {
        if (genusPrefix == null || genusPrefix.isEmpty()) {
            return;
        }
        if (getObservedGenusPrefixes() == null) {
            setObservedGenusPrefixes(new HashSet<>());
        }
        getObservedGenusPrefixes().add(toLower(genusPrefix));
    }

    // ------------------------------------------------------------
    // Build exobiology prediction attributes
    // ------------------------------------------------------------

    /**
     * Convert this body into ExobiologyData.BodyAttributes.
     * Returns null if insufficient data is present.
     */
    public ExobiologyData.BodyAttributes buildBodyAttributes() {
        double gravityG = Double.NaN;
        if (getGravityMS() != null && !Double.isNaN(getGravityMS())) {
            gravityG = getGravityMS() / 9.80665;
        }

        if ((getPlanetClass() == null || getPlanetClass().isEmpty())
                && (getAtmosphere() == null || getAtmosphere().isEmpty())
                && Double.isNaN(gravityG)) {
            return null; // Not enough info to predict
        }
        String pc = getPlanetClass();
        PlanetType pt = ExobiologyData.parsePlanetType(pc);
        AtmosphereType at = ExobiologyData.parseAtmosphere(getAtmosphere());

        double tempMin = getSurfaceTempK() != null ? getSurfaceTempK() : Double.NaN;
        double tempMax = tempMin;

        boolean hasVolc = getVolcanism() != null && !getVolcanism().isEmpty() && !getVolcanism().toLowerCase().startsWith("no volcanism");

        BodyAttributes attr = new BodyAttributes(
        		getBodyName(),
        		getStarSystem(),
        		starPos,
                pt,
                gravityG,
                at,
                tempMin,
                tempMax,
                surfacePressure,
                hasVolc,
                getVolcanism()
        );
//        public BodyAttributes(String bodyName,
//                PlanetType planetType,
//                double gravity,
//                AtmosphereType atmosphere,
//                double tempKMin,
//                double tempKMax,
//                double pressure,
//                boolean hasVolcanism,
//                String volcanismType,
//                Map<String, Double> atmosphereComponents,
//                Double orbitalPeriod,
//                Double distance,
//                Boolean guardian,
//                String nebula,
//                String parentStar,
//                String region,
//                String starClass) {
        
        
        
        
        return attr;
    }

    public void addObservedGenusPrefix(String genus) {
        if (genus == null || genus.isEmpty()) {
            return;
        }
        if (getObservedGenusPrefixes() == null) {
            setObservedGenusPrefixes(new java.util.HashSet<>());
        }
        getObservedGenusPrefixes().add(genus.toLowerCase(java.util.Locale.ROOT));
    }

    public void addObservedBioDisplayName(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        if (getObservedBioDisplayNames() == null) {
            setObservedBioDisplayNames(new java.util.HashSet<>());
        }
        getObservedBioDisplayNames().add(name);
    }

    
    // ------------------------------------------------------------
    // Utils
    // ------------------------------------------------------------

    private static String toLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

	public boolean isHasBio() {
		return hasBio;
	}

	public boolean isHasGeo() {
		return hasGeo;
	}

	public void setAxialTilt(Double axialTilt) {
		this.axialTilt = axialTilt;
	}

	public void setRadius(Double radius) {
		this.radius = radius;
	}

	public Double getSurfacePressure() {
		return surfacePressure;
	}

	public void setSurfacePressure(Double surfacePressure) {
		this.surfacePressure = surfacePressure;
	}

	public void setStarSystem(String starSystem) {
		this.starSystem = starSystem;
	}

}
