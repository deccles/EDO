package org.dce.ed.mining;

import java.time.Instant;
import java.util.List;

/**
 * Backend for prospector log: append rows, load rows, and update run end time (local CSV or Google Sheets).
 */
public interface ProspectorLogBackend {

    /**
     * Append the given rows. May throw on I/O or API failure.
     */
    void appendRows(List<ProspectorLogRow> rows);

    /**
     * Load all rows. Returns empty list if none or on read failure (caller may log).
     */
    List<ProspectorLogRow> loadRows();

    /**
     * Set run end time on the canonical row (the row with run start time set) for the given run and commander.
     * No-op if no such row or backend does not support updates.
     */
    void updateRunEndTime(String commander, int run, Instant endTime);
}
