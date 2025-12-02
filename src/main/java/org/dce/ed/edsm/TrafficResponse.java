package org.dce.ed.edsm;

public class TrafficResponse {
    public String name;
    public long id;
    public Traffic traffic;

    public static class Traffic {
        public int total;
        public int week;
        public int day;
    }
}
