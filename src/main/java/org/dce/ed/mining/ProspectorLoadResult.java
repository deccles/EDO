package org.dce.ed.mining;

import java.util.Collections;
import java.util.List;

/**
 * Result of loading prospector log rows from a backend, with explicit status.
 */
public final class ProspectorLoadResult {

    public enum Status {
        OK,
        EMPTY_SHEET,
        ERROR
    }

    private final Status status;
    private final List<ProspectorLogRow> rows;
    /** Optional detail for {@link Status#ERROR} (e.g. API message). */
    private final String detailMessage;

    public ProspectorLoadResult(Status status, List<ProspectorLogRow> rows) {
        this(status, rows, null);
    }

    public ProspectorLoadResult(Status status, List<ProspectorLogRow> rows, String detailMessage) {
        this.status = status != null ? status : Status.ERROR;
        this.rows = rows != null ? rows : Collections.emptyList();
        this.detailMessage = detailMessage;
    }

    public Status getStatus() {
        return status;
    }

    public List<ProspectorLogRow> getRows() {
        return rows;
    }

    /**
     * When status is {@link Status#ERROR}, a short message suitable for the status bar (may be null).
     */
    public String getDetailMessage() {
        return detailMessage;
    }
}

