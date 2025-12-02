package org.dce.ed.edsm;

import java.util.List;

public class LogsResponse {

    /** Status code, e.g. 100 = OK, 201/202/etc = errors */
    public int msgnum;

    /** Human-readable message, e.g. "OK", "Missing commander name" */
    public String msg;

    /** Start date/time used by the request (if any) */
    public String startDateTime;

    /** End date/time used by the request (if any) */
    public String endDateTime;

    /** The actual flight log entries (may be null/empty on error) */
    public List<LogEntry> logs;

    public static class LogEntry {
        public Integer shipId;
        public String system;
        public Long systemId;
        public Boolean firstDiscover;
        public String date;
    }
}
