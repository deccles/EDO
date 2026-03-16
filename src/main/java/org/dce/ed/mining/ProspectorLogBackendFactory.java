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

        // #region agent log
        try (java.io.FileWriter fw = new java.io.FileWriter("debug-78e78c.log", true)) {
            long tsMs = System.currentTimeMillis();
            String id = java.util.UUID.randomUUID().toString();
            String safeUrl = url == null ? "" : url;
            String json = "{\"sessionId\":\"78e78c\",\"id\":\"" + id
                + "\",\"timestamp\":" + tsMs
                + ",\"location\":\"ProspectorLogBackendFactory.create\""
                + ",\"message\":\"select backend\""
                + ",\"data\":{\"backend\":\"" + backend
                + "\",\"urlBlank\":" + (safeUrl.isBlank())
                + "}"
                + ",\"runId\":\"pre-fix\",\"hypothesisId\":\"H1\"}\n";
            fw.write(json);
        } catch (Exception ignored) {
        }
        // #endregion

        if ("google".equals(backend) && url != null && !url.isBlank()) {
            return new GoogleSheetsBackend(url);
        }
        return new LocalCsvBackend();
    }
}
