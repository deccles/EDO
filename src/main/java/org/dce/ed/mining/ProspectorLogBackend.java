package org.dce.ed.mining;

import java.util.List;

/**
 * Backend for prospector log: append rows and load rows (local CSV or Google Sheets).
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
}
