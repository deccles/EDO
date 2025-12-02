package org.dce.ed.edsm;

public class DeathsResponse {
    public long id;
    public String name;
    public Death death;

    public static class Death {
        public int total;
        public int week;
        public int day;
    }
}
