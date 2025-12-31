package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
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
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import java.awt.BasicStroke;

public class BiologyTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color ED_ORANGE = new Color(255, 140, 0);
    private static final Color ED_DARK = new Color(0, 0, 0);
    private static final Color TEXT_BLACK = Color.BLACK;

    private final JLabel header = new JLabel("Biology");
    private final BioTableModel model = new BioTableModel();
    private final JTable table = new JTable(model);
    private final JScrollPane scroll = new JScrollPane(table);

    private final BioMapPanel mapPanel = new BioMapPanel();

    private final CenterLayoutPanel center = new CenterLayoutPanel();
private static final int REQUIRED_SAMPLES = 3;

private static final class MovementSample {
    private final Instant t;
    private final double lat;
    private final double lon;
    private final double radiusM;

    private MovementSample(Instant t, double lat, double lon, double radiusM) {
        this.t = t;
        this.lat = lat;
        this.lon = lon;
        this.radiusM = radiusM;
    }
}

private final Deque<MovementSample> movement = new ArrayDeque<>();
private Double movementHeadingDeg; // 0=N, clockwise. null until we have enough movement.


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

        // Columns: Bio | Count | Min (m) | Samples
        table.getColumnModel().getColumn(0).setPreferredWidth(260); // Bio
        table.getColumnModel().getColumn(1).setPreferredWidth(70);  // Count
        table.getColumnModel().getColumn(2).setPreferredWidth(90);  // Min (m)
        table.getColumnModel().getColumn(3).setPreferredWidth(340); // Samples

        table.getColumnModel().getColumn(3).setCellRenderer(new SamplePillsRenderer());

        styleHeader(table);

        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        mapPanel.setOpaque(false);

        center.setOpaque(false);
        center.setTableAndMap(scroll, mapPanel);
        add(center, BorderLayout.CENTER);
setPreferredSize(new Dimension(560, 320));
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

            Double newLat = e.getLatitude();
            Double newLon = e.getLongitude();
            Double newRadius = e.getPlanetRadius();
            String newBodyName = e.getBodyName();

            boolean bodyChanged = newBodyName != null && !newBodyName.isBlank() && !newBodyName.equals(currentBodyName);

            currentLat = newLat;
            currentLon = newLon;
            currentPlanetRadius = newRadius;
            currentBodyName = newBodyName;


if (currentLat != null && currentLon != null && currentPlanetRadius != null) {
    recordMovementSample(e.getTimestamp(), currentLat.doubleValue(), currentLon.doubleValue(), currentPlanetRadius.doubleValue());

    double hdg = movementHeadingDeg == null ? 0.0 : movementHeadingDeg.doubleValue();
    mapPanel.setShipHeadingDeg(hdg);
    mapPanel.setShipLatLon(currentLat.doubleValue(), currentLon.doubleValue(), currentPlanetRadius.doubleValue());
}


            // Only rebuild rows when the body changes; otherwise just update distances/heading/map.
            if (bodyChanged) {
                refreshTableForCurrentBody();
            } else if (currentLat != null && currentLon != null && currentPlanetRadius != null) {
                model.updateDistances(currentLat.doubleValue(), currentLon.doubleValue(), currentPlanetRadius.doubleValue());
                table.repaint();
            }

            updateVoiceTransitions();
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

        center.updateFixedTableHeight(table, model);
        if (currentLat != null && currentLon != null && currentPlanetRadius != null) {
            model.updateDistances(currentLat.doubleValue(), currentLon.doubleValue(), currentPlanetRadius.doubleValue());
        }
    }

    
    private static final class CenterLayoutPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private JScrollPane scroll;
        private BioMapPanel map;

        private int fixedTableHeightPx = 0;

        CenterLayoutPanel() {
            super(null);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setOpaque(false);
        }

        void setTableAndMap(JScrollPane scroll, BioMapPanel map) {
            this.scroll = scroll;
            this.map = map;

            removeAll();
            if (scroll != null) {
                add(scroll);
            }
            if (map != null) {
                add(map);
            }
        }

        void updateFixedTableHeight(JTable table, BioTableModel model) {
            if (table == null || model == null) {
                return;
            }

            JTableHeader th = table.getTableHeader();
            int headerH = (th == null) ? 0 : th.getPreferredSize().height;

            // minimum of 6 rows, but never shrink once we've seen more
            int rows = Math.max(6, model.getMaxRowsSeen());
            int rowsH = table.getRowHeight() * Math.max(1, rows);

            fixedTableHeightPx = headerH + rowsH;
            revalidate();
        }

        @Override
        public void doLayout() {
            Insets in = getInsets();
            int w = Math.max(0, getWidth() - in.left - in.right);
            int h = Math.max(0, getHeight() - in.top - in.bottom);

            int y = in.top;

            // Reserve space for the map (about 80% of the available square).
            int minMapPx = 80; // keep it from collapsing to 0
            int availableSquare = Math.min(w, h);
            int desiredMap = (int) Math.round(availableSquare * 0.8);
            desiredMap = Math.max(minMapPx, desiredMap);

            int maxTableH = Math.max(0, h - desiredMap);
            int tableH = Math.min(maxTableH, Math.max(0, fixedTableHeightPx));

            if (scroll != null) {
                scroll.setBounds(in.left, y, w, tableH);
            }
            y += tableH;

            int remainingH = Math.max(0, h - tableH);

            int square = Math.min(w, remainingH);
            int mapSize = (int) Math.round(square * 0.8);
            mapSize = Math.max(0, Math.min(mapSize, square));

            if (map != null) {
                int x = in.left + Math.max(0, (w - mapSize) / 2);
                int yMap = y + Math.max(0, (remainingH - mapSize) / 2);
                map.setBounds(x, yMap, mapSize, mapSize);
            }
        }
    }

private static List<BioRow> buildRows(BodyInfo body) {
        List<BioRow> rows = new ArrayList<>();

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

        // Ensure observed entries appear even if they weren't predicted
        if (body.getObservedBioDisplayNames() != null) {
            for (String observed : body.getObservedBioDisplayNames()) {
                if (observed == null || observed.isBlank()) {
                    continue;
                }

                String ok = canonicalBioKey(observed);
                boolean exists = false;
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

        for (BioRow r : rows) {

            // Elite only tracks ONE active genus at a time; snapshot counts can reflect only that active genus.
            // If we have recorded sample points for this row, those are ground truth for progress / bubble filling.
            List<BodyInfo.BioSamplePoint> pts = lookupPoints(points, r.displayName);
            int ptsCount = (pts == null) ? 0 : pts.size();
            int countFromSnapshot = lookupCount(counts, r.displayName);

            if (ptsCount > 0) {
                r.points = new ArrayList<>(pts);

                // If the player finished (Analyse) but we didn't record a 3rd point (common),
                // let the snapshot promote completion, otherwise drive progress from points.
                if (countFromSnapshot >= REQUIRED_SAMPLES) {
                    r.sampleCount = REQUIRED_SAMPLES;
                } else {
                    r.sampleCount = Math.min(ptsCount, REQUIRED_SAMPLES);
                }
            } else {
                r.sampleCount = Math.min(Math.max(countFromSnapshot, 0), REQUIRED_SAMPLES);
            }

            BioCandidate cand = candByKey.get(canonicalBioKey(r.displayName));
            r.genusKey = genusKeyForRow(r.displayName, cand);
            r.requiredMeters = BioColonyDistance.metersForBio(r.genusKey);
        }

        collapseRowsByGenus(body, rows, candByKey);

        // Complete first, then in-progress, then unstarted
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
                if (cnt >= REQUIRED_SAMPLES) {
                    return 0;
                }
                if (cnt > 0) {
                    return 1;
                }
                return 2;
            }
        });

        return rows;
    }

    private static void collapseRowsByGenus(
            BodyInfo body,
            List<BioRow> rows,
            Map<String, BioCandidate> candByKey) {

        Set<String> knownGenus = new HashSet<>();

        if (body.getObservedGenusPrefixes() != null) {
            for (String g : body.getObservedGenusPrefixes()) {
                if (g != null && !g.isBlank()) {
                    knownGenus.add(g.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        for (BioRow r : rows) {
            if (r.sampleCount > 0 && r.genusKey != null && !r.genusKey.isBlank()) {
                knownGenus.add(r.genusKey);
            }
        }

        if (body.getObservedBioDisplayNames() != null) {
            for (String s : body.getObservedBioDisplayNames()) {
                String g = genusFromDisplayName(s);
                if (g != null && !g.isBlank()) {
                    knownGenus.add(g);
                }
            }
        }

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
                return (e.getValue() == null) ? 0 : e.getValue().intValue();
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

    private static String formatMetersFixed(double meters) {
        if (Double.isNaN(meters) || Double.isInfinite(meters)) {
            return "";
        }

        if (meters < 1000.0) {
            return String.format(java.util.Locale.ROOT, "%.0fm", meters);
        }

        double km = meters / 1000.0;
        // nearest tenth
        km = Math.round(km * 10.0) / 10.0;
        return String.format(java.util.Locale.ROOT, "%.1fkm", km);
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
        private int requiredMeters;
        private List<BodyInfo.BioSamplePoint> points = Collections.emptyList();
        private final List<Double> distancesM = new ArrayList<>();

        private BioRow(String displayName) {
            this.displayName = displayName;
        }

        private void recomputeDistances(double curLat, double curLon, double radiusM) {
            distancesM.clear();

            // Complete: don’t track distances anymore
            if (sampleCount >= 3) {
                return;
            }

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

        private final String[] cols = { "Bio", "Count", "Min (m)", "Samples" };
        private final List<BioRow> rows = new ArrayList<>();

        // Track the maximum row count we've ever shown so the table height doesn't shrink.
        private int maxRowsSeen = 0;

        void setRows(List<BioRow> newRows) {
            rows.clear();

            if (newRows != null) {
                rows.addAll(newRows);
            }

            if (rows.size() > maxRowsSeen) {
                maxRowsSeen = rows.size();
            }

            fireTableDataChanged();
        }

        int getMaxRowsSeen() {
            return Math.max(maxRowsSeen, rows.size());
        }

        BioRow getRowAt(int row) {
            if (row < 0 || row >= rows.size()) {
                return null;
            }
            return rows.get(row);
        }

        List<BioRow> getRowsSnapshot() {
            return new ArrayList<>(rows);
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
                if (r.requiredMeters <= 0) {
                    return "";
                }
                return Integer.valueOf(r.requiredMeters);
            }
            if (columnIndex == 3) {
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
                // Color Bio, Count, Min columns by status.
                if (column == 0 || column == 1 || column == 2) {
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
            setPreferredSize(new Dimension(340, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(font);

                FontMetrics fm = g2.getFontMetrics();

                // Fixed width bubble sized for up to "999m" (and "9.9km" if wider)
                int bubbleW = Math.max(fm.stringWidth("999m"), fm.stringWidth("9.9km")) + 14;
                int bubbleH = 16;

                int x = 8;
                int yMid = getHeight() / 2;
                int y = yMid - (bubbleH / 2);
                int gap = 8;

                Color c = colorForSamples(row.sampleCount);

                // Complete: show checkmarks only and no distances.
                if (row.sampleCount >= 3) {
                    g2.setColor(c);

                    String check = "\u2713";
                    int checkW = fm.stringWidth(check);

                    for (int i = 0; i < 3; i++) {
                        int tx = x + (bubbleW - checkW) / 2;
                        int ty = yMid + (fm.getAscent() / 2) - 1;
                        g2.drawString(check, tx, ty);
                        x += bubbleW + gap;
                    }
                    return;
                }

                int have = Math.min(3, Math.max(0, row.sampleCount));
                int slots = 3;

                for (int i = 0; i < slots; i++) {
                    boolean filled = i < have;

                    String txt = null;
                    if (row.distancesM != null && i < row.distancesM.size()) {
                        txt = formatMetersFixed(row.distancesM.get(i).doubleValue());
                    }

                    g2.setColor(c);
                    g2.drawRoundRect(x, y, bubbleW, bubbleH, bubbleH, bubbleH);

                    if (filled) {
                        g2.setColor(c);
                        g2.fillRoundRect(x + 1, y + 1, bubbleW - 1, bubbleH - 1, bubbleH, bubbleH);
                    }

                    if (txt != null && !txt.isBlank()) {
                        g2.setColor(TEXT_BLACK);
                        int tx = x + (bubbleW - fm.stringWidth(txt)) / 2;
                        int ty = yMid + (fm.getAscent() / 2) - 1;
                        g2.drawString(txt, tx, ty);
                    }

                    x += bubbleW + gap;
                }

            } finally {
                g2.dispose();
            }
        }

        // Fixed formatting:
        // - meters: integer up to 999m display
        // - km: nearest tenth
        private static String formatMetersFixed(double m) {
            if (Double.isNaN(m) || Double.isInfinite(m)) {
                return "";
            }

            if (m < 1000.0) {
                long mm = Math.round(m);
                if (mm > 999) {
                    mm = 999;
                }
                return String.format(Locale.US, "%dm", Long.valueOf(mm));
            }

            double km = m / 1000.0;
            // nearest tenth: String.format rounds
            return String.format(Locale.US, "%.1fkm", Double.valueOf(km));
        }
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

        // Build the same rows the UI uses so we pick the same “active” in-progress item.
        List<BioRow> rows = buildRows(body);
        if (rows == null || rows.isEmpty()) {
            return;
        }

        // Find the first in-progress row (1/3 or 2/3) that has at least one recorded sample point.
        BioRow active = null;
        for (BioRow r : rows) {
            if (r == null) {
                continue;
            }
            if (r.sampleCount <= 0 || r.sampleCount >= 3) {
                continue;
            }
            if (r.points == null || r.points.isEmpty()) {
                continue;
            }
            active = r;
            break;
        }

        if (active == null) {
            return;
        }

        int needed = active.requiredMeters;
        if (needed <= 0) {
            // Fall back to lookup by genus if the row didn’t get it for some reason.
            needed = BioColonyDistance.metersForBio(active.genusKey);
        }
        if (needed <= 0) {
            return;
        }

        BodyInfo.BioSamplePoint last = active.points.get(active.points.size() - 1);

        double distM = greatCircleMeters(
                currentLat.doubleValue(),
                currentLon.doubleValue(),
                last.getLatitude(),
                last.getLongitude(),
                currentPlanetRadius.doubleValue());

        boolean inside = distM < needed;

        // Use canonical key to avoid duplicate state for casing differences.
        String bioKey = canonicalBioKey(active.displayName);

        Boolean prev = insideStateByBioKey.put(bioKey, Boolean.valueOf(inside));
        if (prev == null) {
            // First time we’ve evaluated this target; don’t speak.
            return;
        }

        if (prev.booleanValue() == inside) {
            return;
        }

        // Announce transition. Replaceables wrapped in {} for caching.
        if (inside) {
            tts.speakf("Entering clonal colony range of {species}. Minimum {meters} meters.",
                    active.displayName,
                    Integer.valueOf(needed));
        } else {
            tts.speakf("Leaving clonal colony range of {species}. Minimum {meters} meters.",
                    active.displayName,
                    Integer.valueOf(needed));
        }
    }



private final class BioMapPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private double shipLat;
    private double shipLon;
    private double shipRadiusM;
    private boolean haveShip;

    private double shipHeadingDeg; // 0=N, clockwise. "Up" on map.

    private BioMapPanel() {
        setOpaque(false);
    }

    private void setShipLatLon(double lat, double lon, double radiusM) {
        this.shipLat = lat;
        this.shipLon = lon;
        this.shipRadiusM = radiusM;
        this.haveShip = true;
        repaint();
    }

    private void setShipHeadingDeg(double headingDeg) {
        this.shipHeadingDeg = headingDeg;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int side = Math.min(w, h);
            int x0 = (w - side) / 2;
            int y0 = (h - side) / 2;

            // Border / background
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRect(x0, y0, side, side);
            g2.setColor(new Color(255, 140, 0, 140));
            g2.drawRect(x0, y0, side - 1, side - 1);

            if (!haveShip) {
                g2.setColor(Color.WHITE);
                String msg = "No position";
                FontMetrics fm = g2.getFontMetrics();
                int tx = x0 + (side - fm.stringWidth(msg)) / 2;
                int ty = y0 + (side + fm.getAscent()) / 2;
                g2.drawString(msg, tx, ty);
                return;
            }

            int cx = x0 + side / 2;
            int cy = y0 + side / 2;

            // Draw ship marker
            int r = 6;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            g2.setColor(Color.BLACK);
            g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));

            // Collect target points: use all rows' recorded sample points (incomplete only)
            java.util.List<BioRow> rows = model.getRowsSnapshot();
            if (rows == null || rows.isEmpty()) {
                g2.setColor(Color.WHITE);
                String msg = "No specimens detected";
                FontMetrics fm = g2.getFontMetrics();
                int tx = x0 + (side - fm.stringWidth(msg)) / 2;
                int ty = y0 + (side + fm.getAscent()) / 2;
                g2.drawString(msg, tx, ty);
            } else {
                double maxDistM = 1.0;
                for (BioRow row : rows) {
                    if (row == null || row.sampleCount >= REQUIRED_SAMPLES || row.points == null) {
                        continue;
                    }
                    for (BodyInfo.BioSamplePoint p : row.points) {
                        if (p == null) {
                            continue;
                        }
                        double d = greatCircleMeters(shipLat, shipLon, p.getLatitude(), p.getLongitude(), shipRadiusM);
                        if (d > maxDistM) {
                            maxDistM = d;
                        }
                    }
                }
                // leave margin so labels fit
                double scale = (side * 0.42) / maxDistM;

                for (BioRow row : rows) {
                    if (row == null || row.sampleCount >= REQUIRED_SAMPLES || row.points == null || row.points.isEmpty()) {
                        continue;
                    }
                    for (BodyInfo.BioSamplePoint p : row.points) {
                        double d = greatCircleMeters(shipLat, shipLon, p.getLatitude(), p.getLongitude(), shipRadiusM);
                        double brng = bearingDeg(shipLat, shipLon, p.getLatitude(), p.getLongitude());
                        double rel = Math.toRadians(brng - shipHeadingDeg);

                        double dx = Math.sin(rel) * d;
                        double dy = -Math.cos(rel) * d;

                        int tx = cx + (int) Math.round(dx * scale);
                        int ty = cy + (int) Math.round(dy * scale);

                        g2.setStroke(new BasicStroke(2f));
                        g2.setColor(new Color(255, 140, 0, 220));
                        g2.draw(new Line2D.Double(cx, cy, tx, ty));

                        // distance label at midpoint
                        String label = BiologyTabPanel.formatMetersFixed(d);
                        FontMetrics fm = g2.getFontMetrics();
                        int mx = (cx + tx) / 2;
                        int my = (cy + ty) / 2;
                        g2.setColor(Color.WHITE);
                        g2.drawString(label, mx - fm.stringWidth(label) / 2, my - 2);
                    }
                }
            }

            // Compass (upper right)
            int pad = 10;
            int compR = 22;
            int compCx = x0 + side - pad - compR;
            int compCy = y0 + pad + compR;

            g2.setColor(new Color(0, 0, 0, 140));
            g2.fill(new Ellipse2D.Double(compCx - compR, compCy - compR, compR * 2, compR * 2));
            g2.setColor(new Color(255, 255, 255, 200));
            g2.draw(new Ellipse2D.Double(compCx - compR, compCy - compR, compR * 2, compR * 2));

            double relN = Math.toRadians(0.0 - shipHeadingDeg);
            double nx = Math.sin(relN);
            double ny = -Math.cos(relN);

            int nx2 = compCx + (int) Math.round(nx * (compR - 4));
            int ny2 = compCy + (int) Math.round(ny * (compR - 4));

            g2.draw(new Line2D.Double(compCx, compCy, nx2, ny2));
            g2.drawString("N", nx2 - 4, ny2 - 2);
        } finally {
            g2.dispose();
        }
    }
}

private void recordMovementSample(Instant t, double lat, double lon, double radiusM) {
    if (t == null) {
        return;
    }

    MovementSample last = movement.peekLast();
    if (last != null) {
        double d = greatCircleMeters(last.lat, last.lon, lat, lon, radiusM);
        // Ignore tiny jitter; do not let "stopped" updates influence heading.
        if (d < 2.0) {
            return;
        }
    }

    movement.addLast(new MovementSample(t, lat, lon, radiusM));
    while (movement.size() > 3) {
        movement.removeFirst();
    }

    if (movement.size() >= 2) {
        MovementSample a = movement.peekFirst();
        MovementSample b = movement.peekLast();
        if (a != null && b != null) {
            movementHeadingDeg = bearingDeg(a.lat, a.lon, b.lat, b.lon);
        }
    }
}

private static double bearingDeg(double lat1, double lon1, double lat2, double lon2) {
    double phi1 = Math.toRadians(lat1);
    double phi2 = Math.toRadians(lat2);
    double dLambda = Math.toRadians(lon2 - lon1);

    double y = Math.sin(dLambda) * Math.cos(phi2);
    double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLambda);

    double theta = Math.atan2(y, x);
    double deg = Math.toDegrees(theta);
    deg = (deg + 360.0) % 360.0;
    return deg;
}

}