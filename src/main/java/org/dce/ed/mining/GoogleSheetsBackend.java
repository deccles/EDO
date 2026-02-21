package org.dce.ed.mining;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prospector log backend that writes to and reads from a Google Sheet.
 * Column order: Run, Body, Timestamp, Type, Percentage, Before Amount, After Amount, Actual, Email Address (A:I).
 * <p>
 * Requires OAuth 2.0 desktop flow and stored refresh token to be implemented for append/read.
 * Until then, append is a no-op and load returns an empty list.
 */
public final class GoogleSheetsBackend implements ProspectorLogBackend {

    private static final Pattern SPREADSHEET_ID_PATTERN = Pattern.compile("/d/([a-zA-Z0-9_-]+)");

    private final String spreadsheetId;
    private final String url;

    public GoogleSheetsBackend(String spreadsheetUrl) {
        this.url = spreadsheetUrl != null ? spreadsheetUrl.trim() : "";
        this.spreadsheetId = parseSpreadsheetId(this.url);
    }

    /**
     * Extract spreadsheet ID from the edit URL, e.g.
     * https://docs.google.com/spreadsheets/d/18bYWZFYQWKZREZIMOh6A/edit?gid=43311951
     */
    static String parseSpreadsheetId(String spreadsheetUrl) {
        if (spreadsheetUrl == null || spreadsheetUrl.isBlank()) {
            return "";
        }
        Matcher m = SPREADSHEET_ID_PATTERN.matcher(spreadsheetUrl);
        return m.find() ? m.group(1) : "";
    }

    @Override
    public void appendRows(List<ProspectorLogRow> rows) {
        if (rows == null || rows.isEmpty() || spreadsheetId.isEmpty()) {
            return;
        }
        // TODO: OAuth 2.0 desktop flow, then use Sheets API to append values to sheet (A:I).
        // Sheets.Spreadsheets.Values.append with valueInputOption USER_ENTERED.
    }

    @Override
    public List<ProspectorLogRow> loadRows() {
        if (spreadsheetId.isEmpty()) {
            return Collections.emptyList();
        }
        // TODO: Use stored credential to call Sheets.Spreadsheets.Values.get for range A:I (or used range).
        // Map rows to ProspectorLogRow (parse timestamp, numbers).
        return Collections.emptyList();
    }
}
