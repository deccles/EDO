package org.dce.ed.edsm;

import java.util.List;

public class ShowSystemResponse {

    public System system;

    public static class System {
        public long id;
        public String name;
        public Information information;
        public java.util.List<Station> stations;
    }

    public static class Information {
        public String allegiance;
        public String economy;
        public String security;
        public long population;
    }

    public static class Station {
        public long id;
        public String name;
        public String type;
        public Double distanceToArrival;
        public List<String> services;
    }
}
