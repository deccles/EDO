package org.dce.ed.edsm;

public class SphereSystemsResponse {
    public long id;
    public String name;
    public double distance;
    public Coordinates coords;

    public static class Coordinates {
        public double x;
        public double y;
        public double z;
    }
}
