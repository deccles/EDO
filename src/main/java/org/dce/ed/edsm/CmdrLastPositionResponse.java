package org.dce.ed.edsm;

public class CmdrLastPositionResponse {

    private int msgnum;
    private String msg;
    private String system;
    private Long systemId;
    private Boolean firstDiscover;
    private String date;
    private Coordinates coordinates;
    private String url;

    public int getMsgnum() {
        return msgnum;
    }

    public String getMsg() {
        return msg;
    }

    public String getSystem() {
        return system;
    }

    public Long getSystemId() {
        return systemId;
    }

    public Boolean getFirstDiscover() {
        return firstDiscover;
    }

    public String getDate() {
        return date;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public String getUrl() {
        return url;
    }

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
}
