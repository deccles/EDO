package org.dce.ed.mining;

import org.dce.ed.OverlayPreferences;

/**
 * Creates the prospector log backend from preferences (local CSV or Google Sheets).
 */
public final class ProspectorLogBackendFactory {

    private ProspectorLogBackendFactory() {
    }

    /**
     * Returns the backend to use: Google Sheets if backend is "google" and URL is set, otherwise local CSV.
     */
    public static ProspectorLogBackend create() {
        String backend = OverlayPreferences.getMiningLogBackend();
        String url = OverlayPreferences.getMiningGoogleSheetsUrl();
        if ("google".equals(backend) && url != null && !url.isBlank()) {
            return new GoogleSheetsBackend(url);
        }
        return new LocalCsvBackend();
    }
}
