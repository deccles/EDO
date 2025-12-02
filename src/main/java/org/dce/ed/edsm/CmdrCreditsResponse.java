package org.dce.ed.edsm;

import java.util.List;

public class CmdrCreditsResponse {

    private int msgnum;
    private String msg;
    private List<CreditEntry> credits;

    public int getMsgnum() {
        return msgnum;
    }

    public String getMsg() {
        return msg;
    }

    public List<CreditEntry> getCredits() {
        return credits;
    }

    public static class CreditEntry {
        private long balance;
        private long loan;
        private String date;

        public long getBalance() {
            return balance;
        }

        public long getLoan() {
            return loan;
        }

        public String getDate() {
            return date;
        }
    }
}
