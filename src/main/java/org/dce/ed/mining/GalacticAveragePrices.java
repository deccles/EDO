package org.dce.ed.mining;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Loads INARA commodity "avg sell" values from a bundled CSV (no network calls).
 *
 * Expected resource:
 *   /market/inara_commodities.csv
 *
 * Expected header:
 *   name,key,avg_sell,avg_buy,avg_profit,max_sell,min_buy,max_profit
 *
 * Lookup is by normalized key (lowercase alnum only). The CSV includes that key already.
 */
public final class GalacticAveragePrices {

    private static final String RESOURCE = "/market/inara_commodities.csv";

    private final Map<String, Integer> avgSellByKey;
    private final Map<String, String> displayNameByKey;

    private GalacticAveragePrices(Map<String, Integer> avgSellByKey, Map<String, String> displayNameByKey) {
        this.avgSellByKey = Map.copyOf(avgSellByKey);
        this.displayNameByKey = Map.copyOf(displayNameByKey);
    }

    public static GalacticAveragePrices loadDefault() {
        Map<String, Integer> avgSell = new HashMap<>();
        Map<String, String> names = new HashMap<>();

        try (InputStream in = GalacticAveragePrices.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                // Not found on classpath; return empty (callers should handle OptionalInt.empty()).
                return new GalacticAveragePrices(avgSell, names);
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String header = r.readLine();
                if (header == null) {
                    return new GalacticAveragePrices(avgSell, names);
                }

                // Determine column indexes by header name to be resilient to column reordering.
                String[] headerCols = splitCsvLine(header);
                int idxName = findHeaderIndex(headerCols, "name");
                int idxKey = findHeaderIndex(headerCols, "key");
                int idxAvgSell = findHeaderIndex(headerCols, "avg_sell");

                if (idxKey < 0 || idxAvgSell < 0) {
                    return new GalacticAveragePrices(avgSell, names);
                }

                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] cols = splitCsvLine(line);
                    if (idxKey >= cols.length || idxAvgSell >= cols.length) {
                        continue;
                    }

                    String key = cols[idxKey].trim();
                    if (key.isEmpty()) {
                        continue;
                    }

                    int v;
                    try {
                        v = Integer.parseInt(cols[idxAvgSell].trim());
                    } catch (Exception e) {
                        continue;
                    }
                    if (v <= 0) {
                        continue;
                    }

                    // Keep first occurrence if duplicates exist (shouldn't happen, but stable behavior).
                    avgSell.putIfAbsent(key, v);

                    if (idxName >= 0 && idxName < cols.length) {
                        String name = cols[idxName].trim();
                        if (!name.isEmpty()) {
                            names.putIfAbsent(key, name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fail closed (empty maps). No logging here since you may not want noise on startup.
        }

        return new GalacticAveragePrices(avgSell, names);
    }

    /**
     * Lookup Avg sell credits/ton by journal material/commodity name.
     *
     * Accepts names like:
     *  - "Low Temperature Diamonds"
     *  - "LowTemperatureDiamonds"
     *  - "$LowTemperatureDiamonds_Name;"
     */
    public OptionalInt getAvgSellCrPerTon(String journalMaterialName) {
        if (journalMaterialName == null || journalMaterialName.isBlank()) {
            return OptionalInt.empty();
        }

        String key = normalizeMaterialKey(journalMaterialName);
        Integer v = avgSellByKey.get(key);
        if (v == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(v);
    }

    /**
     * Human readable display name from the CSV for a journal material name, if available.
     */
    public String getDisplayName(String journalMaterialName) {
        if (journalMaterialName == null || journalMaterialName.isBlank()) {
            return null;
        }
        String key = normalizeMaterialKey(journalMaterialName);
        return displayNameByKey.get(key);
    }

    /**
     * Normalization aligned with how your overlay already treats material names:
     * - strips journal wrappers like $..._Name;
     * - keeps only alphanumerics, lowercased
     */
    public static String normalizeMaterialKey(String s) {
        if (s == null) {
            return "";
        }

        String t = s.trim();
        if (t.startsWith("$")) {
            t = t.substring(1);
        }
        t = t.replace("_name", "");
        t = t.replace("_Name", "");
        t = t.replace(";", "");

        StringBuilder out = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    private static int findHeaderIndex(String[] headerCols, String wanted) {
        for (int i = 0; i < headerCols.length; i++) {
            String h = headerCols[i].trim().toLowerCase(Locale.ROOT);
            if (h.equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Minimal CSV split that supports quoted fields and escaped quotes ("").
     * Enough for INARA output (commodity names can include spaces, punctuation, commas).
     */
    private static String[] splitCsvLine(String line) {
        // Fast path: no quotes
        if (line.indexOf('"') < 0) {
            return line.split(",", -1);
        }

        StringBuilder field = new StringBuilder(line.length());
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();

        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field.setLength(0);
                continue;
            }

            field.append(c);
        }

        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }
}
