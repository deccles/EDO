package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.FsdJumpEvent;
import org.dce.ed.logreader.EliteLogEvent.LocationEvent;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent;

/**
 * System tab – shows system bodies based on local journal events,
 * with exobiology predictions and a cache-backed preload using SystemCache.
 *
 * Bodies are cached on disk via {@link SystemCache}, so that
 * we can re-load system info when ED isn't running.
 */
public class SystemTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color ED_ORANGE = new Color(255, 140, 0);

    private final JTable table;
    private final JLabel headerLabel;
    private final SystemBodiesTableModel tableModel;
    private final SystemTracker tracker;

    public SystemTabPanel() {
        super(new BorderLayout());

        setOpaque(false);

        headerLabel = new JLabel("Waiting for system data…");
        headerLabel.setForeground(ED_ORANGE);
        headerLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));

        tableModel = new SystemBodiesTableModel();
        table = new JTable(tableModel);
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setPreferredScrollableViewportSize(new Dimension(500, 300));

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setForeground(ED_ORANGE);
        header.setBackground(Color.BLACK);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
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
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setHorizontalAlignment(LEFT);
                label.setBorder(new EmptyBorder(0, 4, 0, 4));

                return label;
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
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                c.setForeground(ED_ORANGE);
                if (isSelected) {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);

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
        add(headerLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Column widths
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(40);   // Body
        columns.getColumn(1).setPreferredWidth(60);   // g
        columns.getColumn(2).setPreferredWidth(220);  // Atmosphere / Type
        columns.getColumn(3).setPreferredWidth(140);  // Bio
        columns.getColumn(4).setPreferredWidth(100);  // Value
        columns.getColumn(5).setPreferredWidth(60);   // Land
        columns.getColumn(6).setPreferredWidth(80);   // Dist (Ls)

        tracker = new SystemTracker();

        // On startup, try to preload the current system from cache using Status.json
        refreshFromCache();
    }

    /**
     * Called by EliteOverlayTabbedPane's LiveJournalMonitor wiring.
     */
    public void handleLogEvent(EliteLogEvent event) {
        if (event != null) {
            tracker.handleEvent(event);
        }
    }

    /**
     * On startup (or when explicitly invoked), try to infer the *current* system
     * from Status.json and load that system's bodies from SystemCache.
     *
     * If the system is in the cache (from your rescan tool or live caching),
     * the System tab will immediately show those bodies even before any new
     * journal events arrive.
     */
    public void refreshFromCache() {
        try {
            // Look at the last journal file and figure out where we are now
            EliteJournalReader reader = new EliteJournalReader();
            List<EliteLogEvent> events = reader.readEventsFromLatestJournal();

            String systemName = null;
            long systemAddress = 0L;

            for (EliteLogEvent event : events) {
                if (event instanceof LocationEvent) {
                    LocationEvent e = (LocationEvent) event;
                    systemName = e.getStarSystem();
                    systemAddress = e.getSystemAddress();
                } else if (event instanceof FsdJumpEvent) {
                    FsdJumpEvent e = (FsdJumpEvent) event;
                    systemName = e.getStarSystem();
                    systemAddress = e.getSystemAddress();
                } else if (event instanceof EliteLogEvent.FssDiscoveryScanEvent) {
                    EliteLogEvent.FssDiscoveryScanEvent e = (EliteLogEvent.FssDiscoveryScanEvent) event;
                    if (systemName == null) {
                        systemName = e.getSystemName();
                    }
                    if (systemAddress == 0L) {
                        systemAddress = e.getSystemAddress();
                    }
                }
            }

            // If we couldn't find *any* system info, just bail quietly
            if (systemName == null && systemAddress == 0L) {
                return;
            }

            // Use the cache to get the full body list for that system
            SystemCache cache = SystemCache.getInstance();
            SystemCache.CachedSystem cs = cache.get(systemAddress, systemName);

            if (cs != null) {
                // Populate from cache (this fills bodies + header)
                tracker.loadFromCache(cs);
            } else {
                // At least update the header with the current system
                tracker.setCurrentSystem(systemName, systemAddress);
            }
        } catch (Exception e) {
            // Best-effort only; failure here should not break the overlay
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    //  Internal model
    // ---------------------------------------------------------------------

    class BodyInfo {
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

        // Additional fields for exobiology prediction / detail rows
        // These are only populated for main body rows (detailRow == false)
        String planetClass;
        String atmosphere;
        Double surfaceTempK;
        boolean hasVolcanism;
        String volcanismType;

        // If true, this is a synthetic "detail" row (e.g. a BioCandidate)
        boolean detailRow = false;
        int parentBodyId = -1;
        String bioDetailText;
        String bioDetailValueText;

        List<ExobiologyData.BioCandidate> bioCandidates;
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
                info.planetClass = e.getPlanetClass();
                info.atmosphere = e.getAtmosphere();
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

            // New system – clear everything
            systemName = name;
            systemAddress = addr;
            totalBodies = null;
            nonBodyCount = null;
            fssProgress = null;
            bodies.clear();
        }

        private boolean isBeltOrRing(String bodyName) {
            if (bodyName == null) {
                return false;
            }
            String n = bodyName.toLowerCase(Locale.ROOT);
            return n.contains("belt cluster")
                    || n.contains("ring")
                    || n.contains("belt ");
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

        void refreshTable() {
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

            // Build flattened view: each real body row plus optional Bio detail rows
            List<BodyInfo> sortedBodies = new ArrayList<>(bodies.values());
            sortedBodies.sort(Comparator.comparingDouble(b -> {
                if (Double.isNaN(b.distanceLs)) {
                    return Double.MAX_VALUE;
                }
                return b.distanceLs;
            }));

            List<BodyInfo> rows = new ArrayList<>();

            for (BodyInfo b : sortedBodies) {
                // Main body row
                b.detailRow = false;
                b.parentBodyId = -1;
                rows.add(b);

                if (!b.hasBio) {
                    continue;
                }

                // Ensure we have prediction candidates if possible
                if (b.bioCandidates == null) {
                    ExobiologyData.BodyAttributes attrs = buildBodyAttributes(b);
                    if (attrs != null) {
                        List<ExobiologyData.BioCandidate> preds = ExobiologyData.predictGenera(attrs);
                        if (preds != null && !preds.isEmpty()) {
                            b.bioCandidates = preds;
                        } else {
                            b.bioCandidates = new ArrayList<>();
                        }
                    } else {
                        b.bioCandidates = new ArrayList<>();
                    }
                }

                if (b.bioCandidates != null && !b.bioCandidates.isEmpty()) {
                    for (ExobiologyData.BioCandidate cand : b.bioCandidates) {
                        BodyInfo detail = new BodyInfo();
                        detail.detailRow = true;
                        detail.parentBodyId = b.bodyId;
                        detail.bioDetailText = cand.getDisplayName();
                        long baseVal = cand.getBaseValue();
                        detail.bioDetailValueText = String.format(Locale.US, "%,d Cr", baseVal);
                        rows.add(detail);
                    }
                } else {
                    BodyInfo detail = new BodyInfo();
                    detail.detailRow = true;
                    detail.parentBodyId = b.bodyId;
                    detail.bioDetailText = "Biological signals detected";
                    detail.bioDetailValueText = "";
                    rows.add(detail);
                }
            }

            tableModel.setBodies(rows);
            
            if (systemName != null && systemAddress != 0L && !bodies.isEmpty()) {
                try {
                    List<SystemCache.CachedBody> cachedBodies = toCachedBodies();
                    SystemCache.getInstance().put(systemAddress, systemName, cachedBodies);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private ExobiologyData.BodyAttributes buildBodyAttributes(BodyInfo b) {
            String planetClass = b.planetClass;
            String atmosphere = b.atmosphere;
            double gravityG = Double.NaN;
            if (b.gravityMS != null && !Double.isNaN(b.gravityMS)) {
                gravityG = b.gravityMS / 9.80665;
            }

            if ((planetClass == null || planetClass.isEmpty()) &&
                (atmosphere == null || atmosphere.isEmpty()) &&
                Double.isNaN(gravityG)) {
                return null;
            }

            // Temperature / volcanism left as defaults for now.
            return new ExobiologyData.BodyAttributes(
                    planetClass,
                    gravityG,
                    atmosphere,
                    Double.NaN,
                    false,
                    null
            );
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
                info.planetClass = cb.planetClass;
                info.atmosphere = cb.atmosphere;
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
                cb.planetClass = b.planetClass;
                cb.atmosphere = b.atmosphere;
                list.add(cb);
            }
            return list;
        }

        private String computeShortName(String name) {
            if (name == null) {
                return "";
            }
            int idx = name.lastIndexOf(' ');
            if (idx >= 0 && idx + 1 < name.length()) {
                return name.substring(idx + 1);
            }
            return name;
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
            if (tf.contains("terraformable")) {
                return true;
            }
            return false;
        }
    }

    // ---------------------------------------------------------------------
    //  Table model
    // ---------------------------------------------------------------------

    class SystemBodiesTableModel extends AbstractTableModel {

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

            // Detail rows: only Bio + Value columns are populated
            if (b.detailRow) {
                switch (columnIndex) {
                    case 3:
                        return b.bioDetailText != null ? b.bioDetailText : "";
                    case 4:
                        return b.bioDetailValueText != null ? b.bioDetailValueText : "";
                    default:
                        return "";
                }
            }

            // Normal body rows
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
