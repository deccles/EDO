package org.dce.ed.edsm;

/**
 * Matches https://www.edsm.net/api-v1/system output when using:
 *  showId=1&showCoordinates=1&showPermit=1&showInformation=1&showPrimaryStar=1
 */
public class ShowSystemResponse {

    // Core fields
    private String name;
    private Integer id;          // EDSM internal ID
    private Coordinates coords;

    // Permit info
    private Boolean requirePermit;
    private String permitName;

    // Extra info block (can be null or empty)
    private Information information;

    // Primary star info (can be null)
    private PrimaryStar primaryStar;

    // --- getters ---

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public Coordinates getCoords() {
        return coords;
    }

    public Boolean getRequirePermit() {
        return requirePermit;
    }

    public String getPermitName() {
        return permitName;
    }

    public Information getInformation() {
        return information;
    }

    public PrimaryStar getPrimaryStar() {
        return primaryStar;
    }

    // --- nested types matching EDSM JSON ---

    public static class Coordinates {
        private double x;
        private double y;
        private double z;

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }

    public static class Information {
        // All of these may be null depending on what's stored
        private String allegiance;
        private String government;
        private String faction;
        private String factionState;
        private Long population;
        private String security;
        private String economy;

        public String getAllegiance() {
            return allegiance;
        }

        public String getGovernment() {
            return government;
        }

        public String getFaction() {
            return faction;
        }

        public String getFactionState() {
            return factionState;
        }

        public Long getPopulation() {
            return population;
        }

        public String getSecurity() {
            return security;
        }

        public String getEconomy() {
            return economy;
        }
    }

    public static class PrimaryStar {
        private String type;
        private String name;
        private Boolean isScoopable;

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Boolean getIsScoopable() {
            return isScoopable;
        }
    }
}
