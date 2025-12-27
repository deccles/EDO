package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.event.BioScanPredictionEvent;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.logreader.event.ScanOrganicEvent;
import org.dce.ed.logreader.event.StatusEvent;
import org.dce.ed.tts.PollyTtsCached;
import org.dce.ed.tts.TtsSprintf;

/**
 * Biology tab - used on the planetary surface to track sample distances.
 *
 * - Shows predicted/known biology for the current body.
 * - Tracks up to 3 sample points per species (from ScanOrganic + current Status lat/long).
 * - Draws 0-3 circles with distance-to-sample (like Exploration Buddy).
 * - Speaks when entering/leaving the required min-distance boundary for the active species.
 */
public class BiologyTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color ED_ORANGE = new Color(255, 140, 0);
    private static final Color ED_ORANGE_TRANS = new Color(255, 140, 0, 64);

    private Font uiFont = OverlayPreferences.getUiFont();

    private final JLabel header;
    private final JTable table;
    private final BiologyTableModel tableModel;

    // Current context (from Location/FSD/Status)
    private volatile String currentSystemName;
    private volatile long currentSystemAddress;
    private volatile String currentBodyName;
    private volatile Double currentLat;
    private volatile Double currentLon;
    private volatile Double currentPlanetRadiusM;
    private volatile boolean onFootOrSrv;

    // Predictions: bodyId -> candidates (for current system)
    private final Map<Integer, List<BioCandidate>> predictionsByBodyId = new HashMap<>();

    // Observations (scanned organics): bodyId -> genusLower -> observedDisplayName
    private final Map<Integer, Map<String, String>> observedByBodyIdAndGenus = new HashMap<>();

    // Sample points: bodyId -> speciesKey -> points
    private final Map<Integer, Map<String, List<SamplePoint>>> samplePointsByBodyId = new HashMap<>();

    // Last Status (for ScanOrganic -> point capture)
    private volatile StatusEvent lastStatus;

    // Active (incomplete) species tracking for voice boundary
    private volatile String activeSpeciesKey;
    private volatile String activeSpeciesGenus;
    private volatile boolean wasInsideBoundary;

    public BiologyTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        header = new JLabel("Waiting for surface biology...");
        header.setOpaque(false);
        header.setForeground(ED_ORANGE);
        header.setBorder(new EmptyBorder(4, 8, 4, 8));
        header.setFont(uiFont.deriveFont(Font.BOLD));

        tableModel = new BiologyTableModel();
        table = new BiologyTable(tableModel);
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setFont(uiFont);
        table.setRowHeight(26);

        JTableHeader th = table.getTableHeader();
        th.setOpaque(true);
        th.setForeground(ED_ORANGE);
        th.setBackground(Color.BLACK);
        th.setFont(uiFont.deriveFont(Font.BOLD));
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
                l.setOpaque(true);
                l.setBackground(Color.BLACK);
                l.setForeground(ED_ORANGE);
                l.setFont(uiFont.deriveFont(Font.BOLD));
                l.setBorder(new EmptyBorder(0, 6, 0, 6));
                return l;
            }
        });

        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            {
                setOpaque(false);
                setForeground(ED_ORANGE);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    l.setForeground(Color.BLACK);
                } else {
                    l.setForeground(ED_ORANGE);
                }

                l.setBorder(new EmptyBorder(0, 6, 0, 6));
                return l;
            }
        };

        table.setDefaultRenderer(String.class, textRenderer);
        table.getColumnModel().getColumn(BiologyTableModel.COL_SAMPLES).setCellRenderer(new SamplesCellRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------------
    // Event handling
    // ---------------------------------------------------------------------

    public void handleLogEvent(EliteLogEvent event) {
        if (event == null) {
            return;
        }

        if (event instanceof FsdJumpEvent) {
            FsdJumpEvent e = (FsdJumpEvent) event;

            currentSystemName = e.getStarSystem();
            currentSystemAddress = e.getSystemAddress();

            clearSurfaceState();

            requestRebuild();
            return;
        }

        if (event instanceof LocationEvent) {
            LocationEvent e = (LocationEvent) event;

            currentSystemName = e.getStarSystem();
            currentSystemAddress = e.getSystemAddress();
            currentBodyName = e.getBody();

            requestRebuild();
            return;
        }

        if (event instanceof BioScanPredictionEvent) {
            BioScanPredictionEvent e = (BioScanPredictionEvent) event;

            predictionsByBodyId.put(e.getBodyId(), safeList(e.getCandidates()));
            requestRebuild();
            return;
        }

        if (event instanceof StatusEvent) {
            StatusEvent e = (StatusEvent) event;
            lastStatus = e;

            currentBodyName = e.getBodyName();
            currentLat = e.getLatitude();
            currentLon = e.getLongitude();
            currentPlanetRadiusM = e.getPlanetRadius();
            onFootOrSrv = e.isOnFoot() || e.isInSrv();

            updateBoundaryVoiceIfNeeded();
            requestRebuild();
            return;
        }

        if (event instanceof ScanOrganicEvent) {
            ScanOrganicEvent e = (ScanOrganicEvent) event;

            handleScanOrganic(e);
            requestRebuild();
        }
    }

    private void clearSurfaceState() {
        predictionsByBodyId.clear();
        observedByBodyIdAndGenus.clear();
        samplePointsByBodyId.clear();
        activeSpeciesKey = null;
        activeSpeciesGenus = null;
        wasInsideBoundary = false;
    }

    private void handleScanOrganic(ScanOrganicEvent e) {
        int bodyId = e.getBodyId();

        String genus = canonicalName(nonEmpty(e.getGenusLocalised(), e.getGenus()));
        String species = canonicalName(nonEmpty(e.getSpeciesLocalised(), e.getSpecies()));

        if (genus == null || genus.isEmpty()) {
            return;
        }

        String displayName;
        if (species != null && !species.isEmpty()) {
            displayName = species;
        } else {
            displayName = genus;
        }

        // Update "observed" map: for this genus, we now know which species is present.
        Map<String, String> byGenus = observedByBodyIdAndGenus.get(bodyId);
        if (byGenus == null) {
            byGenus = new HashMap<>();
            observedByBodyIdAndGenus.put(bodyId, byGenus);
        }
        byGenus.put(genus.toLowerCase(Locale.ROOT), displayName);

        // Capture a sample point if we have lat/long.
        StatusEvent st = lastStatus;
        Double lat = null;
        Double lon = null;
        if (st != null) {
            lat = st.getLatitude();
            lon = st.getLongitude();
        }
        if (lat == null || lon == null) {
            return;
        }

        // In-game rule: scanning a different species before completion resets previous partial.
        if (activeSpeciesKey != null && !activeSpeciesKey.equals(displayName)) {
            int prevCount = getSampleCount(bodyId, activeSpeciesKey);
            if (prevCount > 0 && prevCount < 3) {
                clearSamples(bodyId, activeSpeciesKey);
            }
        }

        activeSpeciesKey = displayName;
        activeSpeciesGenus = genus;

        addSamplePoint(bodyId, displayName, new SamplePoint(lat.doubleValue(), lon.doubleValue(), e.getTimestamp()));
    }

    private void addSamplePoint(int bodyId, String speciesKey, SamplePoint p) {
        Map<String, List<SamplePoint>> bySpecies = samplePointsByBodyId.get(bodyId);
        if (bySpecies == null) {
            bySpecies = new LinkedHashMap<>();
            samplePointsByBodyId.put(bodyId, bySpecies);
        }

        List<SamplePoint> pts = bySpecies.get(speciesKey);
        if (pts == null) {
            pts = new ArrayList<>();
            bySpecies.put(speciesKey, pts);
        }

        // De-dupe: if extremely close to the most recent point, ignore.
        if (!pts.isEmpty()) {
            SamplePoint last = pts.get(pts.size() - 1);
            double d = greatCircleMeters(last.lat, last.lon, p.lat, p.lon, effectiveRadiusM());
            if (d < 2.0) {
                return;
            }
        }

        if (pts.size() >= 3) {
            pts.remove(0);
        }

        pts.add(p);
    }

    private void clearSamples(int bodyId, String speciesKey) {
        Map<String, List<SamplePoint>> bySpecies = samplePointsByBodyId.get(bodyId);
        if (bySpecies == null) {
            return;
        }

        bySpecies.remove(speciesKey);
    }

    private int getSampleCount(int bodyId, String speciesKey) {
        Map<String, List<SamplePoint>> bySpecies = samplePointsByBodyId.get(bodyId);
        if (bySpecies == null) {
            return 0;
        }

        List<SamplePoint> pts = bySpecies.get(speciesKey);
        if (pts == null) {
            return 0;
        }

        return pts.size();
    }

    private void updateBoundaryVoiceIfNeeded() {
        if (activeSpeciesKey == null) {
            return;
        }
        if (!onFootOrSrv) {
            return;
        }
        if (currentLat == null || currentLon == null) {
            return;
        }

        int bodyId = currentBodyIdBestEffort();
        if (bodyId < 0) {
            return;
        }

        int count = getSampleCount(bodyId, activeSpeciesKey);
        if (count <= 0 || count >= 3) {
            wasInsideBoundary = false;
            return;
        }

        Integer thresholdM = BioColonyDistance.metersForGenus(activeSpeciesGenus);
        if (thresholdM == null) {
            return;
        }

        double minDist = minDistanceToSamples(bodyId, activeSpeciesKey);
        boolean inside = (minDist >= 0.0) && (minDist < thresholdM.intValue());

        if (inside == wasInsideBoundary) {
            return;
        }

        wasInsideBoundary = inside;

        TtsSprintf tts = new TtsSprintf(new PollyTtsCached());
        if (inside) {
            tts.speakf("Too close to {bio}. Need {meters} meters.", activeSpeciesKey, thresholdM);
        } else {
            tts.speakf("Clear for next {bio} sample.", activeSpeciesKey);
        }
    }

    private double minDistanceToSamples(int bodyId, String speciesKey) {
        Map<String, List<SamplePoint>> bySpecies = samplePointsByBodyId.get(bodyId);
        if (bySpecies == null) {
            return -1.0;
        }
        List<SamplePoint> pts = bySpecies.get(speciesKey);
        if (pts == null || pts.isEmpty()) {
            return -1.0;
        }
        if (currentLat == null || currentLon == null) {
            return -1.0;
        }

        double r = effectiveRadiusM();
        double min = Double.MAX_VALUE;

        for (SamplePoint p : pts) {
            double d = greatCircleMeters(currentLat.doubleValue(), currentLon.doubleValue(), p.lat, p.lon, r);
            if (d < min) {
                min = d;
            }
        }

        return min;
    }

    private double effectiveRadiusM() {
        Double r = currentPlanetRadiusM;
        if (r == null || r.isNaN() || r.doubleValue() < 1000.0) {
            return 6_371_000.0;
        }
        return r.doubleValue();
    }

    private int currentBodyIdBestEffort() {
        // We do not have bodyId in Status.json. Best effort:
        // - If we have any ScanOrganic points, that bodyId is authoritative.
        // - Otherwise, if we have predictions, pick the one whose short name matches currentBodyName.

        if (!samplePointsByBodyId.isEmpty()) {
            // Most recent bodyId that has samples.
            // (LinkedHashMap is not guaranteed here; just return any - typically there is only one active body.)
            for (Integer id : samplePointsByBodyId.keySet()) {
                if (id != null) {
                    return id.intValue();
                }
            }
        }

        if (currentBodyName == null) {
            return -1;
        }

        String shortName = stripSystemPrefix(currentSystemName, currentBodyName);

        // We do not have bodyName -> bodyId map; predictions are stored by bodyId only.
        // So we cannot match reliably without additional state.
        // Return the first predicted body id as a fallback.
        for (Integer id : predictionsByBodyId.keySet()) {
            if (id != null) {
                return id.intValue();
            }
        }

        // If nothing else, use -1 (no surface tracking).
        @SuppressWarnings("unused")
        String unused = shortName;
        return -1;
    }

    private void requestRebuild() {
        SwingUtilities.invokeLater(() -> rebuildUi());
    }

    private void rebuildUi() {
        int bodyId = currentBodyIdBestEffort();

        String bodyLabel = currentBodyName;
        if (bodyLabel == null || bodyLabel.trim().isEmpty()) {
            bodyLabel = "(unknown body)";
        }

        if (onFootOrSrv) {
            header.setText("Surface biology: " + stripSystemPrefix(currentSystemName, bodyLabel));
        } else {
            header.setText("Surface biology (need on-foot/SRV): " + stripSystemPrefix(currentSystemName, bodyLabel));
        }

        List<RowData> rows = buildRowsForBody(bodyId);
        tableModel.setRows(rows);
    }

    private List<RowData> buildRowsForBody(int bodyId) {
        if (bodyId < 0) {
            return Collections.emptyList();
        }

        List<BioCandidate> preds = predictionsByBodyId.get(bodyId);
        if (preds == null) {
            preds = Collections.emptyList();
        }

        Map<String, String> observedByGenus = observedByBodyIdAndGenus.get(bodyId);
        if (observedByGenus == null) {
            observedByGenus = Collections.emptyMap();
        }

        Map<String, List<SamplePoint>> samples = samplePointsByBodyId.get(bodyId);
        if (samples == null) {
            samples = Collections.emptyMap();
        }

        // Build a combined set of species display names.
        // - Start with predictions.
        // - Apply observed-genus filtering: if a genus has an observed species, keep only that.
        // - Also include any sampled species that might not be in predictions.
        Map<String, RowData> byName = new LinkedHashMap<>();

        for (BioCandidate c : preds) {
            String display = canonicalName(c.getDisplayName());
            if (display == null || display.isEmpty()) {
                continue;
            }

            String genusLower = firstWord(display).toLowerCase(Locale.ROOT);
            String observedSpecies = observedByGenus.get(genusLower);
            if (observedSpecies != null && !observedSpecies.equalsIgnoreCase(display)) {
                continue;
            }

            RowData r = byName.get(display);
            if (r == null) {
                r = new RowData(display);
                byName.put(display, r);
            }
            r.estimatedCr = c.getEstimatedPayout(Boolean.TRUE);
            r.genus = firstWord(display);
        }

        // Ensure observed species appear even if not predicted.
        for (String observed : observedByGenus.values()) {
            String display = canonicalName(observed);
            if (display == null || display.isEmpty()) {
                continue;
            }
            RowData r = byName.get(display);
            if (r == null) {
                r = new RowData(display);
                r.genus = firstWord(display);
                byName.put(display, r);
            }
            r.observed = true;
        }

        // Ensure sampled species appear.
        for (String sampled : samples.keySet()) {
            String display = canonicalName(sampled);
            if (display == null || display.isEmpty()) {
                continue;
            }
            RowData r = byName.get(display);
            if (r == null) {
                r = new RowData(display);
                r.genus = firstWord(display);
                byName.put(display, r);
            }
        }

        // Attach sample points + compute distances.
        for (RowData r : byName.values()) {
            List<SamplePoint> pts = samples.get(r.displayName);
            if (pts == null) {
                pts = Collections.emptyList();
            }
            r.samplePoints = pts;
            r.sampleDistancesM = computeDistances(pts);
            r.sampleCount = pts.size();
        }

        List<RowData> rows = new ArrayList<>(byName.values());
        rows.sort((a, b) -> {
            // Incomplete with samples first
            boolean aInProgress = a.sampleCount > 0 && a.sampleCount < 3;
            boolean bInProgress = b.sampleCount > 0 && b.sampleCount < 3;
            if (aInProgress != bInProgress) {
                return aInProgress ? -1 : 1;
            }

            // Observed next
            if (a.observed != b.observed) {
                return a.observed ? -1 : 1;
            }

            // Higher value next
            long aVal = (a.estimatedCr != null) ? a.estimatedCr.longValue() : Long.MIN_VALUE;
            long bVal = (b.estimatedCr != null) ? b.estimatedCr.longValue() : Long.MIN_VALUE;
            int cmp = Long.compare(bVal, aVal);
            if (cmp != 0) {
                return cmp;
            }

            return a.displayName.compareToIgnoreCase(b.displayName);
        });

        return rows;
    }

    private double[] computeDistances(List<SamplePoint> pts) {
        double[] d = new double[]{Double.NaN, Double.NaN, Double.NaN};

        if (pts == null || pts.isEmpty()) {
            return d;
        }
        if (currentLat == null || currentLon == null) {
            return d;
        }

        double r = effectiveRadiusM();

        for (int i = 0; i < pts.size() && i < 3; i++) {
            SamplePoint p = pts.get(i);
            d[i] = greatCircleMeters(currentLat.doubleValue(), currentLon.doubleValue(), p.lat, p.lon, r);
        }

        return d;
    }

    // ---------------------------------------------------------------------
    // UI / font
    // ---------------------------------------------------------------------

    public void applyUiFontPreferences() {
        applyUiFont(OverlayPreferences.getUiFont());
    }

    public void applyUiFont(Font font) {
        if (font != null) {
            uiFont = font;
            header.setFont(uiFont.deriveFont(Font.BOLD));
            table.setFont(uiFont);
            table.setRowHeight(Math.max(22, uiFont.getSize() + 2));
            JTableHeader th = table.getTableHeader();
            if (th != null) {
                th.setFont(uiFont.deriveFont(Font.BOLD));
            }
        }

        revalidate();
        repaint();
    }

    // ---------------------------------------------------------------------
    // Table + renderer
    // ---------------------------------------------------------------------

    private static final class BiologyTable extends JTable {

        BiologyTable(BiologyTableModel model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(ED_ORANGE_TRANS);

                int rowCount = getRowCount();
                for (int row = 1; row < rowCount; row++) {
                    int y = getCellRect(row, 0, true).y;
                    g2.drawLine(0, y, getWidth(), y);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class BiologyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        static final int COL_BIO = 0;
        static final int COL_SAMPLES = 1;
        static final int COL_VALUE = 2;

        private final String[] cols = new String[]{"Biology", "Samples", "Value"};
        private final List<RowData> rows = new ArrayList<>();

        void setRows(List<RowData> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        RowData rowAt(int row) {
            return rows.get(row);
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
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == COL_SAMPLES) {
                return RowData.class;
            }
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RowData r = rows.get(rowIndex);

            if (columnIndex == COL_BIO) {
                return r.displayName;
            }
            if (columnIndex == COL_SAMPLES) {
                return r;
            }
            if (columnIndex == COL_VALUE) {
                if (r.estimatedCr == null) {
                    return "";
                }
                long millions = Math.round(r.estimatedCr.longValue() / 1_000_000.0);
                return String.format(Locale.US, "%dM Cr", Long.valueOf(millions));
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private final class SamplesCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private RowData row;
        private boolean selected;

        SamplesCellRenderer() {
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            this.selected = isSelected;
            this.row = (value instanceof RowData) ? (RowData) value : null;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(160, 26);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int h = getHeight();
                int cx0 = 16;
                int r = Math.min(12, (h - 4) / 2);
                int spacing = 52;
                int cy = h / 2;

                Color fg;
                if (selected) {
                    fg = Color.BLACK;
                } else {
                    fg = ED_ORANGE;
                }

                g2.setFont(uiFont.deriveFont(Font.PLAIN, Math.max(10f, uiFont.getSize2D() - 2f)));

                for (int i = 0; i < 3; i++) {
                    int cx = cx0 + (i * spacing);

                    boolean hasSample = false;
                    String text = "";
                    if (row != null && row.sampleCount > i) {
                        hasSample = true;
                        double dM = row.sampleDistancesM != null && row.sampleDistancesM.length > i
                                ? row.sampleDistancesM[i]
                                : Double.NaN;
                        text = fmtDistance(dM);
                    }

                    Ellipse2D circle = new Ellipse2D.Double(cx - r, cy - r, r * 2.0, r * 2.0);

                    if (hasSample) {
                        g2.setColor(fg);
                        g2.fill(circle);
                        g2.setColor(selected ? Color.BLACK : Color.BLACK);
                        // text will be drawn in black? That disappears. Use white.
                        g2.setColor(Color.WHITE);
                    } else {
                        g2.setColor(fg);
                        g2.draw(circle);
                        g2.setColor(fg);
                    }

                    if (!text.isEmpty()) {
                        int tw = g2.getFontMetrics().stringWidth(text);
                        int th = g2.getFontMetrics().getAscent();
                        g2.drawString(text, cx - (tw / 2), cy + (th / 2) - 2);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ---------------------------------------------------------------------
    // Row state
    // ---------------------------------------------------------------------

    private static final class RowData {
        final String displayName;
        String genus;
        Long estimatedCr;
        boolean observed;
        int sampleCount;
        List<SamplePoint> samplePoints = Collections.emptyList();
        double[] sampleDistancesM = new double[]{Double.NaN, Double.NaN, Double.NaN};

        RowData(String displayName) {
            this.displayName = displayName;
        }
    }

    private static final class SamplePoint {
        final double lat;
        final double lon;
        final Instant time;

        SamplePoint(double lat, double lon, Instant time) {
            this.lat = lat;
            this.lon = lon;
            this.time = time;
        }
    }

    // ---------------------------------------------------------------------
    // Colony distance table
    // ---------------------------------------------------------------------

    private static final class BioColonyDistance {

        private static final Map<String, Integer> GENUS_TO_METERS;

        static {
            Map<String, Integer> m = new HashMap<>();

            // Common Odyssey exobiology minimum separation distances.
            // (These are per-genus; most are constant for a given genus.)
            // If you want the full table expanded, tell me what you want included.
            m.put("bacterium", Integer.valueOf(500));
            m.put("stratum", Integer.valueOf(500));
            m.put("tussock", Integer.valueOf(200));
            m.put("fungoida", Integer.valueOf(300));
            m.put("tubus", Integer.valueOf(800));
            m.put("clypeus", Integer.valueOf(800));
            m.put("electricae", Integer.valueOf(1000));
            m.put("frutexa", Integer.valueOf(150));
            m.put("osseo", Integer.valueOf(800));
            m.put("concha", Integer.valueOf(300));
            m.put("aleoida", Integer.valueOf(150));
            m.put("recepta", Integer.valueOf(800));
            m.put("fonticulua", Integer.valueOf(500));
            m.put("shrub", Integer.valueOf(150));
            m.put("cornar", Integer.valueOf(300));

            GENUS_TO_METERS = Collections.unmodifiableMap(m);
        }

        static Integer metersForGenus(String genusName) {
            if (genusName == null) {
                return null;
            }
            String g = genusName.trim().toLowerCase(Locale.ROOT);
            if (g.isEmpty()) {
                return null;
            }
            return GENUS_TO_METERS.get(g);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static <T> List<T> safeList(List<T> in) {
        if (in == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(in);
    }

    private static String firstWord(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        int sp = t.indexOf(' ');
        if (sp < 0) {
            return t;
        }
        return t.substring(0, sp);
    }

    private static String canonicalName(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        // Normalize whitespace
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    private static String nonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a;
        }
        return b;
    }

    private static String stripSystemPrefix(String systemName, String bodyName) {
        if (bodyName == null) {
            return "";
        }
        String t = bodyName;
        if (systemName != null && !systemName.trim().isEmpty()) {
            t = t.replace(systemName, "").trim();
            if (t.isEmpty()) {
                t = bodyName;
            }
        }
        return t;
    }

    private static String fmtDistance(double meters) {
        if (Double.isNaN(meters) || meters < 0.0) {
            return "";
        }
        if (meters < 1000.0) {
            return String.format(Locale.US, "%.0fm", Double.valueOf(meters));
        }
        double km = meters / 1000.0;
        if (km < 10.0) {
            return String.format(Locale.US, "%.1fk", Double.valueOf(km));
        }
        return String.format(Locale.US, "%.0fk", Double.valueOf(km));
    }

    // Haversine great-circle distance on a sphere.
    private static double greatCircleMeters(double lat1Deg,
                                            double lon1Deg,
                                            double lat2Deg,
                                            double lon2Deg,
                                            double radiusM) {
        double lat1 = Math.toRadians(lat1Deg);
        double lon1 = Math.toRadians(lon1Deg);
        double lat2 = Math.toRadians(lat2Deg);
        double lon2 = Math.toRadians(lon2Deg);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return radiusM * c;
    }
}
