package org.dce.ed.mining;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.dce.ed.MiningTabPanel;

/**
 * Prospector log backend that writes to and reads from a local CSV file.
 * CSV columns: run,body,timestamp,material,percent,before amount,after amount,difference,email address
 */
public final class LocalCsvBackend implements ProspectorLogBackend {

    private static final String HEADER = "run,body,timestamp,material,percent,before amount,after amount,difference,email address";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.US);

    private final Path csvPath;

    public LocalCsvBackend(Path csvPath) {
        this.csvPath = csvPath != null ? csvPath : defaultPath();
    }

    public LocalCsvBackend() {
        this(defaultPath());
    }

    private static Path defaultPath() {
        return Paths.get(System.getProperty("user.home", ""), "EDO").resolve("prospector_log.csv");
    }

    @Override
    public void appendRows(List<ProspectorLogRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        try {
            Path parent = csvPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            boolean newFile = !Files.exists(csvPath);
            if (newFile) {
                Files.writeString(csvPath, HEADER + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            ZoneId zone = ZoneId.systemDefault();
            for (ProspectorLogRow r : rows) {
                String tsStr = r.getTimestamp() != null ? r.getTimestamp().atZone(zone).format(TIMESTAMP_FORMAT) : "";
                if (tsStr.isEmpty()) {
                    tsStr = "-";
                }
                String line = r.getRun() + ","
                    + MiningTabPanel.csvEscape(r.getFullBodyName()) + ","
                    + MiningTabPanel.csvEscape(tsStr) + ","
                    + MiningTabPanel.csvEscape(r.getMaterial()) + ","
                    + formatDouble(r.getPercent()) + ","
                    + formatDouble(r.getBeforeAmount()) + ","
                    + formatDouble(r.getAfterAmount()) + ","
                    + formatDouble(r.getDifference()) + ","
                    + MiningTabPanel.csvEscape(r.getEmailAddress());
                Files.writeString(csvPath, line + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to append prospector log", e);
        }
    }

    private static String formatDouble(double v) {
        if (Double.isNaN(v)) {
            return "0.00";
        }
        return String.format(Locale.US, "%.2f", v);
    }

    @Override
    public List<ProspectorLogRow> loadRows() {
        if (!Files.exists(csvPath)) {
            return List.of();
        }
        List<ProspectorLogRow> out = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                return List.of();
            }
            // Skip header; column order: run,body,timestamp,material,percent,before amount,after amount,difference,email address
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 9) {
                    continue;
                }
                try {
                    int run = Integer.parseInt(cols.get(0).trim());
                    String fullBodyName = cols.get(1).trim();
                    Instant ts = parseTimestamp(cols.get(2).trim());
                    String material = cols.get(3).trim();
                    double percent = parseDouble(cols.get(4), 0.0);
                    double before = parseDouble(cols.get(5), 0.0);
                    double after = parseDouble(cols.get(6), 0.0);
                    double diff = parseDouble(cols.get(7), 0.0);
                    String email = cols.get(8).trim();
                    out.add(new ProspectorLogRow(run, fullBodyName, ts, material, percent, before, after, diff, email));
                } catch (Exception e) {
                    // skip malformed line
                }
            }
        } catch (Exception e) {
            // return what we have so far, or empty
        }
        return out;
    }

    private static Instant parseTimestamp(String s) {
        if (s == null || s.isBlank() || "-".equals(s)) {
            return null;
        }
        try {
            return java.time.LocalDateTime.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.US))
                .atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            try {
                return java.time.LocalDateTime.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy H:m:s", Locale.US))
                    .atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static double parseDouble(String s, double def) {
        if (s == null || s.isBlank()) {
            return def;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        return cols;
    }
}
