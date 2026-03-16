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

    public ProspectorLoadResult(Status status, List<ProspectorLogRow> rows) {
        this.status = status != null ? status : Status.ERROR;
        this.rows = rows != null ? rows : Collections.emptyList();
    }

    public Status getStatus() {
        return status;
    }

    public List<ProspectorLogRow> getRows() {
        return rows;
    }
}

