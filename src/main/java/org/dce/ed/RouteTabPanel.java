package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.dce.ed.cache.CachedSystem;
import org.dce.ed.cache.SystemCache;
import org.dce.ed.edsm.EdsmClient;
import org.dce.ed.edsm.EdsmClient.BodiesScanInfo;
import org.dce.ed.edsm.EdsmClient.BodyDiscoveryInfo;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.FsdTargetEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteClearEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteEvent;
import org.dce.ed.logreader.EliteLogFileLocator;
import org.dce.ed.state.SystemState;
import org.dce.ed.ui.SystemTableHoverCopyManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Route tab that visualizes the current plotted route from NavRoute.json.
 * This is styled to match the Elite Dangerous UI and SystemTabPanel for the
 * overlay: the panel and scrollpane are non-opaque, and all text uses the
 * same orange as SystemTabPanel.
 */
public class RouteTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color ED_ORANGE = new Color(255, 140, 0);
    private static final Color STATUS_GRAY = new Color(210, 210, 210);
    private static final Color STATUS_BLUE = new Color(100, 149, 237);
    private static final Color STATUS_YELLOW = new Color(255, 215, 0);

    // Orange / gray checkmarks for fully discovered systems.
    private static final Icon ICON_FULLY_DISCOVERED_VISITED =
            new StatusCircleIcon(ED_ORANGE, "\u2713");
    private static final Icon ICON_FULLY_DISCOVERED_NOT_VISITED =
            new StatusCircleIcon(STATUS_GRAY, "\u2713");

    // Crossed-out eye equivalents when any body is missing discovery.commander.
    // (Rendered as an X in a colored circle; you can swap to a real eye icon later.)
    private static final Icon ICON_DISCOVERY_MISSING_VISITED =
            new StatusCircleIcon(STATUS_BLUE, "X");
    private static final Icon ICON_DISCOVERY_MISSING_NOT_VISITED =
            new StatusCircleIcon(STATUS_GRAY, "X");

    // BodyCount mismatch between EDSM bodyCount and the number of bodies returned.
    private static final Icon ICON_BODYCOUNT_MISMATCH_VISITED =
            new StatusCircleIcon(STATUS_YELLOW, "!");
    private static final Icon ICON_BODYCOUNT_MISMATCH_NOT_VISITED =
            new StatusCircleIcon(STATUS_GRAY, "!");

    private static final Icon ICON_UNKNOWN =
            new StatusCircleIcon(STATUS_GRAY, "?");

    // Column indexes
    private static final int COL_MARKER    = 0;
    private static final int COL_INDEX    = 1;
    private static final int COL_SYSTEM   = 2;
    private static final int COL_CLASS    = 3;
    private static final int COL_STATUS   = 4;
    private static final int COL_DISTANCE = 5;

    private final JLabel headerLabel;
    private JTable table=null;
    private final RouteTableModel tableModel;
    private final EdsmClient edsmClient;

    private String currentSystemName = null;
    private String pendingJumpSystemName = null;
    private boolean jumpFlashOn = true;
    private final Timer jumpFlashTimer = new Timer(500, e -> {
        jumpFlashOn = !jumpFlashOn;
        table.repaint();
    });

    
    public RouteTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        this.edsmClient = new EdsmClient();

        headerLabel = new JLabel("Route: (no data)");
        headerLabel.setForeground(ED_ORANGE);
        headerLabel.setBorder(new EmptyBorder(4, 4, 4, 4));

        tableModel = new RouteTableModel();
        table = new JTable(tableModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setRowHeight(20);
        table.setForeground(ED_ORANGE);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setSelectionForeground(Color.BLACK);
        table.setSelectionBackground(new Color(255, 255, 255, 64));
        table.setFont(new Font("Dialog", Font.PLAIN, 12));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        table.getTableHeader().setForeground(ED_ORANGE);
        table.getTableHeader().setBackground(new Color(0, 0, 0, 0));
        table.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));

        // Default renderer that gives us consistent orange text + padding
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
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

        table.getColumnModel().getColumn(COL_MARKER).setCellRenderer(new MarkerRenderer());
        table.getColumnModel().getColumn(COL_MARKER).setMaxWidth(20);
        table.getColumnModel().getColumn(COL_MARKER).setPreferredWidth(20);
        
        // Status column uses a special renderer for the check / ? glyphs
        table.getColumnModel()
             .getColumn(COL_STATUS)
             .setCellRenderer(new StatusRenderer());

        // Distance column right-aligned
        DefaultTableCellRenderer distanceRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
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
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(new EmptyBorder(0, 4, 0, 8));
                }
                return c;
            }
        };
        table.getColumnModel()
             .getColumn(COL_DISTANCE)
             .setCellRenderer(distanceRenderer);

        // Copy-to-clipboard behavior on hover for the system name column,
        // consistent with SystemTabPanel.
        SystemTableHoverCopyManager systemTableHoverCopyManager = new SystemTableHoverCopyManager(table, COL_SYSTEM);
        systemTableHoverCopyManager.start();

        // Column widths
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(COL_INDEX).setPreferredWidth(40);   // #
        columns.getColumn(COL_SYSTEM).setPreferredWidth(260); // system name
        columns.getColumn(COL_CLASS).setPreferredWidth(40);   // class
        columns.getColumn(COL_STATUS).setPreferredWidth(40);  // check/? status
        columns.getColumn(COL_DISTANCE).setPreferredWidth(60); // Ly

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));

        add(headerLabel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            TableColumnModel cols = table.getColumnModel();
            cols.getColumn(COL_MARKER).setMinWidth(20);
            cols.getColumn(COL_MARKER).setMaxWidth(20);
            cols.getColumn(COL_MARKER).setPreferredWidth(20);
        });

        
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
            reloadFromNavRouteFile();
        }
        
        if (event instanceof EliteLogEvent.LocationEvent loc) {
            currentSystemName = loc.getStarSystem();
            pendingJumpSystemName = null;
        }

        if (event instanceof EliteLogEvent.FsdJumpEvent jump) {
            currentSystemName = jump.getStarSystem();
            pendingJumpSystemName = null;
        }

        if (event instanceof EliteLogEvent.StartJumpEvent sj) {
        	System.out.println("Start jump event!");
            pendingJumpSystemName = sj.getStarSystem();
            jumpFlashTimer.start();
        }

    }

    private void reloadFromNavRouteFile() {
        Path dir = OverlayPreferences.resolveJournalDirectory();
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

                    RouteEntry entry = new RouteEntry();
                    entry.index = entries.size();
                    entry.systemName    = systemName;
                    entry.systemAddress = systemAddress;
                    entry.starClass     = starClass;
                    entry.status        = ScanStatus.UNKNOWN;
                    entries.add(entry);

                    if (pos != null && pos.size() == 3) {
                        double x = pos.get(0).getAsDouble();
                        double y = pos.get(1).getAsDouble();
                        double z = pos.get(2).getAsDouble();
                        coords.add(new double[] { x, y, z });
                    } else {
                        coords.add(null);
                    }
                }

                // Compute per-jump distances (Ly) from the StarPos coordinates
                for (int i = 0; i < entries.size(); i++) {
                    if (i == 0) {
                        entries.get(i).distanceLy = null; // origin system
                    } else {
                        double[] prev = coords.get(i - 1);
                        double[] cur  = coords.get(i);
                        if (prev == null || cur == null) {
                            entries.get(i).distanceLy = null;
                        } else {
                            double dx = cur[0] - prev[0];
                            double dy = cur[1] - prev[1];
                            double dz = cur[2] - prev[2];
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            entries.get(i).distanceLy = dist;
                        }
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
        if (entry == null) {
            return;
        }
        boolean v = isVisited(entry);
        // First check local cache (if you wire this up)
        if (isLocallyFullyScanned(entry)) {
            entry.status = v ? ScanStatus.FULLY_DISCOVERED_VISITED : ScanStatus.FULLY_DISCOVERED_NOT_VISITED;
            SwingUtilities.invokeLater(() -> tableModel.fireRowChanged(row));
            return;
        }

        try {
            BodiesScanInfo info = edsmClient.fetchBodiesScanInfo(entry.systemName);

            ScanStatus newStatus = ScanStatus.UNKNOWN;


            if (info != null) {
                int returnedBodies = (info.bodies == null) ? 0 : info.bodies.size();
                Integer bodyCount = info.bodyCount;
                boolean hasBodyCount = (bodyCount != null && bodyCount > 0);

                // Mismatch if EDSM says there should be bodies, but the array size differs.
                // This covers the "bodyCount 19, bodies []" case explicitly.
                boolean bodyCountMismatch =
                        hasBodyCount && bodyCount.intValue() != returnedBodies;

                if (returnedBodies > 0 && !hasBodyCount) {
                	newStatus = v ? ScanStatus.DISCOVERY_MISSING_VISITED : ScanStatus.DISCOVERY_MISSING_NOT_VISITED;
                } else if (bodyCountMismatch) {
                    newStatus = v ? ScanStatus.BODYCOUNT_MISMATCH_VISITED : ScanStatus.BODYCOUNT_MISMATCH_NOT_VISITED;
                } else if (returnedBodies > 0) {
                    // We have bodies and bodyCount (if present) matches the array size.
                    newStatus = v? ScanStatus.FULLY_DISCOVERED_VISITED : ScanStatus.FULLY_DISCOVERED_NOT_VISITED;
                } else {
                    // No bodies and no usable bodyCount info: leave as UNKNOWN.
                    newStatus = ScanStatus.UNKNOWN;
                }
            }

            entry.status = newStatus;
            SwingUtilities.invokeLater(() -> tableModel.fireRowChanged(row));
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
    /**
     * Returns true if this system is fully scanned in our local cache
     * (all bodies known and FSS progress ~100%).
     */
    /**
     * Returns true if this system is fully scanned *by you* according to the
     * local cache.
     *
     * Uses the new SystemState fields:
     *   - allBodiesFound (from FSSAllBodiesFound)
     *   - totalBodies
     *   - fssProgress
     * and the number of cached bodies.
     */
    private boolean isLocallyFullyScanned(RouteEntry entry) {
        if (entry == null) {
            return false;
        }
        

        

        // Look up cached system by address/name (same pattern as SystemTabPanel)
        SystemCache cache = SystemCache.getInstance();
        CachedSystem cs = cache.get(entry.systemAddress, entry.systemName);
        if (cs == null) {
            // Nothing cached locally → definitely not "fully scanned by me"
            return false;
        }

        // Load into a temporary SystemState so we can inspect metadata
        SystemState tmp = new SystemState();
        cache.loadInto(tmp, cs);

        // 1) If we have an explicit "all bodies found" flag, trust that first.
        Boolean all = tmp.getAllBodiesFound();
        if (Boolean.TRUE.equals(all)) {
            return true;
        }

        // 2) Otherwise, fall back to counts / progress.
        Integer totalBodies = tmp.getTotalBodies();
        if (totalBodies == null || totalBodies == 0) {
            // We don't know how many bodies there should be; can't claim "fully scanned".
            return false;
        }

        int knownBodies = tmp.getBodies().size();
        if (knownBodies < totalBodies) {
            // We've seen some bodies but not all → not fully scanned.
            return false;
        }

        // If FSS progress exists, require it to be ~100%.
        Double progress = tmp.getFssProgress();
        if (progress != null && progress < 0.999) {
            return false;
        }

        // At this point, cache says we know all bodies and FSS is effectively complete.
        return true;
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

    /**
     * Returns true if the system for this route entry appears in the local cache.
     * This is the only "me-related" state: it means you have visited the system.
     */
    private boolean isVisited(RouteEntry entry) {
        if (entry == null) {
            return false;
        }
        SystemCache cache = SystemCache.getInstance();
        CachedSystem cs = cache.get(entry.systemAddress, entry.systemName);
        return cs != null;
    }

    
    // ---------------------------------------------------------------------
    // Model + table
    // ---------------------------------------------------------------------
    enum ScanStatus {
        // Any body missing discovery.commander and you have visited the system.
        DISCOVERY_MISSING_VISITED,
        // Any body missing discovery.commander and you have NOT visited the system.
        DISCOVERY_MISSING_NOT_VISITED,
        // EDSM bodyCount does not match the number of bodies returned, and you have visited.
        BODYCOUNT_MISMATCH_VISITED,
        // EDSM bodyCount does not match the number of bodies returned, and you have NOT visited.
        BODYCOUNT_MISMATCH_NOT_VISITED,
        // All bodies accounted for in EDSM and each has discovery.commander, and you have visited.
        FULLY_DISCOVERED_VISITED,
        // All bodies accounted for in EDSM and each has discovery.commander, and you have NOT visited.
        FULLY_DISCOVERED_NOT_VISITED,
        // Anything else / no data.
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
        private static final long serialVersionUID = 1L;

        private final List<RouteEntry> entries = new ArrayList<>();

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return 6; // +1 for marker column
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            	case COL_MARKER: 
            		return "";
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
            	case COL_MARKER:
            		return null;
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

    private static final class StatusCircleIcon implements Icon {

        private final int size;
        private final Color circleColor;
        private final String symbol;

        StatusCircleIcon(Color circleColor, String symbol) {
            this(circleColor, symbol, 14);
        }

        StatusCircleIcon(Color circleColor, String symbol, int size) {
            this.circleColor = circleColor;
            this.symbol = symbol;
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                int d = size - 1;
                g2.setColor(circleColor);
                g2.fillOval(x, y, d, d);

                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, d, d);

                if (symbol != null && !symbol.isEmpty()) {
                    Font font = c.getFont();
                    if (font != null) {
                        font = font.deriveFont(Font.BOLD,
                                               Math.max(10f, font.getSize2D()));
                        g2.setFont(font);
                    }
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(symbol);
                    int textAscent = fm.getAscent();
                    int tx = x + (size - textWidth) / 2;
                    int ty = y + (size + textAscent) / 2 - 2;
                    g2.drawString(symbol, tx, ty);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

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
            label.setText("");
            label.setIcon(null);
            
            if (value instanceof ScanStatus) {
                ScanStatus status = (ScanStatus) value;
                switch (status) {
                    case FULLY_DISCOVERED_VISITED:
                        label.setIcon(ICON_FULLY_DISCOVERED_VISITED);
                        break;
                    case FULLY_DISCOVERED_NOT_VISITED:
                        label.setIcon(ICON_FULLY_DISCOVERED_NOT_VISITED);
                        break;
                    case DISCOVERY_MISSING_VISITED:
                        label.setIcon(ICON_DISCOVERY_MISSING_VISITED);
                        break;
                    case DISCOVERY_MISSING_NOT_VISITED:
                        label.setIcon(ICON_DISCOVERY_MISSING_NOT_VISITED);
                        break;
                    case BODYCOUNT_MISMATCH_VISITED:
                        label.setIcon(ICON_BODYCOUNT_MISMATCH_VISITED);
                        break;
                    case BODYCOUNT_MISMATCH_NOT_VISITED:
                        label.setIcon(ICON_BODYCOUNT_MISMATCH_NOT_VISITED);
                        break;
                    case UNKNOWN:
                    default:
                        label.setIcon(ICON_UNKNOWN);
                        break;
                }
            }


            return label;
        }
    }
    private class MarkerRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            JLabel l = (JLabel) super.getTableCellRendererComponent(
                    table, "", false, false, row, column);
            l.setHorizontalAlignment(SwingConstants.CENTER);

            String system = (String) table.getValueAt(row, 2); // YOUR system column

            Icon icon = null;

            if (system != null) {
                if (system.equals(currentSystemName)) {
                    icon = new TriangleIcon(Color.ORANGE, 10, 10);
                } else if (system.equals(pendingJumpSystemName) && jumpFlashOn) {
                    icon = new TriangleIcon(Color.ORANGE, 10, 10);
                }
            }

            l.setIcon(icon);
            return l;
        }
    }
    private static class TriangleIcon implements Icon {
        private final Color color;
        private final int w, h;

        TriangleIcon(Color c, int w, int h) { this.color = c; this.w = w; this.h = h; }

        public int getIconWidth() { return w; }
        public int getIconHeight() { return h; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(color);
            int[] xs = { x, x, x + w };
            int[] ys = { y, y + h, y + h/2 };
            g2.fillPolygon(xs, ys, 3);
        }
    }

}
