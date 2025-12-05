package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.edsm.EdsmClient;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.FsdTargetEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteClearEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteEvent;
import org.dce.ed.logreader.EliteLogFileLocator;
import org.dce.ed.ui.SystemTableHoverCopyManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Route tab – shows the current plotted NavRoute.json as a table.
 *
 * Designed for the transparent overlay: the panel and scrollpane are
 * non-opaque, and all text uses the same orange as SystemTabPanel.
 */
public class RouteTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Same color as SystemTabPanel.ED_ORANGE. */
    private static final Color ED_ORANGE = new Color(255, 140, 0);

    // Column indexes
    private static final int COL_INDEX    = 0;
    private static final int COL_SYSTEM   = 1;
    private static final int COL_CLASS    = 2;
    private static final int COL_STATUS   = 3;
    private static final int COL_DISTANCE = 4;

    private final JLabel headerLabel;
    private final JTable table;
    private final RouteTableModel tableModel;

    private final EdsmClient edsmClient = new EdsmClient();

    public RouteTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        // Header row – simple summary of route
        headerLabel = new JLabel("No plotted route.");
        headerLabel.setOpaque(false);
        headerLabel.setForeground(ED_ORANGE);
        headerLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(headerLabel, BorderLayout.NORTH);

        // Table setup
        tableModel = new RouteTableModel();
        table = new JTable(tableModel);
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setForeground(ED_ORANGE);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setRowHeight(22);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        add(scroll, BorderLayout.CENTER);

        // Default renderer for most columns – transparent + orange text
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            {
                setOpaque(false);
                setForeground(ED_ORANGE);
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
                c.setForeground(ED_ORANGE);
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(new EmptyBorder(0, 4, 0, 4));
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, defaultRenderer);

        // Status column uses a special renderer for the check / ? glyphs
        table.getColumnModel()
             .getColumn(COL_STATUS)
             .setCellRenderer(new StatusRenderer());

        // Right-align the distance column
        DefaultTableCellRenderer distanceRenderer = new DefaultTableCellRenderer() {
            {
                setOpaque(false);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setForeground(ED_ORANGE);
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
                c.setForeground(ED_ORANGE);
                return c;
            }
        };
        table.getColumnModel().getColumn(COL_DISTANCE).setCellRenderer(distanceRenderer);

        SystemTableHoverCopyManager hoverManager =
                new SystemTableHoverCopyManager(table, 1);
        hoverManager.start();
        
        // Rough column widths similar to in-game layout
        TableColumnModel cols = table.getColumnModel();
        cols.getColumn(COL_INDEX).setPreferredWidth(32);
        cols.getColumn(COL_SYSTEM).setPreferredWidth(260);
        cols.getColumn(COL_CLASS).setPreferredWidth(40);
        cols.getColumn(COL_STATUS).setPreferredWidth(40);
        cols.getColumn(COL_DISTANCE).setPreferredWidth(80);
        
        reloadFromNavRouteFile();
    }

    /**
     * Entry point from LiveJournalMonitor.
     *
     * We only care about NavRoute / FSDTarget / NavRouteClear; they all
     * indicate the plotted route has changed and we should re-read
     * NavRoute.json from the journal directory.
     */
    public void handleLogEvent(EliteLogEvent event) {
        if (event == null) {
            return;
        }

        if (event instanceof NavRouteEvent
            || event instanceof FsdTargetEvent
            || event instanceof NavRouteClearEvent) {
        	System.out.println("Reloading");
            reloadFromNavRouteFile();
        }
    }

    private void reloadFromNavRouteFile() {
        Path dir = EliteLogFileLocator.findDefaultJournalDirectory();
        if (dir == null) {
            headerLabel.setText("No journal directory.");
            tableModel.setEntries(new ArrayList<>());
            return;
        }

        Path navRoute = dir.resolve("NavRoute.json");
        if (!Files.isRegularFile(navRoute)) {
            headerLabel.setText("No plotted route.");
            tableModel.setEntries(new ArrayList<>());
            return;
        }

        List<RouteEntry> entries = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(navRoute, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray route = root.getAsJsonArray("Route");
            if (route != null) {
                List<double[]> coords = new ArrayList<>();

                for (JsonElement elem : route) {
                    if (!elem.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = elem.getAsJsonObject();

                    String systemName    = safeString(obj, "StarSystem");
                    long systemAddress   = safeLong(obj, "SystemAddress");
                    String starClass     = safeString(obj, "StarClass");
                    JsonArray pos        = obj.getAsJsonArray("StarPos");

                    double x = 0;
                    double y = 0;
                    double z = 0;
                    if (pos != null && pos.size() == 3) {
                        x = pos.get(0).getAsDouble();
                        y = pos.get(1).getAsDouble();
                        z = pos.get(2).getAsDouble();
                    }
                    coords.add(new double[] { x, y, z });

                    RouteEntry entry = new RouteEntry();
                    entry.index         = entries.size();
                    entry.systemName    = systemName;
                    entry.systemAddress = systemAddress;
                    entry.starClass     = starClass;
                    entry.status        = ScanStatus.UNKNOWN;
                    entries.add(entry);
                }

                // Compute per-jump distances (Ly) from the StarPos coordinates
                for (int i = 0; i < entries.size(); i++) {
                    if (i == 0) {
                        entries.get(i).distanceLy = null; // origin system
                    } else {
                        double[] prev = coords.get(i - 1);
                        double[] cur  = coords.get(i);
                        double dx = cur[0] - prev[0];
                        double dy = cur[1] - prev[1];
                        double dz = cur[2] - prev[2];
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        entries.get(i).distanceLy = dist;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            headerLabel.setText("Error reading NavRoute.json");
            tableModel.setEntries(new ArrayList<>());
            return;
        }

        headerLabel.setText(entries.isEmpty()
                            ? "No plotted route."
                            : "Route: " + entries.size() + " systems");
        tableModel.setEntries(entries);

        // Async EDSM lookups to refine status icons
        for (int row = 0; row < entries.size(); row++) {
            final int r = row;
            final RouteEntry entry = entries.get(row);
            new Thread(() -> updateStatusFromEdsm(entry, r),
                       "RouteEdsm-" + entry.systemName).start();
        }
    }

    private void updateStatusFromEdsm(RouteEntry entry, int row) {
        // First check local cache (if you wire this up)
        if (isLocallyFullyScanned(entry)) {
            entry.status = ScanStatus.LOCAL_COMPLETE;
            SwingUtilities.invokeLater(() -> tableModel.fireRowChanged(row));
            return;
        }

        // Then ask EDSM
        try {
            BodiesResponse bodies = edsmClient.showBodies(entry.systemName);
            if (bodies != null && bodies.bodies != null && !bodies.bodies.isEmpty()) {
                entry.status = ScanStatus.EDSM_COMPLETE;
                SwingUtilities.invokeLater(() -> tableModel.fireRowChanged(row));
            }
        } catch (Exception e) {
            // Network / API errors – leave status as UNKNOWN
            e.printStackTrace();
        }
    }

    /**
     * Hook for your local cache: return true if you consider the system
     * fully scanned locally (all bodies discovered/mapped).
     *
     * Right now this is a stub so that only EDSM can produce a checkmark.
     * Replace with your own integration against SystemState / DB, etc.
     */
    private boolean isLocallyFullyScanned(RouteEntry entry) {
        // TODO: integrate with your SystemState / local cache
        return false;
    }

    private static String safeString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : "";
    }

    private static long safeLong(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        try {
            return (el != null && !el.isJsonNull()) ? el.getAsLong() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // ---------------------------------------------------------------------
    // Model + table
    // ---------------------------------------------------------------------

    private enum ScanStatus {
        LOCAL_COMPLETE,
        EDSM_COMPLETE,
        UNKNOWN
    }

    private static final class RouteEntry {
        int index;
        String systemName;
        long systemAddress;
        String starClass;
        Double distanceLy;
        ScanStatus status;
    }

    private static final class RouteTableModel extends AbstractTableModel {

        private final List<RouteEntry> entries = new ArrayList<>();

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_INDEX:
                    return "#";
                case COL_SYSTEM:
                    return "System";
                case COL_CLASS:
                    return "Class";
                case COL_STATUS:
                    return "";
                case COL_DISTANCE:
                    return "Ly";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RouteEntry e = entries.get(rowIndex);
            switch (columnIndex) {
                case COL_INDEX:
                    return Integer.valueOf(e.index);
                case COL_SYSTEM:
                    return e.systemName != null ? e.systemName : "";
                case COL_CLASS:
                    return e.starClass != null ? e.starClass : "";
                case COL_STATUS:
                    return e.status;
                case COL_DISTANCE:
                    if (e.distanceLy == null) {
                        return "";
                    }
                    return String.format("%.2f Ly", e.distanceLy.doubleValue());
                default:
                    return "";
            }
        }

        void setEntries(List<RouteEntry> newEntries) {
            entries.clear();
            if (newEntries != null) {
                entries.addAll(newEntries);
            }
            fireTableDataChanged();
        }

        void fireRowChanged(int row) {
            if (row >= 0 && row < entries.size()) {
                fireTableRowsUpdated(row, row);
            }
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {

        StatusRenderer() {
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                                                                        "",
                                                                        false,
                                                                        false,
                                                                        row,
                                                                        column);
            label.setBorder(new EmptyBorder(0, 0, 0, 0));

            if (value instanceof ScanStatus) {
                ScanStatus status = (ScanStatus) value;
                switch (status) {
                    case LOCAL_COMPLETE:
                        label.setText("\u2713"); // orange check
                        label.setForeground(ED_ORANGE);
                        break;
                    case EDSM_COMPLETE:
                        label.setText("\u2713"); // gray check
                        label.setForeground(Color.LIGHT_GRAY);
                        break;
                    case UNKNOWN:
                    default:
                        label.setText("?");
                        label.setForeground(ED_ORANGE);
                        break;
                }
            } else {
                label.setText("");
                label.setForeground(ED_ORANGE);
            }

            return label;
        }
    }
}
