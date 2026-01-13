package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.dce.ed.logreader.event.ProspectedAsteroidEvent;
import org.dce.ed.logreader.event.ProspectedAsteroidEvent.MaterialProportion;
import org.dce.ed.mining.GalacticAveragePrices;
import org.dce.ed.ui.EdoUi;

/**
 * Overlay tab: Mining
 *
 * Shows the most recent ProspectedAsteroid materials sorted by galactic average value.
 *
 * Note: The journal only provides proportions. This tab uses user-configurable
 * heuristics (estimated total tons for Low/Medium/High and Core) to estimate
 * expected tons and resale value.
 */
public class MiningTabPanel extends JPanel {

    private final GalacticAveragePrices prices;

    private final JLabel headerLabel;
    private final JTable table;
    private final MiningTableModel model;

    private Font uiFont;

    public MiningTabPanel(GalacticAveragePrices prices) {
        super(new BorderLayout());
        this.prices = prices;

        // Always render transparent so passthrough mode looks right.
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));

        headerLabel = new JLabel("Mining (latest prospector)");
        headerLabel.setForeground(EdoUi.ED_ORANGE);
        headerLabel.setHorizontalAlignment(SwingConstants.LEFT);
        headerLabel.setOpaque(false);

        model = new MiningTableModel();

        table = new JTable(model) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public boolean editCellAt(int row, int column, java.util.EventObject e) {
                return false;
            }

            @Override
            protected void configureEnclosingScrollPane() {
                super.configureEnclosingScrollPane();

                Container p = SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
                if (p instanceof JScrollPane) {
                    JScrollPane sp = (JScrollPane)p;
                    sp.setBorder(null);
                    sp.setViewportBorder(null);

                    // Also neutralize any header viewport background/border some LAFs paint.
                    JViewport hv = sp.getColumnHeader();
                    if (hv != null) {
                        hv.setOpaque(false);
                        hv.setBackground(new Color(0, 0, 0, 0));
                        hv.setBorder(null);
                    }
                }
            }
        };

        // Hard-disable editing and selection to keep click-through visuals consistent.
        table.setDefaultEditor(Object.class, null);
        table.setDefaultEditor(String.class, null);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setSurrendersFocusOnKeystroke(false);
        table.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

        table.setOpaque(false);
        table.setBorder(null);
        table.setFillsViewportHeight(true);

        table.setShowGrid(false);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setGridColor(new Color(0, 0, 0, 0));

        table.setForeground(EdoUi.ED_ORANGE);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setRowHeight(22);

        // Header styling (no solid white header/background in passthrough)
        JTableHeader th = table.getTableHeader();
        if (th != null) {
            th.setOpaque(false);
            th.setForeground(EdoUi.ED_ORANGE);
            th.setBackground(new Color(0, 0, 0, 0));
            th.setBorder(null);
            th.setReorderingAllowed(false);
            th.setFocusable(false);
            th.putClientProperty("JTableHeader.focusCellBackground", null);
            th.putClientProperty("JTableHeader.cellBorder", null);
            th.setDefaultRenderer(new HeaderRenderer());

            // Tighten header height to match rows (avoids thick bar look)
            Dimension pref = th.getPreferredSize();
            th.setPreferredSize(new Dimension(pref.width, table.getRowHeight()));
        }

        // Default renderer: orange text, padding, and a semi-transparent orange separator line per row.
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            {
                setOpaque(false);
                setForeground(EdoUi.ED_ORANGE);
            }

            @Override
            public Component getTableCellRendererComponent(JTable tbl,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Component c = super.getTableCellRendererComponent(tbl,
                                                                  value,
                                                                  false,
                                                                  false,
                                                                  row,
                                                                  column);

                if (c instanceof JLabel) {
                    JLabel l = (JLabel)c;
                    l.setForeground(EdoUi.ED_ORANGE);
                    l.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
                    l.setBorder(new EmptyBorder(3, 4, 3, 4));
                    l.setOpaque(false);
                }

                return c;
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                super.paintComponent(g2);

                g2.setColor(EdoUi.ED_ORANGE_TRANS);
                int y = getHeight() - 1;
                g2.drawLine(0, y, getWidth(), y);

                g2.dispose();
            }
        };
        table.setDefaultRenderer(Object.class, defaultRenderer);

        JScrollPane scroller = new JScrollPane(table);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        scroller.setViewportBorder(null);

        JViewport headerViewport = scroller.getColumnHeader();
        if (headerViewport != null) {
            headerViewport.setOpaque(false);
            headerViewport.setBackground(new Color(0, 0, 0, 0));
            headerViewport.setBorder(null);
        }

        add(headerLabel, BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);

        applyUiFontPreferences();
    }

    public void applyUiFontPreferences() {
        applyUiFont(OverlayPreferences.getUiFont());
    }

    public void applyUiFont(Font font) {
        if (font == null) {
            return;
        }

        uiFont = font;

        headerLabel.setFont(uiFont.deriveFont(Font.BOLD));

        table.setFont(uiFont);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));
        }

        // Keep row height readable as font size changes
        int rowH = Math.max(18, uiFont.getSize() + 6);
        table.setRowHeight(rowH);

        // Keep header height in sync with row height
        JTableHeader th = table.getTableHeader();
        if (th != null) {
            Dimension pref = th.getPreferredSize();
            th.setPreferredSize(new Dimension(pref.width, rowH));
        }

        revalidate();
        repaint();
    }

    public void applyOverlayTransparency(boolean transparent) {
        // Keep visuals consistent across tabs: Mining tab stays non-opaque.
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));
        table.setOpaque(false);
        repaint();
    }

    public void updateFromProspector(ProspectedAsteroidEvent event) {
        if (event == null) {
            model.setRows(List.of());
            headerLabel.setText("Mining (latest prospector)");
            return;
        }

        String motherlode = event.getMotherlodeMaterial();
        String content = event.getContent();

        // Pull localized names out of raw JSON if present.
        Map<String, String> localisedByRawLower = extractMaterialLocalisedNames(event.getRawJson());

        List<Row> rows = new ArrayList<>();

        double totalTons = estimateTotalTons(content);
        for (MaterialProportion mp : event.getMaterials()) {
            if (mp == null || mp.getName() == null) {
                continue;
            }

            String rawName = mp.getName();
            String localised = localisedByRawLower.get(rawName.trim().toLowerCase(Locale.US));
            String canonical = canonicalCommodityName(rawName, localised);

            int avg = lookupAvgSell(canonical);
            double tons = (mp.getProportion() / 100.0) * totalTons;
            double value = tons * avg;

            rows.add(new Row(canonical, avg, tons, value));
        }

        // If we have a motherlode material, add a "Core" line at the top.
        if (motherlode != null && !motherlode.isBlank()) {
            String rawName = motherlode;
            String localised = localisedByRawLower.get(rawName.trim().toLowerCase(Locale.US));
            String canonical = canonicalCommodityName(rawName, localised);

            int avg = lookupAvgSell(canonical);
            double tons = OverlayPreferences.getMiningEstimateTonsCore();
            double value = tons * avg;

            rows.add(new Row(canonical + " (Core)", avg, tons, value, true));
        }

        rows.sort(Comparator
                .comparingInt(Row::getAvgSell).reversed()
                .thenComparing(Row::getName, String.CASE_INSENSITIVE_ORDER));

        model.setRows(rows);

        String hdr = "Mining (" + (content == null ? "" : content) + ")";
        if (motherlode != null && !motherlode.isBlank()) {
            hdr += " - Motherlode: " + motherlode;
        }
        headerLabel.setText(hdr);
    }

    private int lookupAvgSell(String canonicalName) {
        if (canonicalName == null || canonicalName.isBlank()) {
            return 0;
        }

        int v = prices.getAvgSellCrPerTon(canonicalName).orElse(0);
        if (v != 0) {
            return v;
        }

        // Tolerate singular/plural mismatches ("Void Opal" vs "Void Opals").
        String n = canonicalName.trim();
        if (n.endsWith("s") && n.length() > 1) {
            v = prices.getAvgSellCrPerTon(n.substring(0, n.length() - 1)).orElse(0);
            if (v != 0) {
                return v;
            }
        } else {
            v = prices.getAvgSellCrPerTon(n + "s").orElse(0);
            if (v != 0) {
                return v;
            }
        }

        return 0;
    }

    private static String canonicalCommodityName(String rawName, String localisedName) {
        if (localisedName != null && !localisedName.isBlank()) {
            return localisedName.trim();
        }
        if (rawName == null) {
            return "";
        }

        String n = rawName.trim();
        String key = n.toLowerCase(Locale.US);

        // Known journal/internal aliases -> market name keys used by prices.
        if (key.equals("opal") || key.equals("voidopal") || key.equals("void_opal")) {
            return "Void Opals";
        }

        // Journal sometimes uses "$something_name;" style tokens
        if (n.startsWith("$") && n.endsWith(";") && n.length() > 2) {
            n = n.substring(1, n.length() - 1);
            if (n.endsWith("_name")) {
                n = n.substring(0, n.length() - "_name".length());
            }
        }

        // Basic fallback formatting
        n = n.replace('_', ' ');
        if (n.isEmpty()) {
            return n;
        }
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static Map<String, String> extractMaterialLocalisedNames(Object rawJson) {
        Map<String, String> out = new HashMap<>();
        if (rawJson == null) {
            return out;
        }

        JsonObject obj = null;
        if (rawJson instanceof JsonObject) {
            obj = (JsonObject) rawJson;
        } else if (rawJson instanceof String) {
            try {
                obj = JsonParser.parseString((String) rawJson).getAsJsonObject();
            } catch (Exception ex) {
                return out;
            }
        } else {
            // Unknown rawJson type in this build; just return empty.
            return out;
        }

        JsonElement matsEl = obj.get("Materials");
        if (matsEl == null || !matsEl.isJsonArray()) {
            return out;
        }

        JsonArray arr = matsEl.getAsJsonArray();
        for (JsonElement e : arr) {
            if (e == null || !e.isJsonObject()) {
                continue;
            }
            JsonObject m = e.getAsJsonObject();

            String name = optString(m, "Name");
            String loc = optString(m, "Name_Localised");
            if (name == null || name.isBlank() || loc == null || loc.isBlank()) {
                continue;
            }

            out.put(name.trim().toLowerCase(Locale.US), loc.trim());
        }

        return out;
    }

    private static String optString(JsonObject obj, String key) {
        if (obj == null || key == null) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) {
            return null;
        }
        try {
            return el.getAsString();
        } catch (Exception ex) {
            return null;
        }
    }

    private static double estimateTotalTons(String content) {
        if (content == null) {
            return OverlayPreferences.getMiningEstimateTonsMedium();
        }
        String c = content.trim().toLowerCase(Locale.US);
        if (c.equals("high")) {
            return OverlayPreferences.getMiningEstimateTonsHigh();
        }
        if (c.equals("low")) {
            return OverlayPreferences.getMiningEstimateTonsLow();
        }
        return OverlayPreferences.getMiningEstimateTonsMedium();
    }

    private static final class Row {
        private final String name;
        private final int avgSell;
        private final double expectedTons;
        private final double estimatedValue;
        private final boolean isCore;

        Row(String name, int avgSell, double expectedTons, double estimatedValue) {
            this(name, avgSell, expectedTons, estimatedValue, false);
        }

        Row(String name, int avgSell, double expectedTons, double estimatedValue, boolean isCore) {
            this.name = name;
            this.avgSell = avgSell;
            this.expectedTons = expectedTons;
            this.estimatedValue = estimatedValue;
            this.isCore = isCore;
        }

        String getName() {
            return name;
        }

        int getAvgSell() {
            return avgSell;
        }

        double getExpectedTons() {
            return expectedTons;
        }

        double getEstimatedValue() {
            return estimatedValue;
        }

        boolean isCore() {
            return isCore;
        }
    }

    private static final class MiningTableModel extends AbstractTableModel {

        private static final String[] COLS = new String[] {
                "Material",
                "Avg Cr/t",
                "Est. Tons",
                "Est. Value"
        };

        private final NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
        private final NumberFormat tonsFmt = NumberFormat.getNumberInstance(Locale.US);

        private List<Row> rows = List.of();

        MiningTableModel() {
            tonsFmt.setMaximumFractionDigits(1);
            tonsFmt.setMinimumFractionDigits(0);
        }

        void setRows(List<Row> newRows) {
            rows = (newRows == null) ? List.of() : List.copyOf(newRows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Row r = rows.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return r.getName();
            case 1:
                return r.getAvgSell() <= 0 ? "" : intFmt.format(r.getAvgSell());
            case 2:
                return tonsFmt.format(r.getExpectedTons());
            case 3:
                return r.getAvgSell() <= 0 ? "" : intFmt.format(Math.round(r.getEstimatedValue()));
            default:
                return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table,
                                                                       value,
                                                                       false,
                                                                       false,
                                                                       row,
                                                                       column);

            // Transparent header like SystemTabPanel: text + orange rule only.
            label.setOpaque(false);
            label.setBackground(new Color(0, 0, 0, 0));
            label.setForeground(EdoUi.ED_ORANGE_TRANS);
            label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
            label.setBorder(new EmptyBorder(0, 4, 0, 4));
            return label;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            super.paintComponent(g2);

            // Header separator rule
            g2.setColor(EdoUi.ED_ORANGE_TRANS);
            int y = getHeight() - 1;
            g2.drawLine(0, y, getWidth(), y);

            g2.dispose();
        }
    }
}
