package org.dce.ed;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
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
import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteClearEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteEvent;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FsdTargetEvent;
import org.dce.ed.logreader.event.FssAllBodiesFoundEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.logreader.event.StatusEvent;
import org.dce.ed.state.SystemState;
import org.dce.ed.ui.SystemTableHoverCopyManager;
import org.dce.ed.util.EdsmClient;

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


    private Font uiFont = OverlayPreferences.getUiFont();

    private static final Color ED_ORANGE = new Color(255, 140, 0);
    private static final Color ED_ORANGE_TRANS = new Color(255, 140, 0, 64);
    private static final Color ED_ORANGE_LESS_TRANS = new Color(255, 140, 0, 96);
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
    private String targetSystemName = null;  
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
        headerLabel.setFont(uiFont.deriveFont(Font.BOLD));

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
        table.setRowHeight(computeRowHeight(table, uiFont, 6));
        table.setForeground(ED_ORANGE);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setSelectionForeground(Color.BLACK);
        table.setSelectionBackground(new Color(255, 255, 255, 64));
        table.setFont(uiFont);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        table.getTableHeader().setForeground(ED_ORANGE);
        table.getTableHeader().setBackground(new Color(0, 0, 0, 0));
        table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));

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

                if (c instanceof JLabel) {
                    c.setForeground(ED_ORANGE);
                    // Add a bit of vertical padding for readability
                    ((JLabel) c).setBorder(new EmptyBorder(3, 4, 3, 4));
                }
                return c;
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                super.paintComponent(g2);

                // ED_ORANGE separator line at the bottom of each row
                g2.setColor(ED_ORANGE_TRANS);
                int y = getHeight() - 1;
                g2.drawLine(0, y, getWidth(), y);

                g2.dispose();
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
                    // Slight right padding for numbers
                    ((JLabel) c).setBorder(new EmptyBorder(3, 4, 3, 8));
                }
                return c;
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                super.paintComponent(g2);

                g2.setColor(ED_ORANGE_TRANS);
                int y = getHeight() - 1;
                g2.drawLine(0, y, getWidth(), y);

                g2.dispose();
            }
        };
        table.getColumnModel()
             .getColumn(COL_DISTANCE)
             .setCellRenderer(distanceRenderer);



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
        // Copy-to-clipboard behavior on hover for the system name column,
        // consistent with SystemTabPanel.
        SystemTableHoverCopyManager systemTableHoverCopyManager = new SystemTableHoverCopyManager(table, COL_SYSTEM);
        systemTableHoverCopyManager.start();
        
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
            || event instanceof NavRouteClearEvent) {
            reloadFromNavRouteFile();
        }
        if (event instanceof NavRouteClearEvent) {
            // Route cleared: no active FSD target anymore
            targetSystemName = null;
            table.repaint();
        }
        if (event instanceof FsdTargetEvent target) {
            // FSD target selected: remember the target system for the crosshair
            targetSystemName = target.getName();  // adjust to getStarSystem() if needed
            table.repaint();
        }
        
        
        if (event instanceof LocationEvent loc) {
            setCurrentSystemName(loc.getStarSystem());
            pendingJumpSystemName = null;
        }

        if (event instanceof FsdJumpEvent jump) {
            setCurrentSystemName(jump.getStarSystem());
            Long currentSystemAddress = jump.getSystemAddress();
            
            pendingJumpSystemName = null;
            
            if (jumpFlashTimer != null && jumpFlashTimer.isRunning()) {
    			System.out.println("Stop jump event!");
    			jumpFlashTimer.stop();
    			jumpFlashOn = false;
    		}
            setCurrentSystemIfEmpty(getCurrentSystemName(), currentSystemAddress);
        }
		if (event instanceof FssAllBodiesFoundEvent) {
			FssAllBodiesFoundEvent fss = (FssAllBodiesFoundEvent)event;
			
			reloadFromNavRouteFile();
		}
        if (event instanceof StatusEvent sj) {
        	StatusEvent se = (StatusEvent)sj;
        	boolean hyperdriveCharging = se.isFsdHyperdriveCharging();
        	boolean timerRunning = jumpFlashTimer.isRunning();
        	
        	if (hyperdriveCharging && !timerRunning) {
        		System.out.println("Start jump event! " + se.isFsdCharging() + " " + se.isFsdHyperdriveCharging());
        		pendingJumpSystemName = se.getDestinationName();
        		jumpFlashTimer.start();
        	} 
        	if (!hyperdriveCharging && timerRunning ){
        		jumpFlashTimer.stop();
        		jumpFlashOn = false;
        	}
        }

    }

    private void setCurrentSystemIfEmpty(String systemName, long systemAddress) {

        if (tableModel.getRowCount() > 0) {
            return; // route exists, nothing to do
        }
        RouteEntry entry = new RouteEntry(
                0,                    // index
                systemName,
                systemAddress,
                "?",                  // class until EDSM loads
                0.0,                   // Ly
                ScanStatus.UNKNOWN       // use whatever your enum is

        );
//        int index;
//        String systemName;
//        long systemAddress;
//        String starClass;
//        Double distanceLy;
//        ScanStatus status;
        
        
        List<RouteEntry> list = new ArrayList<>();
        list.add(entry);

        tableModel.setEntries(list);
        tableModel.fireTableDataChanged();
    }

    
    int getRowForSystem(String systemName) {
    	for (int row=0; row < table.getModel().getRowCount(); row++) {
    		String system = (String) table.getValueAt(row, COL_SYSTEM); // YOUR system column
    		if (system.equals(getCurrentSystemName())) {
    			return row;
    		}
    	}
    	return -1;
    }
    public void setDistanceSumMode(boolean sum) {
        tableModel.setSumDistances(sum);
    }

    public boolean isDistanceSumMode() {
        return tableModel.isSumDistances();
    }

    private void reloadFromNavRouteFile() {
        Path dir = OverlayPreferences.resolveJournalDirectory(EliteDangerousOverlay.clientKey);
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

                        entry.x = Double.valueOf(x);
                        entry.y = Double.valueOf(y);
                        entry.z = Double.valueOf(z);

                        coords.add(new double[] { x, y, z });
                    } else {
                        entry.x = null;
                        entry.y = null;
                        entry.z = null;

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

        // Prefer LOCAL scan state first (this matches in-game "Bodies: N of N Complete")
        ScanStatus local = getLocalScanStatus(entry);
        if (local != ScanStatus.UNKNOWN) {
            entry.status = local;
            SwingUtilities.invokeLater(() -> tableModel.fireRowChanged(row));
            return;
        }

        boolean v = isVisited(entry);

        try {
            BodiesResponse bodies = edsmClient.showBodies(entry.systemName);

            ScanStatus newStatus = ScanStatus.UNKNOWN;

            if (bodies != null && bodies.bodies != null) {
                int returnedBodies = bodies.bodies.size();
                boolean hasBodies = returnedBodies > 0;

                if (hasBodies) {
                    if (bodies.bodyCount != returnedBodies) {
                        newStatus = v
                                ? ScanStatus.BODYCOUNT_MISMATCH_VISITED
                                : ScanStatus.BODYCOUNT_MISMATCH_NOT_VISITED;
                    } else {
                        newStatus = v
                                ? ScanStatus.FULLY_DISCOVERED_VISITED
                                : ScanStatus.FULLY_DISCOVERED_NOT_VISITED;
                    }
                } else {
                    newStatus = ScanStatus.UNKNOWN;
                }
            }

            entry.status = newStatus;
            SwingUtilities.invokeLater(() -> tableModel.fireRowChanged(row));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private ScanStatus getLocalScanStatus(RouteEntry entry) {
        if (entry == null) {
            return ScanStatus.UNKNOWN;
        }

        SystemCache cache = SystemCache.getInstance();
        CachedSystem cs = cache.get(entry.systemAddress, entry.systemName);
        if (cs == null) {
            return ScanStatus.UNKNOWN; // not visited / no local info
        }

        SystemState tmp = new SystemState();
        cache.loadInto(tmp, cs);

        Boolean all = tmp.getAllBodiesFound();
        if (Boolean.TRUE.equals(all)) {
            return ScanStatus.FULLY_DISCOVERED_VISITED;
        }

        Integer totalBodies = tmp.getTotalBodies();
        int knownBodies = tmp.getBodies().size();

        // If we know the system body count and we don't have them all locally -> X
        if (totalBodies != null && totalBodies > 0 && knownBodies > 0 && knownBodies < totalBodies) {
            return ScanStatus.DISCOVERY_MISSING_VISITED;
        }

        // If we know counts match and FSS progress says complete -> checkmark
        Double progress = tmp.getFssProgress();
        if (totalBodies != null && totalBodies > 0 && knownBodies >= totalBodies
                && progress != null && progress >= 1.0) {
            return ScanStatus.FULLY_DISCOVERED_VISITED;
        }

        return ScanStatus.UNKNOWN;
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

//        // 2) Otherwise, fall back to counts / progress.
        Integer totalBodies = tmp.getTotalBodies();
//        if (totalBodies == null || totalBodies == 0) {
//            // We don't know how many bodies there should be; can't claim "fully scanned".
//            return false;
//        }

        int knownBodies = tmp.getBodies().size();
        if (totalBodies != null && knownBodies < totalBodies) {
            // We've seen some bodies but not all → not fully scanned.
            return false;
        }

        // If FSS progress exists, require it to be ~100%.
        Double progress = tmp.getFssProgress();
        if (progress != null && progress == 1.0) {// 0.999) {
            return true;
        }

//         At this point, cache says we know all bodies and FSS is effectively complete.
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

    
    private String getCurrentSystemName() {
        if (currentSystemName != null && !currentSystemName.isBlank()) {
            return currentSystemName;
        }

        // Best source: recent journals (works at startup, no live events required)
        String fromJournal = resolveCurrentSystemNameFromJournal();
        if (fromJournal != null && !fromJournal.isBlank()) {
            currentSystemName = fromJournal;
            return currentSystemName;
        }

        // Fallback: whatever SystemCache persisted last
        try {
            String fromCache = SystemCache.load().systemName;
            if (fromCache != null && !fromCache.isBlank()) {
                currentSystemName = fromCache;
                return currentSystemName;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Never return null (renderer comparisons should not explode or behave oddly)
        return "";
    }
    private String resolveCurrentSystemNameFromJournal() {
        try {
            EliteJournalReader reader = new EliteJournalReader(EliteDangerousOverlay.clientKey);

            String systemName = null;

            List<EliteLogEvent> events = reader.readEventsFromLastNJournalFiles(3);
            for (EliteLogEvent event : events) {
                if (event instanceof LocationEvent e) {
                    systemName = e.getStarSystem();
                } else if (event instanceof FsdJumpEvent e) {
                    systemName = e.getStarSystem();
                }
            }

            return systemName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setCurrentSystemName(String currentSystemName) {
        if (currentSystemName == null) {
            return;
        }

        this.currentSystemName = currentSystemName;
        tableModel.setCurrentSystemName(currentSystemName);
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
        public RouteEntry() {

        }

        public RouteEntry(int i, String systemNameIn, long systemAddressIn, String starClassIn, double dLy, ScanStatus scanStatusIn) {
            index = i;
            systemName = systemNameIn;
            systemAddress = systemAddressIn;
            starClass = starClassIn;
            distanceLy = dLy;
            status = scanStatusIn;
        }

        int index;
        String systemName;
        long systemAddress;
        String starClass;

        /**
         * StarPos coordinates (x,y,z) for this system, in Ly, when available (NavRoute.json provides these).
         * Used for "straight line" distance calculations.
         */
        Double x;
        Double y;
        Double z;

        /**
         * Per-leg distance (Ly) from the previous entry to this entry.
         * Null for the origin row.
         */
        Double distanceLy;

        ScanStatus status;
    }

    private static final class RouteTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private final List<RouteEntry> entries = new ArrayList<>();

        private boolean sumDistances = true;

        private String currentSystemName;

        void setCurrentSystemName(String currentSystemName) {
            this.currentSystemName = currentSystemName;
            fireTableDataChanged();
        }
        
        private int findCurrentSystemRow() {
            if (currentSystemName == null) {
                return -1;
            }
            for (int i = 0; i < entries.size(); i++) {
                String name = entries.get(i).systemName;
                if (currentSystemName.equals(name)) {
                    return i;
                }
            }
            return -1;
        }
        
        void setSumDistances(boolean sumDistances) {
            if (this.sumDistances != sumDistances) {
                this.sumDistances = sumDistances;
                fireTableDataChanged();
            }
        }

        boolean isSumDistances() {
            return sumDistances;
        }

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
                case COL_DISTANCE: {
                    // Toggle locally while you iterate:
                    // true  = along-track distance (sum of legs between current row and this row)
                    // false = straight-line distance from current system to this system (uses StarPos)
                    final boolean useAlongTrackDistance = true;

                    int currentRow = findCurrentSystemRow();

                    // If we truly don't know where we are, don't guess.
                    if (currentRow < 0) {
                        return "";
                    }

                    // Current system row: show blank
                    if (rowIndex == currentRow) {
                        return "";
                    }

                    if (!useAlongTrackDistance) {
                        RouteEntry cur = entries.get(currentRow);
                        RouteEntry dst = entries.get(rowIndex);

                        if (cur.x == null || cur.y == null || cur.z == null
                                || dst.x == null || dst.y == null || dst.z == null) {
                            return "";
                        }

                        double dx = dst.x.doubleValue() - cur.x.doubleValue();
                        double dy = dst.y.doubleValue() - cur.y.doubleValue();
                        double dz = dst.z.doubleValue() - cur.z.doubleValue();
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                        return String.format("%.2f Ly", dist);
                    }

                    int from = Math.min(rowIndex, currentRow);
                    int to = Math.max(rowIndex, currentRow);

                    double total = 0.0;

                    // distanceLy at index i is the distance from (i-1) -> i
                    for (int i = from + 1; i <= to; i++) {
                        Double d = entries.get(i).distanceLy;
                        if (d == null) {
                            // If any leg along the path is unknown, we can't compute the total
                            return "";
                        }
                        total += d.doubleValue();
                    }

                    return String.format("%.2f Ly", total);
                }
default:
                    return "";
            }
        }

        void setEntries(List<RouteEntry> newEntries) {
            entries.clear();
            if (newEntries != null) {
                entries.addAll(newEntries);
            }

            /*
             * If we just plotted a route, we still want a deterministic "current row"
             * even before we have a Location/FSD event to tell us where we are.
             *
             * - If currentSystemName is null, default to the origin (row 0).
             * - If currentSystemName doesn't exist in the new route, also default to row 0.
             */
            if (!entries.isEmpty()) {
                if (currentSystemName == null || findCurrentSystemRow() < 0) {
                    currentSystemName = entries.get(0).systemName;
                }
            }

            fireTableDataChanged();
        }
        RouteEntry getEntries(int row) {
        	return entries.get(row);
        }
        void fireRowChanged(int row) {
            if (row >= 0 && row < entries.size()) {
                fireTableRowsUpdated(row, row);
            }
        }
    }

    private static final class StatusCircleIcon implements Icon {

        private final Color circleColor;
        private final String symbol;

        StatusCircleIcon(Color circleColor, String symbol) {
            this(circleColor, symbol, 18);
        }

        StatusCircleIcon(Color circleColor, String symbol, int size) {
            this.circleColor = circleColor;
            this.symbol = symbol;
//            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return getFontSize();
        }

        @Override
        public int getIconHeight() {
            return getFontSize();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                int d = getFontSize() - 1;
                g2.setColor(circleColor);
                g2.fillOval(x, y, d, d);

                g2.setColor(Color.BLACK);
                g2.drawOval(x, y, d, d);

                if (symbol != null && !symbol.isEmpty()) {
                    Font font = iconFont();
                    if (font != null) {
                        font = font.deriveFont(Font.BOLD); // 
//                                               Math.max(10f, font.getSize2D()));
                        g2.setFont(font);
                    }
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(symbol);
                    int textAscent = fm.getAscent();
                    int tx = x + (getFontSize() - textWidth) / 2;
                    int ty = y + (getFontSize() + textAscent) / 2 - 2;
                    g2.drawString(symbol, tx, ty);
                }
            } finally {
                g2.dispose();
            }
        }

		/**
		 * @return the size
		 */
		private int getFontSize() {
			return OverlayPreferences.getUiFontSize();
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
            label.setBorder(new EmptyBorder(3, 0, 3, 0));
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


        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            super.paintComponent(g2);

            g2.setColor(ED_ORANGE_TRANS);
            int y = getHeight() - 1;
            g2.drawLine(0, y, getWidth(), y);

            g2.dispose();
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

            l.setBorder(new EmptyBorder(3, 0, 3, 0));

            String system = (String) table.getValueAt(row, 2); // YOUR system column

            Icon icon = null;

            if (system != null) {

                // 1) CURRENT system always wins (never blink / never outline)
            	String cachedSystemName = getCurrentSystemName();
                if (system.equals(cachedSystemName)) {
                    icon = new TriangleIcon(ED_ORANGE, 10, 10);

                // 2) Pending jump: blink solid triangle / blank (suppress outline during jump)
                } else if (system.equals(pendingJumpSystemName)) {
                    if (jumpFlashOn) {
                        icon = new TriangleIcon(ED_ORANGE, 10, 10);
                    } else {
                        icon = null;
                    }

                // 3) Target system: outline triangle, only when NOT in a pending jump
                } else if (system.equals(targetSystemName)) {
                    if (pendingJumpSystemName == null) {
                        icon = new OutlineTriangleIcon(ED_ORANGE_LESS_TRANS, 10, 10, 2f);
                    } else {
                        icon = null;
                    }
                }
            }

            l.setIcon(icon);
            return l;

        }


        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            super.paintComponent(g2);

            g2.setColor(ED_ORANGE_TRANS);
            int y = getHeight() - 1;
            g2.drawLine(0, y, getWidth(), y);

            g2.dispose();
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
    private static class OutlineTriangleIcon implements Icon {
        private final Color color;
        private final int w;
        private final int h;
        private final float strokeWidth;

        OutlineTriangleIcon(Color color, int w, int h, float strokeWidth) {
            this.color = color;
            this.w = w;
            this.h = h;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int[] xs = { x, x, x + w };
            int[] ys = { y, y + h, y + (h / 2) };

            g2.drawPolygon(xs, ys, 3);
            g2.dispose();
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

        // Apply recursively so labels/buttons/etc. stay consistent with the table.
        applyFontRecursively(this, uiFont);

        if (headerLabel != null) {
            headerLabel.setFont(uiFont.deriveFont(Font.BOLD));
        }

        if (table != null) {
            table.setFont(uiFont);
            table.setRowHeight(computeRowHeight(table, uiFont, 6));
            if (table.getTableHeader() != null) {
                table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));
            }
        }

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

    private static int computeRowHeight(JTable table, Font font, int verticalPaddingPx) {
        if (table == null || font == null) {
            return 24;
        }
        FontMetrics fm = table.getFontMetrics(font);
        int h = fm.getAscent() + fm.getDescent() + verticalPaddingPx;
        if (h < 18) {
            h = 18;
        }
        return h;
    }
    private static Font iconFont() {
        // Consolas exists on most Windows installs.
        // If it’s missing, fall back to logical Monospaced.
        String family = "SansSerif";
        Font uiFont = OverlayPreferences.getUiFont();
        Font f = new Font(family, Font.BOLD, uiFont.getSize() -1);
        if (!family.equalsIgnoreCase(f.getFamily())) {
            f = new Font(Font.MONOSPACED, Font.PLAIN, uiFont.getSize());
        }
        return f;
    }

}
