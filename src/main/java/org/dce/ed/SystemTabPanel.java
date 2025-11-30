package org.dce.ed;

import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent;
import org.dce.ed.logreader.LiveJournalMonitor;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.border.EmptyBorder;
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
 * System tab – shows system bodies based on journal events.
 */
public class SystemTabPanel extends JPanel {

    // ED-style orange
    private static final Color ED_ORANGE = new Color(255, 140, 0);

    private final JLabel headerLabel;
    private final SystemBodiesTableModel tableModel;
    private final JTable table;

    private final SystemTracker tracker = new SystemTracker();

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

        // Transparent, orange header
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

        // Hide scroll bar (mouse wheel still works)
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        if (vBar != null) {
            vBar.setPreferredSize(new Dimension(0, 0));
        }

        add(scrollPane, BorderLayout.CENTER);

        // Preload last 2 journal files to reconstruct current system on startup
        preloadFromHistory();

        // Then start listening for live events
        LiveJournalMonitor.getInstance().addListener(this::onLogEvent);
    }

    private void preloadFromHistory() {
        try {
            EliteJournalReader reader = new EliteJournalReader();
            for (EliteLogEvent event : reader.readEventsFromLastNJournalFiles(2)) {
                tracker.handleEvent(event);
            }
        } catch (IOException | IllegalStateException ex) {
            // If it fails, we just rely on live tailing.
        }
    }

    private void onLogEvent(EliteLogEvent event) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onLogEvent(event));
            return;
        }
        tracker.handleEvent(event);
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

    private final class SystemTracker {

        String systemName = null;
        long systemAddress = 0L;
        Integer totalBodies = null;
        Integer nonBodyCount = null;
        Double fssProgress = null;

        final Map<Integer, BodyInfo> bodies = new HashMap<>();

        void handleEvent(EliteLogEvent event) {
            if (event instanceof EliteLogEvent.LocationEvent) {
                EliteLogEvent.LocationEvent e = (EliteLogEvent.LocationEvent) event;
                enterSystem(e.getStarSystem(), e.getSystemAddress());

            } else if (event instanceof EliteLogEvent.FsdJumpEvent) {
                EliteLogEvent.FsdJumpEvent e = (EliteLogEvent.FsdJumpEvent) event;
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
