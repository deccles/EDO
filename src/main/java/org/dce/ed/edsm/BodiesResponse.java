package org.dce.ed.edsm;

import java.util.List;

public class BodiesResponse {

    /** EDSM system id */
    public long id;

    /** System name */
    public String name;

    /** Optional id64 if present */
    public Long id64;

    /** List of celestial bodies (stars, planets, moons, etc.) */
    public List<Body> bodies;

    public static class Discovery {
    	public String commander;
    	String date;
    }
    public static class Body {
        public long id;
        public String name;

        // High-level type info
        public String type;          // "Star" or "Planet"
        public String subType;       // e.g. "B (Blue-White) Star", "High metal content world"

//		"discovery": {
//			"commander": "Azotox",
//			"date": "2025-01-14 14:58:02"
//		},
        
        public Discovery discovery;
        
        public Double distanceToArrival;
        public Boolean isMainStar;
        public Boolean isScoopable;

        // Stellar properties (when applicable)
        public Integer age;                 // in millions of years
        public String luminosity;
        public Double absoluteMagnitude;
        public Double solarMasses;
        public Double solarRadius;

        // Shared physical properties
        public Double surfaceTemperature;
        public Double earthMasses;
        public Double radius;
        public Double gravity;

        // Planet-specific environment
        public Boolean isLandable;
        public String volcanismType;
        public String atmosphereType;
        public String terraformingState;

        // Orbital / rotational parameters
        public Double orbitalPeriod;
        public Double semiMajorAxis;
        public Double orbitalEccentricity;
        public Double orbitalInclination;
        public Double argOfPeriapsis;
        public Double rotationalPeriod;
        public Boolean rotationalPeriodTidallyLocked;
        public Double axialTilt;

        // Rings if present
        public List<Ring> rings;
		public Double surfaceGravity;

        public static class Ring {
            public String name;
            public String type;

            // <-- Changed from Long to Double to handle values like 1879.9
            public Double mass;

            public Double innerRadius;
            public Double outerRadius;
        }
    }
}
