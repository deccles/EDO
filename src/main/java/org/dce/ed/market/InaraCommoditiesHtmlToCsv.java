package org.dce.ed.market;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InaraCommoditiesHtmlToCsv {

    // One table row per commodity in the saved HTML
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "<tr[^>]*class\\s*=\\s*\"[^\"]*taggeditem[^\"]*\"[^>]*>(.*?)</tr>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Commodity name link inside the row
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*\"[^\"]*/elite/commodity/\\d+/?[^\"]*\"[^>]*>([^<]+)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // INARA puts sortable numeric values in data-order="12345" on the numeric <td>
    private static final Pattern DATA_ORDER_PATTERN = Pattern.compile(
            "data-order\\s*=\\s*\"(\\d+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private InaraCommoditiesHtmlToCsv() {
    }

    public static final class CommodityRow {
        private final String name; // human readable
        private final String key;  // normalized
        private final long avgSell;
        private final long avgBuy;
        private final long avgProfit;
        private final long maxSell;
        private final long minBuy;
        private final long maxProfit;

        public CommodityRow(String name,
                            String key,
                            long avgSell,
                            long avgBuy,
                            long avgProfit,
                            long maxSell,
                            long minBuy,
                            long maxProfit) {
            this.name = name;
            this.key = key;
            this.avgSell = avgSell;
            this.avgBuy = avgBuy;
            this.avgProfit = avgProfit;
            this.maxSell = maxSell;
            this.minBuy = minBuy;
            this.maxProfit = maxProfit;
        }

        public String getName() { return name; }
        public String getKey() { return key; }
        public long getAvgSell() { return avgSell; }
        public long getAvgBuy() { return avgBuy; }
        public long getAvgProfit() { return avgProfit; }
        public long getMaxSell() { return maxSell; }
        public long getMinBuy() { return minBuy; }
        public long getMaxProfit() { return maxProfit; }
    }

    public static List<CommodityRow> parse(Path htmlFile) throws IOException {
        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);

        List<CommodityRow> rows = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(html);

        while (rowMatcher.find()) {
            String rowHtml = rowMatcher.group(1);
            String name = extractName(rowHtml);
            if (name == null || name.isBlank()) {
                continue;
            }
            name = htmlDecode(name).trim();

            // Grab numeric columns in the order they appear in the row.
            // INARA’s commodities list numeric columns are:
            // Avg sell | Avg buy | Avg profit | Max sell | Min buy | Max profit
            List<Long> nums = extractDataOrderNumbers(rowHtml);
            if (nums.size() < 6) {
                // If INARA changes formatting, don’t write partial garbage
                continue;
            }

            long avgSell = nums.get(0);
            long avgBuy = nums.get(1);
            long avgProfit = nums.get(2);
            long maxSell = nums.get(3);
            long minBuy = nums.get(4);
            long maxProfit = nums.get(5);

            String key = normalizeKey(name);

            rows.add(new CommodityRow(name, key, avgSell, avgBuy, avgProfit, maxSell, minBuy, maxProfit));
        }

        // Stable ordering (nice diffs)
        rows.sort(Comparator.comparing(CommodityRow::getName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    public static void writeCsv(Path outFile, List<CommodityRow> rows) throws IOException {
        Files.createDirectories(outFile.getParent());

        try (BufferedWriter w = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            w.write("name,key,avg_sell,avg_buy,avg_profit,max_sell,min_buy,max_profit");
            w.newLine();

            for (CommodityRow r : rows) {
                w.write(csv(r.getName()));
                w.write(",");
                w.write(csv(r.getKey()));
                w.write(",");
                w.write(Long.toString(r.getAvgSell()));
                w.write(",");
                w.write(Long.toString(r.getAvgBuy()));
                w.write(",");
                w.write(Long.toString(r.getAvgProfit()));
                w.write(",");
                w.write(Long.toString(r.getMaxSell()));
                w.write(",");
                w.write(Long.toString(r.getMinBuy()));
                w.write(",");
                w.write(Long.toString(r.getMaxProfit()));
                w.newLine();
            }
        }
    }

    private static String extractName(String rowHtml) {
        Matcher m = NAME_PATTERN.matcher(rowHtml);
        if (!m.find()) {
            return null;
        }
        return m.group(1);
    }

    private static List<Long> extractDataOrderNumbers(String rowHtml) {
        List<Long> out = new ArrayList<>(8);
        Matcher m = DATA_ORDER_PATTERN.matcher(rowHtml);
        while (m.find()) {
            String s = m.group(1);
            try {
                out.add(Long.parseLong(s));
            } catch (Exception e) {
                // ignore
            }
        }
        return out;
    }

    /**
     * Normalized key that matches well with journal commodity names after similar normalization.
     * Keeps only [a-z0-9], lowercase, strips diacritics.
     */
    public static String normalizeKey(String name) {
        if (name == null) {
            return "";
        }

        String s = name.trim();

        s = Normalizer.normalize(s, Normalizer.Form.NFKD);
        s = s.replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]+", "");

        return s;
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        // Quote if needed
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String t = s.replace("\"", "\"\"");
        if (needsQuote) {
            return "\"" + t + "\"";
        }
        return t;
    }

    private static String htmlDecode(String s) {
        if (s == null || s.indexOf('&') < 0) {
            return s;
        }

        String out = s;
        out = out.replace("&amp;", "&");
        out = out.replace("&lt;", "<");
        out = out.replace("&gt;", ">");
        out = out.replace("&quot;", "\"");
        out = out.replace("&#39;", "'");

        return out;
    }

    /**
     * Main method with defaults you showed, but overridable:
     *   args[0] = html input path
     *   args[1] = csv output path
     */
    public static void main(String[] args) throws Exception {
        Path defaultHtml = Paths.get("src/main/resources/market/Commodities _ Elite_Dangerous _ INARA.html");
        Path defaultCsv = Paths.get("src/main/resources/market/inara_commodities.csv");

        Path htmlFile = defaultHtml;
        Path csvFile = defaultCsv;

        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            htmlFile = Paths.get(args[0]);
        }
        if (args != null && args.length > 1 && args[1] != null && !args[1].isBlank()) {
            csvFile = Paths.get(args[1]);
        }

        if (!Files.exists(htmlFile)) {
            System.err.println("INARA HTML file not found: " + htmlFile.toAbsolutePath());
            System.exit(2);
        }

        List<CommodityRow> rows = parse(htmlFile);
        writeCsv(csvFile, rows);

        System.out.println("Parsed " + rows.size() + " commodities.");
        System.out.println("Input:  " + htmlFile.toAbsolutePath());
        System.out.println("Output: " + csvFile.toAbsolutePath());
    }
}
