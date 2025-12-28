package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.event.BioScanPredictionEvent;
import org.dce.ed.logreader.event.ScanOrganicEvent;
import org.dce.ed.logreader.event.StatusEvent;
import org.dce.ed.state.BodyInfo;
import org.dce.ed.state.SystemState;
import org.dce.ed.tts.PollyTtsCached;
import org.dce.ed.tts.TtsSprintf;

public class BiologyTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color ED_ORANGE = new Color(255, 140, 0);
    private static final Color ED_DARK = new Color(0, 0, 0);
    private static final Color TEXT_BLACK = Color.BLACK;

    private final JLabel header = new JLabel("Biology");
    private final BioTableModel model = new BioTableModel();
    private final JTable table = new JTable(model);
    private final JScrollPane scroll = new JScrollPane(table);

    private SystemTabPanel systemTab;

    private final TtsSprintf tts = new TtsSprintf(new PollyTtsCached());

    private Double currentLat;
    private Double currentLon;
    private Double currentPlanetRadius;
    private String currentBodyName;

    private final Map<String, Boolean> insideStateByBioKey = new HashMap<>();

    public BiologyTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        header.setOpaque(false);
        header.setForeground(ED_ORANGE);
        add(header, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setShowGrid(false);

        table.setOpaque(false);
        table.setDefaultRenderer(Object.class, new BioTextCellRenderer(model));

        table.getColumnModel().getColumn(0).setPreferredWidth(260); // Bio
        table.getColumnModel().getColumn(1).setPreferredWidth(70);  // Count
        table.getColumnModel().getColumn(2).setPreferredWidth(340); // Samples

        table.getColumnModel().getColumn(2).setCellRenderer(new SamplePillsRenderer());

        styleHeader(table);

        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        setPreferredSize(new Dimension(540, 320));
    }

    public void setSystemTabPanel(SystemTabPanel systemTab) {
        this.systemTab = systemTab;
    }

    public void handleLogEvent(EliteLogEvent event) {
        if (event == null) {
            return;
        }

        if (event instanceof StatusEvent) {
            StatusEvent e = (StatusEvent) event;

            currentLat = e.getLatitude();
            currentLon = e.getLongitude();
            currentPlanetRadius = e.getPlanetRadius();
            currentBodyName = e.getBodyName();

            refreshTableForCurrentBody();

            if (e.isOnFoot() || e.isInSrv()) {
                updateVoiceTransitions();
            }
            return;
        }

        if (event instanceof BioScanPredictionEvent || event instanceof ScanOrganicEvent) {
            refreshTableForCurrentBody();
        }
    }

    private void refreshTableForCurrentBody() {
        if (systemTab == null) {
            return;
        }

        if (currentBodyName == null || currentBodyName.isBlank()) {
            model.setRows(Collections.emptyList());
            return;
        }

        SystemState state = systemTab.getState();
        if (state == null) {
            model.setRows(Collections.emptyList());
            return;
        }

        BodyInfo body = findBodyByName(state, currentBodyName);
        if (body == null) {
            model.setRows(Collections.emptyList());
            return;
        }

        List<BioRow> rows = buildRows(body);
        model.setRows(rows);

        if (currentLat != null && currentLon != null && currentPlanetRadius != null) {
            model.updateDistances(currentLat.doubleValue(), currentLon.doubleValue(), currentPlanetRadius.doubleValue());
        }
    }

    private static List<BioRow> buildRows(BodyInfo body) {
        List<BioRow> rows = new ArrayList<>();

        // Map canonical display name -> candidate (for genus + score).
        Map<String, BioCandidate> candByKey = new HashMap<>();

        List<BioCandidate> preds = body.getPredictions();
        if (preds != null) {
            for (BioCandidate c : preds) {
                if (c == null) {
                    continue;
                }
                String dn = c.getDisplayName();
                if (dn == null || dn.isBlank()) {
                    continue;
                }
                rows.add(new BioRow(dn));
                candByKey.put(canonicalBioKey(dn), c);
            }
        }

        // Ensure observed display names appear even if not in predictions.
        if (body.getObservedBioDisplayNames() != null) {
            for (String observed : body.getObservedBioDisplayNames()) {
                if (observed == null || observed.isBlank()) {
                    continue;
                }

                boolean exists = false;
                String ok = canonicalBioKey(observed);
                for (BioRow r : rows) {
                    if (canonicalBioKey(r.displayName).equals(ok)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    rows.add(new BioRow(observed));
                }
            }
        }

        Map<String, Integer> counts = body.getBioSampleCountsSnapshot();
        Map<String, List<BodyInfo.BioSamplePoint>> points = body.getBioSamplePointsSnapshot();

        // Load counts/points.
        for (BioRow r : rows) {
            r.sampleCount = lookupCount(counts, r.displayName);

            List<BodyInfo.BioSamplePoint> pts = lookupPoints(points, r.displayName);
            if (pts != null && !pts.isEmpty()) {
                r.points = new ArrayList<>(pts);
            }

            r.genusKey = genusKeyForRow(r.displayName, candByKey.get(canonicalBioKey(r.displayName)));
        }

        // Collapse to ONE row per genus once that genus is known/started.
        collapseRowsByGenus(body, rows, candByKey);

        // Finished at top, then in-progress, then unstarted, then alpha.
        Collections.sort(rows, new Comparator<BioRow>() {
            @Override
            public int compare(BioRow a, BioRow b) {
                int ra = rank(a.sampleCount);
                int rb = rank(b.sampleCount);

                if (ra != rb) {
                    return Integer.compare(ra, rb);
                }
                return a.displayName.compareToIgnoreCase(b.displayName);
            }

            private int rank(int cnt) {
                if (cnt >= 3) {
                    return 0; // complete first
                }
                if (cnt == 1 || cnt == 2) {
                    return 1; // in-progress next
                }
                return 2;     // unstarted last
            }
        });

        return rows;
    }

    private static void collapseRowsByGenus(
            BodyInfo body,
            List<BioRow> rows,
            Map<String, BioCandidate> candByKey) {

        // Which genera are "known"?
        Set<String> knownGenus = new HashSet<>();

        // 1) observed genus prefixes (already lower in your cache)
        if (body.getObservedGenusPrefixes() != null) {
            for (String g : body.getObservedGenusPrefixes()) {
                if (g != null && !g.isBlank()) {
                    knownGenus.add(g.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        // 2) any started (sampleCount > 0) implies genus known
        for (BioRow r : rows) {
            if (r.sampleCount > 0 && r.genusKey != null && !r.genusKey.isBlank()) {
                knownGenus.add(r.genusKey);
            }
        }

        // 3) observed display names imply genus known
        if (body.getObservedBioDisplayNames() != null) {
            for (String s : body.getObservedBioDisplayNames()) {
                String g = genusFromDisplayName(s);
                if (g != null && !g.isBlank()) {
                    knownGenus.add(g);
                }
            }
        }

        // For each known genus, pick a single "winner" row; remove the rest.
        Map<String, BioRow> winnerByGenus = new HashMap<>();

        for (BioRow r : rows) {
            if (r.genusKey == null || r.genusKey.isBlank()) {
                continue;
            }
            if (!knownGenus.contains(r.genusKey)) {
                continue;
            }

            BioRow cur = winnerByGenus.get(r.genusKey);
            if (cur == null) {
                winnerByGenus.put(r.genusKey, r);
                continue;
            }

            // Prefer: higher sampleCount, then observed, then higher prediction score, then alpha.
            int cmp = Integer.compare(r.sampleCount, cur.sampleCount);
            if (cmp != 0) {
                if (cmp > 0) {
                    winnerByGenus.put(r.genusKey, r);
                }
                continue;
            }

            boolean rObs = isObservedDisplayName(body, r.displayName);
            boolean cObs = isObservedDisplayName(body, cur.displayName);
            if (rObs != cObs) {
                if (rObs) {
                    winnerByGenus.put(r.genusKey, r);
                }
                continue;
            }

            double rs = scoreFor(r, candByKey);
            double cs = scoreFor(cur, candByKey);
            if (rs != cs) {
                if (rs > cs) {
                    winnerByGenus.put(r.genusKey, r);
                }
                continue;
            }

            if (r.displayName.compareToIgnoreCase(cur.displayName) < 0) {
                winnerByGenus.put(r.genusKey, r);
            }
        }

        // Filter out non-winners for known genera.
        List<BioRow> keep = new ArrayList<>(rows.size());
        for (BioRow r : rows) {
            if (r.genusKey == null || r.genusKey.isBlank()) {
                keep.add(r);
                continue;
            }

            if (!knownGenus.contains(r.genusKey)) {
                keep.add(r);
                continue;
            }

            BioRow win = winnerByGenus.get(r.genusKey);
            if (win == r) {
                keep.add(r);
            }
        }

        rows.clear();
        rows.addAll(keep);
    }

    private static boolean isObservedDisplayName(BodyInfo body, String displayName) {
        if (body.getObservedBioDisplayNames() == null) {
            return false;
        }
        String want = canonicalBioKey(displayName);
        for (String s : body.getObservedBioDisplayNames()) {
            if (want.equals(canonicalBioKey(s))) {
                return true;
            }
        }
        return false;
    }

    private static double scoreFor(BioRow r, Map<String, BioCandidate> candByKey) {
        BioCandidate c = candByKey.get(canonicalBioKey(r.displayName));
        if (c == null) {
            return -1.0;
        }
        return c.getScore();
    }

    private static String genusKeyForRow(String displayName, BioCandidate c) {
        if (c != null && c.getGenus() != null && !c.getGenus().isBlank()) {
            return c.getGenus().trim().toLowerCase(Locale.ROOT);
        }
        return genusFromDisplayName(displayName);
    }

    private static String genusFromDisplayName(String displayName) {
        if (displayName == null) {
            return "";
        }
        String s = displayName.trim();
        if (s.isEmpty()) {
            return "";
        }
        int idx = s.indexOf(' ');
        String genus = (idx < 0) ? s : s.substring(0, idx);
        return genus.trim().toLowerCase(Locale.ROOT);
    }

    private void updateVoiceTransitions() {
        if (currentLat == null || currentLon == null || currentPlanetRadius == null) {
            return;
        }
        if (systemTab == null) {
            return;
        }
        if (currentBodyName == null || currentBodyName.isBlank()) {
            return;
        }

        SystemState state = systemTab.getState();
        if (state == null) {
            return;
        }

        BodyInfo body = findBodyByName(state, currentBodyName);
        if (body == null) {
            return;
        }

        Map<String, Integer> counts = body.getBioSampleCountsSnapshot();
        Map<String, List<BodyInfo.BioSamplePoint>> points = body.getBioSamplePointsSnapshot();

        if (counts == null || counts.isEmpty()) {
            return;
        }
        if (points == null || points.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String bioKey = e.getKey();
            if (bioKey == null || bioKey.isBlank()) {
                continue;
            }

            int count = 0;
            if (e.getValue() != null) {
                count = e.getValue().intValue();
            }

            if (count <= 0 || count >= 3) {
                continue;
            }

            List<BodyInfo.BioSamplePoint> pts = points.get(bioKey);
            if (pts == null || pts.isEmpty()) {
                continue;
            }

            BodyInfo.BioSamplePoint last = pts.get(pts.size() - 1);

            double distM = greatCircleMeters(
                    currentLat.doubleValue(),
                    currentLon.doubleValue(),
                    last.getLatitude(),
                    last.getLongitude(),
                    currentPlanetRadius.doubleValue());

            int needed = BioColonyDistance.metersForBio(bioKey);
            if (needed <= 0) {
                return;
            }

            boolean inside = distM < needed;
            Boolean prev = insideStateByBioKey.put(bioKey, Boolean.valueOf(inside));

            if (prev == null) {
                return;
            }
            if (prev.booleanValue() == inside) {
                return;
            }

            if (inside) {
                tts.speakf("Entering clonal colony range of {species} ({meters} meters)", bioKey, Integer.valueOf(needed));
            } else {
                tts.speakf("Leaving clonal colony range of {species} ({meters} meters)", bioKey, Integer.valueOf(needed));
            }
            return;
        }
    }

    private static int lookupCount(Map<String, Integer> counts, String displayName) {
        if (counts == null || counts.isEmpty()) {
            return 0;
        }
        if (displayName == null || displayName.isBlank()) {
            return 0;
        }

        Integer v = counts.get(displayName);
        if (v == null) {
            v = counts.get(canonicalBioKey(displayName));
        }
        if (v != null) {
            return v.intValue();
        }

        String want = canonicalBioKey(displayName);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (want.equals(canonicalBioKey(e.getKey()))) {
                if (e.getValue() != null) {
                    return e.getValue().intValue();
                }
                return 0;
            }
        }
        return 0;
    }

    private static List<BodyInfo.BioSamplePoint> lookupPoints(
            Map<String, List<BodyInfo.BioSamplePoint>> points,
            String displayName) {

        if (points == null || points.isEmpty()) {
            return null;
        }
        if (displayName == null || displayName.isBlank()) {
            return null;
        }

        List<BodyInfo.BioSamplePoint> v = points.get(displayName);
        if (v == null) {
            v = points.get(canonicalBioKey(displayName));
        }
        if (v != null) {
            return v;
        }

        String want = canonicalBioKey(displayName);
        for (Map.Entry<String, List<BodyInfo.BioSamplePoint>> e : points.entrySet()) {
            if (want.equals(canonicalBioKey(e.getKey()))) {
                return e.getValue();
            }
        }
        return null;
    }

    private static BodyInfo findBodyByName(SystemState state, String bodyName) {
        if (state.getBodies() == null || state.getBodies().isEmpty()) {
            return null;
        }
        for (BodyInfo b : state.getBodies().values()) {
            if (b == null || b.getBodyName() == null) {
                continue;
            }
            if (b.getBodyName().equalsIgnoreCase(bodyName)) {
                return b;
            }
        }
        return null;
    }

    private static String canonicalBioKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static double greatCircleMeters(double lat1Deg, double lon1Deg, double lat2Deg, double lon2Deg, double radiusM) {
        double lat1 = Math.toRadians(lat1Deg);
        double lon1 = Math.toRadians(lon1Deg);
        double lat2 = Math.toRadians(lat2Deg);
        double lon2 = Math.toRadians(lon2Deg);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a =
                Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return radiusM * c;
    }

    public void applyUiFontPreferences() {
        applyUiFont(OverlayPreferences.getUiFont());
    }

    public void applyUiFont(Font font) {
        if (font != null) {
            setFont(font);
            header.setFont(font);
            table.setFont(font);
            table.setRowHeight(Math.max(24, font.getSize() + 8));

            JTableHeader th = table.getTableHeader();
            if (th != null) {
                th.setFont(font);
            }
        }

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    private static void styleHeader(JTable table) {
        JTableHeader th = table.getTableHeader();
        if (th == null) {
            return;
        }

        th.setOpaque(true);
        th.setForeground(ED_ORANGE);
        th.setBackground(ED_DARK);

        th.setDefaultRenderer(new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
                label.setOpaque(true);
                label.setBackground(ED_DARK);
                label.setForeground(ED_ORANGE);
                label.setBorder(new EmptyBorder(0, 6, 0, 6));
                return label;
            }
        });
    }

    private static Color colorForSamples(int samples) {
        if (samples >= 3) {
            return Color.GREEN;
        }
        if (samples > 0) {
            return Color.YELLOW;
        }
        return ED_ORANGE;
    }

    // ------------------------------------------------------------
    // Table model + rendering
    // ------------------------------------------------------------

    private static final class BioRow {
        private final String displayName;
        private int sampleCount;
        private String genusKey = "";
        private List<BodyInfo.BioSamplePoint> points = Collections.emptyList();
        private final List<Double> distancesM = new ArrayList<>();

        private BioRow(String displayName) {
            this.displayName = displayName;
        }

        private void recomputeDistances(double curLat, double curLon, double radiusM) {
            distancesM.clear();

            if (points == null || points.isEmpty()) {
                return;
            }

            for (BodyInfo.BioSamplePoint p : points) {
                if (p == null) {
                    continue;
                }

                distancesM.add(Double.valueOf(greatCircleMeters(curLat, curLon, p.getLatitude(), p.getLongitude(), radiusM)));
            }
        }
    }

    private static final class BioTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] cols = { "Bio", "Count", "Samples" };
        private final List<BioRow> rows = new ArrayList<>();

        void setRows(List<BioRow> newRows) {
            rows.clear();

            if (newRows != null) {
                rows.addAll(newRows);
            }

            fireTableDataChanged();
        }

        BioRow getRowAt(int row) {
            if (row < 0 || row >= rows.size()) {
                return null;
            }
            return rows.get(row);
        }

        void updateDistances(double curLat, double curLon, double radiusM) {
            for (BioRow r : rows) {
                r.recomputeDistances(curLat, curLon, radiusM);
            }

            if (!rows.isEmpty()) {
                fireTableRowsUpdated(0, rows.size() - 1);
            }
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int col) {
            return cols[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BioRow r = rows.get(rowIndex);

            if (columnIndex == 0) {
                return r.displayName;
            }
            if (columnIndex == 1) {
                if (r.sampleCount <= 0) {
                    return "";
                }
                return r.sampleCount + "/3";
            }
            if (columnIndex == 2) {
                return r;
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private static final class BioTextCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        private final BioTableModel model;

        private BioTextCellRenderer(BioTableModel model) {
            this.model = model;
            setOpaque(false);
            setForeground(ED_ORANGE);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                label.setOpaque(false);
            } else {
                label.setOpaque(true);
            }

            BioRow r = model.getRowAt(row);
            if (r != null) {
                if (column == 0 || column == 1) {
                    label.setForeground(colorForSamples(r.sampleCount));
                } else {
                    label.setForeground(ED_ORANGE);
                }
            } else {
                label.setForeground(ED_ORANGE);
            }

            label.setBorder(new EmptyBorder(0, 6, 0, 6));
            return label;
        }
    }

    private static final class SamplePillsRenderer implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            if (!(value instanceof BioRow)) {
                DefaultTableCellRenderer fallback = new DefaultTableCellRenderer();
                fallback.setOpaque(false);
                fallback.setForeground(ED_ORANGE);
                return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

            BioRow r = (BioRow) value;
            return new SamplePillsComponent(r, table.getFont());
        }
    }

    private static final class SamplePillsComponent extends JPanel {

        private static final long serialVersionUID = 1L;

        private final BioRow row;
        private final Font font;

        SamplePillsComponent(BioRow row, Font font) {
            this.row = row;
            this.font = font;
            setOpaque(false);
            setPreferredSize(new Dimension(320, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(font);

                FontMetrics fm = g2.getFontMetrics();

                int x = 8;
                int yMid = getHeight() / 2;

                int h = 16;
                int y = yMid - (h / 2);
                int gap = 8;

                int have = Math.min(3, Math.max(0, row.sampleCount));
                int slots = 3;

                for (int i = 0; i < slots; i++) {
                    boolean filled = i < have;

                    String txt = null;
                    if (row.distancesM != null && i < row.distancesM.size()) {
                        txt = formatMetersShort(row.distancesM.get(i).doubleValue());
                    }

                    int w;
                    if (txt != null) {
                        w = fm.stringWidth(txt) + 14; // padding
                        if (w < 22) {
                            w = 22;
                        }
                    } else {
                        w = 18; // small empty pill
                    }

                    // Outline color always indicates status.
                    Color c = colorForSamples(row.sampleCount);
                    g2.setColor(c);
                    g2.drawRoundRect(x, y, w, h, h, h);

                    if (filled) {
                        g2.setColor(c);
                        g2.fillRoundRect(x + 1, y + 1, w - 1, h - 1, h, h);
                    }

                    if (txt != null && filled) {
                        g2.setColor(TEXT_BLACK);
                        int tx = x + (w - fm.stringWidth(txt)) / 2;
                        int ty = yMid + (fm.getAscent() / 2) - 1;
                        g2.drawString(txt, tx, ty);
                    }

                    x += w + gap;
                }

            } finally {
                g2.dispose();
            }
        }

        private static String formatMetersShort(double m) {
            if (Double.isNaN(m) || Double.isInfinite(m)) {
                return "";
            }
            if (m < 1000.0) {
                return String.format(Locale.US, "%.0fm", m);
            }
            return String.format(Locale.US, "%.2fkm", (m / 1000.0));
        }
    }
}
