package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.dce.ed.cache.CachedSystem;
import org.dce.ed.cache.SystemCache;
import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.event.BioScanPredictionEvent;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.state.BodyInfo;
import org.dce.ed.state.SystemEventProcessor;
import org.dce.ed.state.SystemState;
import org.dce.ed.tts.PollyTtsCached;
import org.dce.ed.tts.TtsSprintf;
import org.dce.ed.util.EdsmClient;

/**
 * System tab – now a *pure UI* renderer.
 *
 * All parsing, prediction, and system-state logic lives in:
 *   SystemState
 *   SystemEventProcessor
 *   SystemCache
 */
public class SystemTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color ED_ORANGE = new Color(255, 140, 0);
    // NEW: semi-transparent orange for separators, similar to RouteTabPanel
    private static final Color ED_ORANGE_TRANS = new Color(255, 140, 0, 64);
    // NEW: shared ED font (similar to Route tab)
        private Font uiFont = OverlayPreferences.getUiFont();

    private final JTable table;
    private final JTextField headerLabel;
    private final SystemBodiesTableModel tableModel;

    private final SystemState state = new SystemState();
    private final SystemEventProcessor processor = new SystemEventProcessor(EliteDangerousOverlay.clientKey, state, new EdsmClient());

    private final EdsmClient edsmClient = new EdsmClient();

	private JLabel headerSummaryLabel;
    
    public SystemTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        // Header label
        headerLabel = new JTextField("Waiting for system data…");
        headerLabel.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = headerLabel.getText();
                    if (text == null) {
                        return;
                    }
//PLOEA EURL mn-j d9-22
//PLOEA EURL ZP-T C18-0
                    
                    System.out.println("User hit enter for system: '" + text + "'");

                    // User is specifying by name; let loadSystem resolve address
                    state.setSystemName(text);
                    state.setSystemAddress(0L);

                    loadSystem(text, 0L);
                }
            }
        });
        headerLabel.setForeground(ED_ORANGE);
        headerLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        headerLabel.setOpaque(false);
        headerLabel.setFont(uiFont.deriveFont(Font.BOLD));

        headerSummaryLabel = new JLabel();
        headerSummaryLabel.setForeground(ED_ORANGE);
        headerSummaryLabel.setFont(uiFont.deriveFont(Font.BOLD));
        headerSummaryLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        headerSummaryLabel.setOpaque(false);
        
        // Table setup
        tableModel = new SystemBodiesTableModel();
        table = new SystemBodiesTable(tableModel);
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(500, 300));
        // NEW: apply ED font to table cells
        table.setFont(uiFont);
        table.setRowHeight(24);

        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setForeground(ED_ORANGE);
        header.setBackground(Color.BLACK);
        header.setFont(uiFont.deriveFont(Font.BOLD));

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, false, false, row, column);

                label.setOpaque(true);
                label.setBackground(Color.BLACK);
                label.setForeground(ED_ORANGE);
                label.setFont(uiFont.deriveFont(Font.BOLD));
                label.setHorizontalAlignment(LEFT);
                label.setBorder(new EmptyBorder(0, 4, 0, 4));

                return label;
            }
        });

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
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
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Biological detail rows (bioText/bioValue) should be gray when not selected
                Row r = tableModel.getRowAt(row);
                boolean isBioRow = (r != null && r.detail && (r.bioText != null || r.bioValue != null));

                if (isSelected) {
                    c.setForeground(Color.BLACK);
                } else if (isBioRow) {
                    int samples = r.getBioSampleCount();

                    if (samples >= 3) {
                        c.setForeground(Color.GREEN);
                    } else if (samples > 0) {
                        c.setForeground(Color.YELLOW);
                    } else {
                        c.setForeground(new Color(180, 180, 180)); // gray for biologicals
                    }
                } else {
                    c.setForeground(ED_ORANGE);
                }

                return c;
            }
        };

        table.setDefaultRenderer(Object.class, cellRenderer);


        DefaultTableCellRenderer valueRightRenderer = new DefaultTableCellRenderer() {
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
                Component c = super.getTableCellRendererComponent(table,
                                                                  value,
                                                                  isSelected,
                                                                  hasFocus,
                                                                  row,
                                                                  column);

                setHorizontalAlignment(SwingConstants.RIGHT);

                // Biological detail rows should be gray in the Value column too
                Row r = tableModel.getRowAt(row);
                boolean isBioRow = (r != null && r.detail && (r.bioText != null || r.bioValue != null));

                if (isSelected) {
                    c.setForeground(Color.BLACK);
                } else if (isBioRow) {
                    c.setForeground(new Color(180, 180, 180));
                } else {
                    c.setForeground(ED_ORANGE);
                }

                return c;
            }
        };

        // Column index 4 is "Value"
        table.getColumnModel().getColumn(4).setCellRenderer(valueRightRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JViewport headerViewport = scrollPane.getColumnHeader();
        if (headerViewport != null) {
            headerViewport.setOpaque(false);
            headerViewport.setBackground(new Color(0, 0, 0, 0));
        }

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        setBorder(new EmptyBorder(4, 4, 4, 4));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(headerLabel, BorderLayout.WEST);
        headerPanel.add(headerSummaryLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

     // Column widths preserved
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(40);
        columns.getColumn(1).setPreferredWidth(60);

        // Resize "Atmo / Body" down to ~60% and give the space to "Bio".
        int atmoBodyOld = 220;
        int bioOld = 70;

        int atmoBodyNew = (int) Math.round(atmoBodyOld * 0.60); // 132
        int delta = atmoBodyOld - atmoBodyNew;                  // 88
        int bioNew = bioOld + delta;                            // 158

        columns.getColumn(2).setPreferredWidth(atmoBodyNew); // "Atmo / Body"
        columns.getColumn(3).setPreferredWidth(bioNew);      // "Bio"

        // Keep Value column as-is (same place, same width constraints)
        columns.getColumn(4).setPreferredWidth(20);
        columns.getColumn(4).setMinWidth(60);
        columns.getColumn(4).setMaxWidth(80);
        columns.getColumn(4).setResizable(false);

        columns.getColumn(5).setPreferredWidth(30);
        columns.getColumn(6).setPreferredWidth(80);


        refreshFromCache();
    }

    // ---------------------------------------------------------------------
    // Event forwarding
    // ---------------------------------------------------------------------

    public void handleLogEvent(EliteLogEvent event) {
        if (event == null) {
            return;
        }

        // 1) Mutate domain state (can be on background thread)
        processor.handleEvent(event);

        // 2) If we jumped, do the heavy load/merge off the EDT,
        //    then refresh UI on the EDT.
        if (event instanceof BioScanPredictionEvent) {
        	BioScanPredictionEvent e = (BioScanPredictionEvent)event;
        	
        	List<BioCandidate> candidates = e.getCandidates();
        	
        	long highestPayout = 0L;
        	
        	for (BioCandidate bio : candidates) {
        		System.out.println("Need to know which planet so we can tell expected value");
        		highestPayout = Math.max(highestPayout, bio.getEstimatedPayout(e.getBonusApplies()));
        	}
        	TtsSprintf ttsSprintf = new TtsSprintf(new PollyTtsCached());
        	
        	if (highestPayout > 20000000) {
            	ttsSprintf.speakf("{n} species discovered on planetary body {body} with estimated value of {credits}",
            			candidates.size(),
            			e.getBodyName(),
            			highestPayout);
        	} else {
            	ttsSprintf.speakf("{n} species discovered on planetary body {body}",
            			candidates.size(),
            			e.getBodyName());
        	}



        	
        }
        if (event instanceof FsdJumpEvent) {
            FsdJumpEvent e = (FsdJumpEvent) event;

            new Thread(() -> {


                javax.swing.SwingUtilities.invokeLater(() -> {
                    loadSystem(e.getStarSystem(), e.getSystemAddress());
                    requestRebuild();
                    persistIfPossible();
                });
            }, "SystemTabPanel-loadSystem").start();

            return;
        }

        // 3) Normal events: just refresh UI on EDT
            requestRebuild();
            persistIfPossible();
    }

    // ---------------------------------------------------------------------
    // Cache loading at startup
    // ---------------------------------------------------------------------

    public void refreshFromCache() {
        try {
            EliteJournalReader reader = new EliteJournalReader(EliteDangerousOverlay.clientKey);

            String systemName = null;
            long systemAddress = 0L;

            List<EliteLogEvent> events = reader.readEventsFromLastNJournalFiles(3);

            for (EliteLogEvent event : events) {
                if (event instanceof LocationEvent) {
                    LocationEvent e = (LocationEvent) event;
                    systemName = e.getStarSystem();
                    systemAddress = e.getSystemAddress();
                } else if (event instanceof FsdJumpEvent) {
                    FsdJumpEvent e = (FsdJumpEvent) event;
                    systemName = e.getStarSystem();
                    systemAddress = e.getSystemAddress();
                }
            }

            if ((systemName == null || systemName.isEmpty()) && systemAddress == 0L) {
                rebuildTable();
                return;
            }
            
            loadSystem(systemName, systemAddress);
            rebuildTable();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadSystem(String systemName, long systemAddress) {
        SystemCache cache = SystemCache.getInstance();
        CachedSystem cs = cache.get(systemAddress, systemName);

        // Start from a clean state for this system.
        state.setSystemName(systemName);
        state.setSystemAddress(systemAddress);
        state.resetBodies();
        state.setTotalBodies(null);
        state.setNonBodyCount(null);
        state.setFssProgress(null);
        state.setAllBodiesFound(null);

        // 1) Load from cache if we have it
        if (cs != null) {
            cache.loadInto(state, cs);
        }

        // 2) Always try to enrich with EDSM via a single bodies call
        try {
            BodiesResponse edsmBodies = edsmClient.showBodies(systemName);
            if (edsmBodies != null) {
                edsmClient.mergeBodiesFromEdsm(state, edsmBodies);
            }
        } catch (Exception ex) {
            // EDSM is best-effort; overlay should still work from cache/logs.
            ex.printStackTrace();
        }

        // 3) Refresh UI and persist merged result
        rebuildTable();
        persistIfPossible();
    }
    private final AtomicBoolean rebuildPending = new AtomicBoolean(false);

    // ---------------------------------------------------------------------
    // UI rebuild from SystemState
    // ---------------------------------------------------------------------
    private void requestRebuild() {
        if (!rebuildPending.compareAndSet(false, true)) {
            return; // already queued
        }

        SwingUtilities.invokeLater(() -> {
            try {
                rebuildTable();
            } finally {
                rebuildPending.set(false);
            }
        });
    }
    private void rebuildTable() {
        dedupeBodiesByName();
        updateHeaderLabel();

        List<Row> rows = BioTableBuilder.buildRows(state.getBodies().values());
        tableModel.setRows(rows);

        // Debug only:
        // debugDumpBioRowsToConsole();
    }
    private static String canonicalBioName(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return s;
        }

        String[] parts = s.split("\\s+");
        // Collapse "Genus Genus Species..." -> "Genus Species..."
        if (parts.length >= 3 && parts[0].equalsIgnoreCase(parts[1])) {
            StringBuilder sb = new StringBuilder(parts[0]);
            for (int i = 2; i < parts.length; i++) {
                sb.append(' ').append(parts[i]);
            }
            return sb.toString();
        }

        return s;
    }


    public static String firstWord(String s) {
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    private void updateHeaderLabel() {
        String systemName = state.getSystemName();
        headerLabel.setText(systemName != null ? systemName : "");
        
        StringBuilder sb = new StringBuilder();

        if (state.getTotalBodies() != null) {
            int scanned = state.getBodies().size();
            sb.append("  |  Bodies: ").append(scanned)
              .append(" of ").append(state.getTotalBodies());

            if (state.getFssProgress() != null) {
                sb.append("  (")
                  .append(Math.round(state.getFssProgress() * 100.0))
                  .append("%)");
            }
        }

        if (state.getNonBodyCount() != null) {
            sb.append("  |  Non-bodies: ").append(state.getNonBodyCount());
        }

        headerSummaryLabel.setText(sb.toString());
    }
    public static boolean bodyIssues = false;
    private void persistIfPossible() {
        if (state.getSystemName() == null
                || state.getSystemAddress() == 0L
                || state.getBodies().isEmpty()) {
            return;
        }

        boolean hasAnyRealBodies = false;

        for (BodyInfo x : state.getBodies().values()) {
            if (x == null) {
                continue;
            }

            // Temp bodies created before we learn BodyID are allowed.
            if (x.getBodyId() >= 0) {
                hasAnyRealBodies = true;
            }
        }

        if (!hasAnyRealBodies) {
            return;
        }

        SystemCache.getInstance().storeSystem(state);
    }

    // ---------------------------------------------------------------------
    // Table model
    // ---------------------------------------------------------------------

    public static class Row {
        final BodyInfo body;
        final boolean detail;
        final int parentId;
        final String bioText;
        final String bioValue;
        private int bioSampleCount;
        
        private boolean observedGenusHeader;

        boolean isObservedGenusHeader() {
            return observedGenusHeader;
        }

        void setObservedGenusHeader(boolean observedGenusHeader) {
            this.observedGenusHeader = observedGenusHeader;
        }
        private Row(BodyInfo body,
                    boolean detail,
                    int parentId,
                    String bioText,
                    String bioValue) {
            this.body = body;
            this.detail = detail;
            this.parentId = parentId;
            this.bioText = bioText;
            this.bioValue = bioValue;
            this.bioSampleCount = 0;
        }
        int getBioSampleCount() {
            return bioSampleCount;
        }

        void setBioSampleCount(int bioSampleCount) {
            this.bioSampleCount = bioSampleCount;
        }

        static Row bio(int parentId, String text, String val, int bioSampleCount) {
            Row r = new Row(null, true, parentId, text, val);
            r.setBioSampleCount(bioSampleCount);
            return r;
        }
        static Row body(BodyInfo b) {
            return new Row(b, false, -1, null, null);
        }

        static Row bio(int parentId, String text, String val) {
            return new Row(null, true, parentId, text, val);
        }
    }

    // NEW: custom JTable to draw separators only between systems
    private class SystemBodiesTable extends JTable {

        SystemBodiesTable(SystemBodiesTableModel model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(ED_ORANGE_TRANS);

                int rowCount = tableModel.getRowCount();
                boolean firstBodySeen = false;

                for (int row = 0; row < rowCount; row++) {
                    Row r = tableModel.getRowAt(row);
                    if (!r.detail) { // body row
                        if (firstBodySeen) {
                            Rectangle rect = getCellRect(row, 0, true);
                            int y = rect.y;
                            g2.setColor(ED_ORANGE_TRANS);
                            g2.drawLine(0, y, getWidth(), y);
                        } else {
                            firstBodySeen = true;
                        }
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    class SystemBodiesTableModel extends AbstractTableModel {

        private final String[] columns = {
                "Body",
                "g",
                "Atmo / Body",
                "Bio",
                "Value",
                "Land",
                "Dist (Ls)"
        };

        private final List<Row> rows = new ArrayList<>();

        void setRows(List<Row> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        // NEW: allow table to inspect rows (for separators)
        Row getRowAt(int index) {
            return rows.get(index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int col) {
            Row r = rows.get(rowIndex);

            if (r.detail) {
                switch (col) {
                    case 3: return r.bioText != null ? r.bioText : "";
                    case 4: return r.bioValue != null ? r.bioValue : "";
                    default: return "";
                }
            }

            BodyInfo b = r.body;
            switch (col) {
                case 0:
                    return b.getShortName() != null ? b.getShortName() : "";
                case 1:
                    if (b.getGravityMS() == null) return "";
                    double g = b.getGravityMS() / 9.80665;
                    return String.format(Locale.US, "%.2f g", g);
                case 2:
                    String atmo = b.getAtmoOrType() != null ? b.getAtmoOrType() : "";
                    atmo = atmo.replaceAll("content body",  "body");
                    atmo = atmo.replaceAll("atmosphere",  "");
                    return atmo;
                case 3:
                    // CHANGED: remove bare "Bio" label – only show Geo / Bio+Geo
                    if (b.hasBio() && b.hasGeo()) return "Bio + Geo";
                    if (b.hasGeo()) return "Geo";
                    return "";
                case 4:
                    // Keep "High" marker for the main body row;
                    // detail rows carry the M Cr values.
                    return b.isHighValue() ? "High" : "";
                case 5:
                    return b.isLandable() ? "Yes" : "";
                case 6:
                    if (Double.isNaN(b.getDistanceLs())) return "";
                    return String.format(Locale.US, "%.0f Ls", b.getDistanceLs());
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    }
    private void dedupeBodiesByName() {

        Map<Integer, BodyInfo> bodies = state.getBodies();
        if (bodies == null || bodies.isEmpty()) {
            return;
        }

        Map<String, Integer> nameToKey = new HashMap<>();
        List<Integer> keysToRemove = new ArrayList<>();

        for (Map.Entry<Integer, BodyInfo> e : bodies.entrySet()) {

            Integer key = e.getKey();
            BodyInfo bi = e.getValue();

            if (bi == null) {
                continue;
            }

            String name = bi.getBodyName();
            if (name == null) {
                continue;
            }

            String canon = name.trim().toLowerCase(Locale.ROOT);
            if (canon.isEmpty()) {
                continue;
            }

            Integer existingKey = nameToKey.get(canon);
            if (existingKey == null) {
                nameToKey.put(canon, key);
                continue;
            }

            BodyInfo keep = bodies.get(existingKey);
            BodyInfo drop = bi;

            if (keep == null) {
                nameToKey.put(canon, key);
                continue;
            }

            // Prefer the entry with a non-negative bodyId as the "keeper"
            // (some paths still create temp/unknown ids during rescan).
            if (keep.getBodyId() < 0 && drop.getBodyId() >= 0) {
                BodyInfo tmp = keep;
                keep = drop;
                drop = tmp;

                // Swap which key is considered the keeper for later duplicates
                nameToKey.put(canon, key);
                keysToRemove.add(existingKey);
            } else {
                keysToRemove.add(key);
            }

            mergeBodiesKeepBest(keep, drop);

            // Useful debug to prove what's happening (leave it in until stable)
//            System.out.println("DEDUP body name='" + name + "' keepId=" + keep.getBodyId()
//                    + " dropId=" + drop.getBodyId());
        }

        for (Integer k : keysToRemove) {
            bodies.remove(k);
        }
    }

    private static void mergeBodiesKeepBest(BodyInfo keep, BodyInfo drop) {

        if (keep == null || drop == null) {
            return;
        }

        if (keep.getStarSystem() == null && drop.getStarSystem() != null) {
            keep.setStarSystem(drop.getStarSystem());
        }
        if (keep.getBodyName() == null && drop.getBodyName() != null) {
            keep.setBodyName(drop.getBodyName());
        }

        if (keep.getStarPos() == null && drop.getStarPos() != null) {
            keep.setStarPos(drop.getStarPos());
        }

        if (Double.isNaN(keep.getDistanceLs()) && !Double.isNaN(drop.getDistanceLs())) {
            keep.setDistanceLs(drop.getDistanceLs());
        }

        if (keep.getGravityMS() == null && drop.getGravityMS() != null) {
            keep.setGravityMS(drop.getGravityMS());
        }
        if (keep.getSurfaceTempK() == null && drop.getSurfaceTempK() != null) {
            keep.setSurfaceTempK(drop.getSurfaceTempK());
        }
        if (keep.getSurfacePressure() == null && drop.getSurfacePressure() != null) {
            keep.setSurfacePressure(drop.getSurfacePressure());
        }

        if (keep.getPlanetClass() == null && drop.getPlanetClass() != null) {
            keep.setPlanetClass(drop.getPlanetClass());
        }
        if (keep.getAtmosphere() == null && drop.getAtmosphere() != null) {
            keep.setAtmosphere(drop.getAtmosphere());
        }
        if (keep.getAtmoOrType() == null && drop.getAtmoOrType() != null) {
            keep.setAtmoOrType(drop.getAtmoOrType());
        }

        if (!keep.isLandable() && drop.isLandable()) {
            keep.setLandable(true);
        }
        if (!keep.hasBio() && drop.hasBio()) {
            keep.setHasBio(true);
        }
        if (!keep.hasGeo() && drop.hasGeo()) {
            keep.setHasGeo(true);
        }

        if (keep.getNumberOfBioSignals() == null && drop.getNumberOfBioSignals() != null) {
            keep.setNumberOfBioSignals(drop.getNumberOfBioSignals());
        }

        // Merge observed genus/species if present
        if (drop.getObservedGenusPrefixes() != null) {
            for (String g : drop.getObservedGenusPrefixes()) {
                keep.addObservedGenus(g);
            }
        }
        if (drop.getObservedBioDisplayNames() != null) {
            for (String n : drop.getObservedBioDisplayNames()) {
                keep.addObservedBioDisplayName(n);
            }
        }

        // Keep predictions if keep doesn't have them yet
        if ((keep.getPredictions() == null || keep.getPredictions().isEmpty())
                && drop.getPredictions() != null
                && !drop.getPredictions().isEmpty()) {
            keep.setPredictions(drop.getPredictions());
        }
    }


    public void applyUiFontPreferences() {
        applyUiFont(OverlayPreferences.getUiFont());
    }

    public void applyUiFont(Font font) {
        if (font == null) {
            return;
        }

        uiFont = font;

        // Apply recursively so all labels/etc. stay consistent.
        applyFontRecursively(this, uiFont);

        if (headerLabel != null) {
            headerLabel.setFont(uiFont.deriveFont(Font.BOLD));
        }
        if (headerSummaryLabel != null) {
            headerSummaryLabel.setFont(uiFont.deriveFont(Font.BOLD));
        }
        if (table != null) {
            table.setFont(uiFont);
            if (table.getTableHeader() != null) {
                table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));
            }
        }
        revalidate();
        repaint();
    }

    private static void applyFontRecursively(Component c, Font font) {
        if (c == null || font == null) {
            return;
        }

        try {
            c.setFont(font);
        } catch (Exception e) {
            // ignore
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                applyFontRecursively(child, font);
            }
        }
    }

}
