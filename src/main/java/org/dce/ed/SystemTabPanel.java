package org.dce.ed;

import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.FsdJumpEvent;
import org.dce.ed.logreader.EliteLogEvent.LocationEvent;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * System tab – shows system bodies based on local journal events,
 * with a fallback to EDSM lookup when we don't have enough history.
 *
 * Bodies are also cached on disk via {@link SystemCache} so we can
 * instantly populate systems we've seen before.
 */
public class SystemTabPanel extends JPanel {

    // Close to the default ED HUD orange
    private static final Color ED_ORANGE = new Color(255, 140, 0);

    private final JLabel headerLabel;
    private final SystemBodiesTableModel tableModel;
    private final JTable table;

    final SystemTracker tracker = new SystemTracker();
    private final SystemCache cache = SystemCache.getInstance();
    private final EdsmClient edsmClient = new EdsmClient();

    public SystemTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        headerLabel = new JLabel("Waiting for system data…");
        headerLabel.setForeground(ED_ORANGE);
        headerLabel.setBorder(new EmptyBorder(4, 6, 4, 6));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(headerLabel, BorderLayout.NORTH);

        tableModel = new SystemBodiesTableModel();
        table = new JTable(tableModel);
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setForeground(ED_ORANGE);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setRowHeight(table.getRowHeight() + 2);
        table.setIntercellSpacing(new Dimension(4, 1));
        table.setFont(table.getFont().deriveFont(12f));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

        // Transparent, orange header row
        JTableHeader header = table.getTableHeader();
        header.setOpaque(false);
        header.setForeground(ED_ORANGE);
        header.setBackground(new Color(0, 0, 0, 0));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setOpaque(false);
                setForeground(ED_ORANGE);
                setBorder(new EmptyBorder(2, 4, 2, 4));
            }

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                setText(value == null ? "" : value.toString());
                return this;
            }
        });

        // Transparent cells
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
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);

        TableColumnModel cols = table.getColumnModel();
        if (cols.getColumnCount() >= 7) {
            cols.getColumn(0).setPreferredWidth(60);  // Body
            cols.getColumn(1).setPreferredWidth(55);  // g
            cols.getColumn(2).setPreferredWidth(140); // Atmosphere / Type
            cols.getColumn(3).setPreferredWidth(70);  // Bio
            cols.getColumn(4).setPreferredWidth(60);  // Value
            cols.getColumn(5).setPreferredWidth(55);  // Landable
            cols.getColumn(6).setPreferredWidth(80);  // Dist
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(new EmptyBorder(4, 4, 4, 4));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Hide scrollbar (mouse wheel still works)
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        if (vBar != null) {
            vBar.setPreferredSize(new Dimension(0, 0));
        }

        add(scrollPane, BorderLayout.CENTER);

        // On startup, try to reconstruct the current system using:
        //  1) local cache (previous session)
        //  2) recent journal events
        //  3) EDSM lookup
        preloadSystem();
    }

    /**
     * Call this from your live log-tail code whenever a new event arrives
     * so the System tab can stay up to date while the game runs.
     */
    public void handleLogEvent(EliteLogEvent event) {
        if (event == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> handleLogEvent(event));
            return;
        }
        tracker.handleEvent(event);
        cacheCurrentSystem();
    }

    private void preloadSystem() {
        EliteJournalReader reader;
            reader = new EliteJournalReader();

        String systemName = null;
        long systemAddress = 0L;

        try {
            List<EliteLogEvent> events = reader.readEventsFromLatestJournal();
            // Walk backwards to find the last Location/FSD Jump
            for (int i = events.size() - 1; i >= 0; i--) {
                EliteLogEvent e = events.get(i);
                if (e instanceof LocationEvent) {
                    LocationEvent le = (LocationEvent) e;
                    systemName = le.getStarSystem();
                    systemAddress = le.getSystemAddress();
                    break;
                } else if (e instanceof FsdJumpEvent) {
                    FsdJumpEvent fe = (FsdJumpEvent) e;
                    systemName = fe.getStarSystem();
                    systemAddress = fe.getSystemAddress();
                    break;
                }
            }
        } catch (IOException ex) {
            headerLabel.setText("Could not read latest journal.");
            return;
        }

        if (systemName == null && systemAddress == 0L) {
            headerLabel.setText("Current system unknown.");
            return;
        }

        tracker.setCurrentSystem(systemName, systemAddress);

        // 1) Try cache
        SystemCache.CachedSystem cached = cache.get(systemAddress, systemName);
        if (cached != null && cached.bodies != null && !cached.bodies.isEmpty()) {
            tracker.loadFromCache(cached);
            return;
        }

        // 2) Try to reconstruct from local journal events (best-effort)
        boolean builtFromJournal = buildFromJournalHistory(reader, systemName, systemAddress);
        if (builtFromJournal) {
            cacheCurrentSystem();
            return;
        }

        // 3) Fallback to EDSM lookup
        loadFromEdsm(systemName);
    }

    private boolean buildFromJournalHistory(EliteJournalReader reader, String systemName, long systemAddress) {
        try {
            // For now we only use the latest journal file; if you want to
            // be more aggressive you can add a "readEventsFromLastNJournalFiles"
            // method to EliteJournalReader and iterate over more history.
            List<EliteLogEvent> events = reader.readEventsFromLatestJournal();
            for (EliteLogEvent event : events) {
                tracker.handleEvent(event);
            }
            // If we saw any bodies, consider it a success.
            return tracker.getBodyCount() > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    private void loadFromEdsm(String systemName) {
        if (systemName == null || systemName.isEmpty()) {
            return;
        }

        new Thread(() -> {
            List<EdsmClient.EdsmBody> bodies = edsmClient.fetchSystemBodies(systemName);
            if (bodies == null || bodies.isEmpty()) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                tracker.loadFromEdsm(systemName, bodies);
                cacheCurrentSystem();
            });
        }, "EDSM-SystemLookup").start();
    }

    private void cacheCurrentSystem() {
        if (tracker.systemName == null && tracker.systemAddress == 0L) {
            return;
        }
        List<SystemCache.CachedBody> cachedBodies = tracker.toCachedBodies();
        cache.put(tracker.systemAddress, tracker.systemName, cachedBodies);
    }

    /* ---------- Tracking + table model ---------- */

    private static final class BodyInfo {
        String name;
        String shortName;
        int bodyId = -1;
        double distanceLs = Double.NaN;
        Double gravityMS = null;
        boolean landable = false;
        boolean hasBio = false;
        boolean hasGeo = false;
        boolean highValue = false;
        String atmoOrType = "";
    }

    final class SystemTracker {

        String systemName = null;
        long systemAddress = 0L;
        Integer totalBodies = null;
        Integer nonBodyCount = null;
        Double fssProgress = null;

        final Map<Integer, BodyInfo> bodies = new HashMap<>();

        void setCurrentSystem(String name, long addr) {
            this.systemName = name;
            this.systemAddress = addr;
            refreshTable();
        }

        void handleEvent(EliteLogEvent event) {
            if (event instanceof LocationEvent) {
                LocationEvent e = (LocationEvent) event;
                enterSystem(e.getStarSystem(), e.getSystemAddress());
            } else if (event instanceof FsdJumpEvent) {
                FsdJumpEvent e = (FsdJumpEvent) event;
                enterSystem(e.getStarSystem(), e.getSystemAddress());
            } else if (event instanceof EliteLogEvent.FssDiscoveryScanEvent) {
                EliteLogEvent.FssDiscoveryScanEvent e = (EliteLogEvent.FssDiscoveryScanEvent) event;
                if (systemName == null) {
                    systemName = e.getSystemName();
                }
                if (e.getSystemAddress() != 0L) {
                    systemAddress = e.getSystemAddress();
                }
                fssProgress = e.getProgress();
                totalBodies = e.getBodyCount();
                nonBodyCount = e.getNonBodyCount();
            } else if (event instanceof EliteLogEvent.ScanEvent) {
                EliteLogEvent.ScanEvent e = (EliteLogEvent.ScanEvent) event;

                // Skip belts / rings
                if (isBeltOrRing(e.getBodyName())) {
                    return;
                }

                BodyInfo info = bodies.computeIfAbsent(e.getBodyId(), id -> new BodyInfo());
                info.bodyId = e.getBodyId();
                info.name = e.getBodyName();
                info.shortName = computeShortName(e.getBodyName());
                info.distanceLs = e.getDistanceFromArrivalLs();
                info.landable = e.isLandable();
                info.gravityMS = e.getSurfaceGravity();
                info.atmoOrType = chooseAtmoOrType(e);
                info.highValue = isHighValue(e);
            } else if (event instanceof SaasignalsFoundEvent) {
                SaasignalsFoundEvent e = (SaasignalsFoundEvent) event;
                handleSignals(e.getBodyId(), e.getSignals());
            } else if (event instanceof EliteLogEvent.FssBodySignalsEvent) {
                EliteLogEvent.FssBodySignalsEvent e = (EliteLogEvent.FssBodySignalsEvent) event;
                handleSignals(e.getBodyId(), e.getSignals());
            }

            refreshTable();
        }

        private void enterSystem(String name, long addr) {
            boolean sameName = (name != null && name.equals(systemName));
            boolean sameAddr = (addr != 0L && addr == systemAddress);

            if (sameName || sameAddr) {
                // Same system – keep existing body info, just update address/name
                if (name != null) {
                    systemName = name;
                }
                if (addr != 0L) {
                    systemAddress = addr;
                }
                return;
            }

            systemName = name;
            systemAddress = addr;
            totalBodies = null;
            nonBodyCount = null;
            fssProgress = null;
            bodies.clear();
        }

        private void handleSignals(int bodyId, List<SaasignalsFoundEvent.Signal> signals) {
            if (bodyId < 0 || signals == null || signals.isEmpty()) {
                return;
            }
            BodyInfo info = bodies.computeIfAbsent(bodyId, id -> new BodyInfo());
            for (SaasignalsFoundEvent.Signal s : signals) {
                String type = toLower(s.getType());
                String loc = toLower(s.getTypeLocalised());
                if (type.contains("biological") || loc.contains("biological")) {
                    info.hasBio = true;
                } else if (type.contains("geological") || loc.contains("geological")) {
                    info.hasGeo = true;
                }
            }
        }

        private String chooseAtmoOrType(EliteLogEvent.ScanEvent e) {
            String atmo = e.getAtmosphere();
            if (atmo != null && !atmo.isEmpty()) {
                return atmo;
            }
            String pc = e.getPlanetClass();
            if (pc != null && !pc.isEmpty()) {
                return pc;
            }
            String star = e.getStarType();
            return star != null ? star : "";
        }

        private boolean isHighValue(EliteLogEvent.ScanEvent e) {
            String pc = toLower(e.getPlanetClass());
            String tf = toLower(e.getTerraformState());
            if (pc.contains("earth-like")) {
                return true;
            }
            if (pc.contains("water world")) {
                return true;
            }
            if (pc.contains("ammonia world")) {
                return true;
            }
            return tf.contains("terraformable");
        }

        /**
         * Strip the system name prefix from the body name.
         * Main star becomes "*".
         */
        private String computeShortName(String bodyName) {
            if (bodyName == null || bodyName.isEmpty()) {
                return "";
            }
            if (systemName != null && bodyName.startsWith(systemName)) {
                String remainder = bodyName.substring(systemName.length()).trim();
                if (remainder.isEmpty()) {
                    return "*"; // primary star
                }
                return remainder;
            }
            return bodyName;
        }

        private boolean isBeltOrRing(String bodyName) {
            if (bodyName == null) {
                return false;
            }
            String lower = bodyName.toLowerCase(Locale.ROOT);
            return lower.contains("belt") || lower.contains("ring");
        }

        private void refreshTable() {
            StringBuilder sb = new StringBuilder();
            if (systemName != null && !systemName.isEmpty()) {
                sb.append(systemName);
            } else {
                sb.append("Current system");
            }

            if (totalBodies != null) {
                int scanned = bodies.size();
                sb.append("  |  Bodies: ").append(scanned).append(" of ").append(totalBodies);
                if (fssProgress != null) {
                    sb.append("  (").append(Math.round(fssProgress * 100.0)).append("%)");
                }
            }

            if (nonBodyCount != null) {
                sb.append("  |  Non-bodies: ").append(nonBodyCount);
            }

            if (fssProgress != null && fssProgress < 0.999) {
                sb.append("  |  Incomplete");
            }

            headerLabel.setText(sb.toString());

            List<BodyInfo> rows = new ArrayList<>(bodies.values());
            rows.sort(Comparator.comparingDouble(b -> {
                if (Double.isNaN(b.distanceLs)) {
                    return Double.MAX_VALUE;
                }
                return b.distanceLs;
            }));

            tableModel.setBodies(rows);
        }

        private String toLower(String s) {
            return s == null ? "" : s.toLowerCase(Locale.ROOT);
        }

        int getBodyCount() {
            return bodies.size();
        }

        void loadFromCache(SystemCache.CachedSystem cs) {
            if (cs == null || cs.bodies == null) {
                return;
            }
            this.systemName = cs.systemName;
            this.systemAddress = cs.systemAddress;

            bodies.clear();
            for (SystemCache.CachedBody cb : cs.bodies) {
                BodyInfo info = new BodyInfo();
                info.name = cb.name;
                info.shortName = computeShortName(cb.name);
                info.bodyId = cb.bodyId;
                info.distanceLs = cb.distanceLs;
                info.gravityMS = cb.gravityMS;
                info.landable = cb.landable;
                info.hasBio = cb.hasBio;
                info.hasGeo = cb.hasGeo;
                info.highValue = cb.highValue;
                info.atmoOrType = cb.atmoOrType;
                bodies.put(info.bodyId, info);
            }
            refreshTable();
        }

        void loadFromEdsm(String systemNameFromEdsm, List<EdsmClient.EdsmBody> edsmBodies) {
            if (edsmBodies == null) {
                return;
            }
            if (systemNameFromEdsm != null && !systemNameFromEdsm.isEmpty()) {
                this.systemName = systemNameFromEdsm;
            }

            bodies.clear();
            for (EdsmClient.EdsmBody eb : edsmBodies) {
                BodyInfo info = new BodyInfo();
                info.name = eb.name;
                info.shortName = computeShortName(eb.name);
                info.bodyId = eb.bodyId;
                info.distanceLs = eb.distanceToArrival;
                info.gravityMS = eb.gravity;
                info.landable = eb.landable;
                info.hasBio = eb.hasBio;
                info.hasGeo = eb.hasGeo;
                info.highValue = eb.highValue;
                if (eb.atmosphereType != null && !eb.atmosphereType.isEmpty()) {
                    info.atmoOrType = eb.atmosphereType;
                } else {
                    info.atmoOrType = eb.subType != null ? eb.subType : "";
                }
                bodies.put(info.bodyId, info);
            }
            refreshTable();
        }

        List<SystemCache.CachedBody> toCachedBodies() {
            List<SystemCache.CachedBody> list = new ArrayList<>();
            for (BodyInfo b : bodies.values()) {
                SystemCache.CachedBody cb = new SystemCache.CachedBody();
                cb.name = b.name;
                cb.bodyId = b.bodyId;
                cb.distanceLs = b.distanceLs;
                cb.gravityMS = b.gravityMS;
                cb.landable = b.landable;
                cb.hasBio = b.hasBio;
                cb.hasGeo = b.hasGeo;
                cb.highValue = b.highValue;
                cb.atmoOrType = b.atmoOrType;
                list.add(cb);
            }
            return list;
        }
    }

    private static final class SystemBodiesTableModel extends AbstractTableModel {

        private final String[] columns = {
                "Body",
                "g",
                "Atmosphere / Type",
                "Bio",
                "Value",
                "Land",
                "Dist (Ls)"
        };

        private final List<BodyInfo> bodies = new ArrayList<>();

        void setBodies(List<BodyInfo> newBodies) {
            bodies.clear();
            if (newBodies != null) {
                bodies.addAll(newBodies);
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return bodies.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BodyInfo b = bodies.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return b.shortName != null ? b.shortName : "";
                case 1:
                    if (b.gravityMS == null) {
                        return "";
                    }
                    double g = b.gravityMS / 9.80665;
                    return String.format(Locale.US, "%.2f g", g);
                case 2:
                    return b.atmoOrType != null ? b.atmoOrType : "";
                case 3:
                    if (b.hasBio && b.hasGeo) {
                        return "Bio + Geo";
                    } else if (b.hasBio) {
                        return "Bio";
                    } else if (b.hasGeo) {
                        return "Geo";
                    }
                    return "";
                case 4:
                    return b.highValue ? "High" : "";
                case 5:
                    return b.landable ? "Yes" : "";
                case 6:
                    if (Double.isNaN(b.distanceLs)) {
                        return "";
                    }
                    return String.format(Locale.US, "%.0f Ls", b.distanceLs);
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
