package org.dce.ed.edsm;

import java.util.List;

/**
 * Matches https://www.edsm.net/api-system-v1/stations output:
 *
 * {
 *   id: 4532,
 *   name: "Achali",
 *   stations: [ { ... }, ... ]
 * }
 */
public class SystemStationsResponse {

    private int id;
    private String name;
    private List<Station> stations;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Station> getStations() {
        return stations;
    }

    public static class Station {
        private int id;
        private String name;
        private String type;
        private Double distanceToArrival;
        private String allegiance;
        private String government;
        private String economy;
        private Boolean haveMarket;
        private Boolean haveShipyard;
        private ControllingFaction controllingFaction;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Double getDistanceToArrival() {
            return distanceToArrival;
        }

        public String getAllegiance() {
            return allegiance;
        }

        public String getGovernment() {
            return government;
        }

        public String getEconomy() {
            return economy;
        }

        public Boolean getHaveMarket() {
            return haveMarket;
        }

        public Boolean getHaveShipyard() {
            return haveShipyard;
        }

        public ControllingFaction getControllingFaction() {
            return controllingFaction;
        }
    }

    public static class ControllingFaction {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
