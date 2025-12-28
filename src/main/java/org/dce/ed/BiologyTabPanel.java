package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
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

/**
 * Biology tab – on-surface helper.
 *
 * Populates from cached BodyInfo predictions + observed biology and shows up to 0–3
 * sample-point distance indicators per species.
 *
 * Position comes from StatusEvent (Status.json).
 * Sample points are persisted in cache (BodyInfo -> CachedBody -> SystemCache).
 */
public class BiologyTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

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

    // Tracks last inside/outside state per species key so we can voice transitions.
    private final Map<String, Boolean> insideStateByBioKey = new HashMap<>();

    public BiologyTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        table.setOpaque(false);
        table.setDefaultRenderer(Object.class, new TransparentCellRenderer());
        table.setDefaultRenderer(String.class, new TransparentCellRenderer());
        table.setDefaultRenderer(Integer.class, new TransparentCellRenderer());
        
        header.setOpaque(false);
        add(header, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setShowGrid(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(260); // Bio
        table.getColumnModel().getColumn(1).setPreferredWidth(70);  // Count
        table.getColumnModel().getColumn(2).setPreferredWidth(320); // Samples

        table.getColumnModel().getColumn(2).setCellRenderer(new SampleDotsRenderer());

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

        // If prediction or scan events arrive, refresh the current body view.
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

        List<BioCandidate> preds = body.getPredictions();
        if (preds != null) {
            for (BioCandidate c : preds) {
                if (c == null || c.getDisplayName() == null || c.getDisplayName().isBlank()) {
                    continue;
                }
                rows.add(new BioRow(c.getDisplayName()));
            }
        }

        // Ensure observed display names appear even if not in predictions.
        if (body.getObservedBioDisplayNames() != null) {
            for (String observed : body.getObservedBioDisplayNames()) {
                if (observed == null || observed.isBlank()) {
                    continue;
                }
                boolean exists = false;
                for (BioRow r : rows) {
                    if (canonicalBioKey(r.displayName).equals(canonicalBioKey(observed))) {
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
            String key = canonicalBioKey(r.displayName);

            int cnt = 0;
            if (counts != null) {
                // First try exact displayName (matches cache JSON keys like "Fumerola Nitris")
                Integer v = counts.get(r.displayName);

                // Then try canonical lowercase (matches runtime keys if you normalize there)
                if (v == null) {
                    v = counts.get(canonicalBioKey(r.displayName));
                }

                // Last-chance: case-insensitive match (covers any mixed state)
                if (v == null) {
                    String want = canonicalBioKey(r.displayName);
                    for (Map.Entry<String, Integer> e : counts.entrySet()) {
                        if (want.equals(canonicalBioKey(e.getKey()))) {
                            v = e.getValue();
                            break;
                        }
                    }
                }

                if (v != null) {
                    cnt = v.intValue();
                }
            }
            r.sampleCount = cnt;


            if (points != null) {
                List<BodyInfo.BioSamplePoint> pts = points.get(key);
                if (pts != null && !pts.isEmpty()) {
                    r.points = new ArrayList<>(pts);
                }
            }
        }

        // Sort: in-progress first, then 0/3, then complete, then alpha.
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
                if (cnt == 1 || cnt == 2) return 0;
                if (cnt == 0) return 1;
                return 2; // 3+
            }
        });

        return rows;
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

        if (counts == null || counts.isEmpty() || points == null || points.isEmpty()) {
            return;
        }

        // Choose first in-progress with at least one point.
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String bioKey = e.getKey();
            if (bioKey == null || bioKey.isBlank()) {
                continue;
            }

            int count = (e.getValue() == null) ? 0 : e.getValue().intValue();
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

            if (prev == null || prev.booleanValue() == inside) {
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
            table.getTableHeader().setFont(font);
        }
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    // ------------------------------------------------------------
    // Table model + rendering
    // ------------------------------------------------------------

    private static final class BioRow {
        private final String displayName;
        private int sampleCount;
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
            switch (columnIndex) {
                case 0:
                    return r.displayName;
                case 1:
                    return r.sampleCount <= 0 ? "" : (r.sampleCount + "/3");
                case 2:
                    return r;
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private static final class SampleDotsRenderer implements TableCellRenderer {

        private final DefaultTableCellRenderer fallback = new DefaultTableCellRenderer();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!(value instanceof BioRow)) {
                return fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
            BioRow r = (BioRow) value;
            return new SampleDotsComponent(r, table.getFont());
        }
    }

    private static final class SampleDotsComponent extends JPanel {

        private static final long serialVersionUID = 1L;

        private final BioRow row;
        private final Font font;

        SampleDotsComponent(BioRow row, Font font) {
            this.row = row;
            this.font = font;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(font);

                int x = 8;
                int yMid = getHeight() / 2;
                int radius = 7;
                int gap = 10;

                int have = Math.min(3, Math.max(0, row.sampleCount));
                int dots = 3;

                for (int i = 0; i < dots; i++) {
                    int cx = x + i * (radius * 2 + gap);
                    int cy = yMid;

                    g2.drawOval(cx, cy - radius, radius * 2, radius * 2);

                    if (i < have) {
                        g2.fillOval(cx + 2, cy - radius + 2, (radius * 2) - 3, (radius * 2) - 3);
                    }

                    if (row.distancesM != null && i < row.distancesM.size()) {
                        double m = row.distancesM.get(i).doubleValue();
                        String txt = formatMeters(m);

                        int tx = cx + (radius * 2) + 6;
                        int ty = cy + (font.getSize() / 2) - 1;
                        g2.drawString(txt, tx, ty);
                    }
                }
            } finally {
                g2.dispose();
            }
        }

        private static String formatMeters(double m) {
            if (Double.isNaN(m) || Double.isInfinite(m)) {
                return "";
            }
            if (m < 1000.0) {
                return String.format(Locale.US, "%.0f m", m);
            }
            return String.format(Locale.US, "%.2f km", (m / 1000.0));
        }
    }
    private static final class TransparentCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Keep selection highlighting readable, but otherwise allow overlay alpha.
            if (c instanceof DefaultTableCellRenderer) {
                DefaultTableCellRenderer r = (DefaultTableCellRenderer) c;

                if (isSelected) {
                    r.setOpaque(true);
                } else {
                    r.setOpaque(false);
                }
            }
            
            return c;
        }
    }

}
