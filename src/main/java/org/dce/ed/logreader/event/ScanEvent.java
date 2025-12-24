package org.dce.ed.logreader.event;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * Scan event for stars and bodies.
 * Backed by the journal "Scan" event.
 */
public final class ScanEvent extends EliteLogEvent {

    /**
     * One entry from the journal Parents[] array.
     *
     * Example JSON element: {"Star": 5}
     */
    public static final class ParentRef {
        private final String type;
        private final int bodyId;

        public ParentRef(String type, int bodyId) {
            this.type = type;
            this.bodyId = bodyId;
        }

        public String getType() {
            return type;
        }

        public int getBodyId() {
            return bodyId;
        }

        @Override
        public String toString() {
            return type + ":" + bodyId;
        }
    }

    private final String bodyName;
    private final int bodyId;
    private final String starSystem;
    public String getStarSystem() {
		return starSystem;
	}

	private final long systemAddress;
    private final double distanceFromArrivalLs;
    private final boolean landable;
    private final String planetClass;
    private final String atmosphere;
    private final String terraformState;
    private final Double surfaceGravity;
    private final Double surfaceTemperature;
    private final Double orbitalPeriod;
    private final String volcanism;
    private final boolean wasDiscovered;
    private final boolean wasMapped;
    private final boolean wasFootfalled;

	private final String starType;
    private final List<ParentRef> parents;
	private Double surfacePressure;

    public ScanEvent(Instant timestamp,
                     JsonObject rawJson,
                     String bodyName,
                     int bodyId,
                     String starSystem,
                     long systemAddress,
                     double distanceFromArrivalLs,
                     boolean landable,
                     String planetClass,
                     String atmosphere,
                     String terraformState,
                     Double surfaceGravity,
                     Double surfacePressure,
                     Double surfaceTemperature,
                     Double orbitalPeriod,
                     String volcanism,
                     boolean wasDiscovered,
                     boolean wasMapped,
                     boolean wasFootfalled, 
                     String starType,
                     List<ParentRef> parents) {

        super(timestamp, EliteEventType.SCAN, rawJson);
        this.bodyName = bodyName;
        this.bodyId = bodyId;
        this.starSystem = starSystem;
        this.systemAddress = systemAddress;
        this.distanceFromArrivalLs = distanceFromArrivalLs;
        this.landable = landable;
        this.planetClass = planetClass;
        this.atmosphere = atmosphere;
        this.terraformState = terraformState;
        this.surfaceGravity = surfaceGravity;
        this.surfacePressure = surfacePressure;
        this.surfaceTemperature = surfaceTemperature;
        this.orbitalPeriod = orbitalPeriod;
        this.volcanism = volcanism;
        this.wasDiscovered = wasDiscovered;
        this.wasMapped = wasMapped;
        this.wasFootfalled = wasFootfalled;
        
        this.starType = starType;
        this.parents = (parents == null) ? Collections.emptyList() : parents;
    }

    public String getBodyName() {
        return bodyName;
    }

    public int getBodyId() {
        return bodyId;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public double getDistanceFromArrivalLs() {
        return distanceFromArrivalLs;
    }

    public boolean isLandable() {
        return landable;
    }

    public String getPlanetClass() {
        return planetClass;
    }

    public String getAtmosphere() {
        return atmosphere;
    }

    public String getTerraformState() {
        return terraformState;
    }

    public Double getSurfaceGravity() {
        return surfaceGravity;
    }

    /** Surface temperature in Kelvin (may be null if not present). */
    public Double getSurfaceTemperature() {
        return surfaceTemperature;
    }

    /** Raw Volcanism string from the journal (may be null/empty). */
    public String getVolcanism() {
        return volcanism;
    }

    public boolean isWasDiscovered() {
        return wasDiscovered;
    }

    public boolean isWasMapped() {
        return wasMapped;
    }

    public String getStarType() {
        return starType;
    }

    public List<ParentRef> getParents() {
        return parents;
    }

	public Double getSurfacePressure() {
		return surfacePressure;
	}

	public Double getOrbitalPeriod() {
		return orbitalPeriod;
	}

	/**
	 * @return the wasFootfalled
	 */
	public boolean isWasFootfalled() {
		return wasFootfalled;
	}
}