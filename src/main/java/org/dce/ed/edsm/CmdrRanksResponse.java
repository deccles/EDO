package org.dce.ed.edsm;

import com.google.gson.annotations.SerializedName;

public class CmdrRanksResponse {

    public int msgnum;
    public String msg;

    public Ranks ranks;
    public Progress progress;
    public RanksVerbose ranksVerbose;

    public static class Ranks {
        @SerializedName("Combat")
        public int combat;

        @SerializedName("Trade")
        public int trade;

        @SerializedName("Explore")
        public int explore;

        @SerializedName("CQC")
        public int cqc;

        @SerializedName("Federation")
        public int federation;

        @SerializedName("Empire")
        public int empire;
    }

    public static class Progress {
        @SerializedName("Combat")
        public int combat;

        @SerializedName("Trade")
        public int trade;

        @SerializedName("Explore")
        public int explore;

        @SerializedName("CQC")
        public int cqc;

        @SerializedName("Federation")
        public int federation;

        @SerializedName("Empire")
        public int empire;
    }

    public static class RanksVerbose {
        @SerializedName("Combat")
        public String combat;

        @SerializedName("Trade")
        public String trade;

        @SerializedName("Explore")
        public String explore;

        @SerializedName("CQC")
        public String cqc;

        @SerializedName("Federation")
        public String federation;

        @SerializedName("Empire")
        public String empire;
    }
}
