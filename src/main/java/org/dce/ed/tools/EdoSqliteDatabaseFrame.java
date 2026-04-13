package org.dce.ed.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.dce.ed.cache.SystemCache;

/**
 * Minimal in-process SQLite browser (HSQL Database Manager–style layout):
 * schema tree (left), SQL editor + Clear / Execute (top-right), results grid (bottom-right).
 */
public final class EdoSqliteDatabaseFrame extends JFrame {

    private static final Preferences SQLITE_BROWSER_PREFS = Preferences.userNodeForPackage(EdoSqliteDatabaseFrame.class);
    private static final String PREF_LAST_DISTANCE_LY = "lastDistanceLy";

    private static EdoSqliteDatabaseFrame openInstance;

    private final Path dbPath;
    private final String jdbcUrl;
    private Connection connection;

    private final JTree schemaTree;
    private final DefaultTreeModel treeModel;
    private final JTextArea sqlArea;
    private final JTable resultTable;
    private final DefaultTableModel resultModel;
    private final JLabel statusLabel;

    public static void main(String args[]) {
        EdoSqliteDatabaseFrame.showDefaultOrBringToFront(null);
    }
    private EdoSqliteDatabaseFrame(Path dbPath) throws SQLException {
        super("EDO SQLite Database");
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toString().replace('\\', '/');

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(1000, 700));
        setLocationByPlatform(true);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(jdbcUrl);
        treeModel = new DefaultTreeModel(root);
        schemaTree = new JTree(treeModel);
        schemaTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        schemaTree.setRootVisible(true);
        schemaTree.setShowsRootHandles(true);
        JScrollPane treeScroll = new JScrollPane(schemaTree);
        treeScroll.setPreferredSize(new Dimension(260, 400));

        sqlArea = new JTextArea(8, 40);
        sqlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        sqlArea.setTabSize(4);
        JScrollPane sqlScroll = new JScrollPane(sqlArea);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> sqlArea.setText(""));

        JButton execBtn = new JButton("<html><center>Exe<br>cute</center></html>");
        execBtn.addActionListener(e -> executeCurrentSql());

        JPanel sqlRow = new JPanel(new BorderLayout(4, 0));
        sqlRow.add(clearBtn, BorderLayout.WEST);
        sqlRow.add(sqlScroll, BorderLayout.CENTER);
        sqlRow.add(execBtn, BorderLayout.EAST);

        resultModel = new DefaultTableModel();
        resultTable = new JTable(resultModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane resultScroll = new JScrollPane(resultTable);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sqlRow, resultScroll);
        vertical.setResizeWeight(0.35);
        vertical.setOneTouchExpandable(true);

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, vertical);
        horizontal.setResizeWeight(0.22);
        horizontal.setOneTouchExpandable(true);

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);
        JMenu viewMenu = new JMenu("View");
        JMenuItem refreshItem = new JMenuItem("Refresh schema");
        refreshItem.addActionListener(e -> refreshSchemaTree());
        viewMenu.add(refreshItem);
        bar.add(fileMenu);
        bar.add(viewMenu);
        setJMenuBar(bar);

        JPanel content = new JPanel(new BorderLayout());
        content.add(horizontal, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(content);

        Action executeSqlAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeCurrentSql();
            }
        };
        KeyStroke ctrlM = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK);
        String executeSqlKey = "edoSqlExecute";
        sqlArea.getInputMap(JComponent.WHEN_FOCUSED).put(ctrlM, executeSqlKey);
        sqlArea.getActionMap().put(executeSqlKey, executeSqlAction);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlM, executeSqlKey);
        getRootPane().getActionMap().put(executeSqlKey, executeSqlAction);

        attachResultTableContextMenu();

        schemaTree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path == null) {
                return;
            }
            Object last = path.getLastPathComponent();
            if (!(last instanceof DefaultMutableTreeNode)) {
                return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) last;
            if (node.isLeaf() && node.getParent() != null && node.getParent().getParent() != null) {
                // column under table
                return;
            }
            if (!node.isLeaf() && node.getParent() != null && node.getParent().getParent() == null) {
                // table node (child of root)
                String table = String.valueOf(node.getUserObject());
                sqlArea.setText("SELECT * FROM \"" + table.replace("\"", "\"\"") + "\" LIMIT 200;");
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeConnectionQuietly();
                if (openInstance == EdoSqliteDatabaseFrame.this) {
                    openInstance = null;
                }
            }
        });

        connectAndLoadSchema();
    }

    private void connectAndLoadSchema() throws SQLException {
        if (!Files.isRegularFile(dbPath)) {
            throw new SQLException("Database file not found:\n" + dbPath + "\n\n"
                    + "Run the overlay or rescan once to create the cache, or check -D" + SystemCache.CACHE_DB_PATH_PROPERTY + ".");
        }
        connection = DriverManager.getConnection(jdbcUrl);
        statusLabel.setText("Connected: " + dbPath);
        refreshSchemaTree();
    }

    private void closeConnectionQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    private void refreshSchemaTree() {
        if (connection == null) {
            return;
        }
        statusLabel.setText("Loading schema…");
        new SwingWorker<DefaultMutableTreeNode, Void>() {
            @Override
            protected DefaultMutableTreeNode doInBackground() throws Exception {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(jdbcUrl);
                DatabaseMetaData md = connection.getMetaData();
                try (ResultSet trs = md.getTables(null, null, "%", new String[] { "TABLE", "VIEW" })) {
                    while (trs.next()) {
                        String tname = trs.getString("TABLE_NAME");
                        if (tname == null) {
                            continue;
                        }
                        if ("sqlite_sequence".equalsIgnoreCase(tname)) {
                            continue;
                        }
                        DefaultMutableTreeNode tnode = new DefaultMutableTreeNode(tname);
                        root.add(tnode);
                        try (ResultSet crs = md.getColumns(null, null, tname, "%")) {
                            while (crs.next()) {
                                String cname = crs.getString("COLUMN_NAME");
                                if (cname != null) {
                                    tnode.add(new DefaultMutableTreeNode(cname));
                                }
                            }
                        }
                    }
                }
                return root;
            }

            @Override
            protected void done() {
                try {
                    DefaultMutableTreeNode root = get();
                    treeModel.setRoot(root);
                    schemaTree.expandRow(0);
                    statusLabel.setText("Ready — " + dbPath);
                } catch (Exception ex) {
                    statusLabel.setText("Schema load failed");
                    JOptionPane.showMessageDialog(EdoSqliteDatabaseFrame.this,
                            ex.getMessage(),
                            "Schema",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void executeCurrentSql() {
        if (connection == null) {
            JOptionPane.showMessageDialog(this, "No database connection.", "Execute", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = sqlArea.getText();
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        final String trimmed = sql.trim();
        statusLabel.setText("Running…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Statement st = connection.createStatement()) {
                    st.setMaxRows(5001);
                    boolean hasResult = st.execute(trimmed);
                    if (hasResult) {
                        try (ResultSet rs = st.getResultSet()) {
                            buildTableFromResultSet(rs);
                        }
                    } else {
                        int uc = st.getUpdateCount();
                        SwingUtilities.invokeLater(() -> {
                            resultModel.setRowCount(0);
                            resultModel.setColumnCount(0);
                            statusLabel.setText("OK — update count: " + uc);
                        });
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Error");
                    JOptionPane.showMessageDialog(EdoSqliteDatabaseFrame.this,
                            c.getMessage(),
                            "SQL Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void buildTableFromResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        Vector<String> names = new Vector<>();
        for (int i = 1; i <= colCount; i++) {
            String label = meta.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = meta.getColumnName(i);
            }
            names.add(label);
        }
        List<Vector<Object>> rows = new ArrayList<>();
        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
            Vector<Object> row = new Vector<>();
            for (int i = 1; i <= colCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
            if (rowCount >= 5000) {
                break;
            }
        }
        final int displayed = rowCount;
        final Vector<Vector<Object>> data = new Vector<>(rows);
        SwingUtilities.invokeLater(() -> {
            resultModel.setDataVector(data, names);
            statusLabel.setText("Rows: " + displayed + (displayed >= 5000 ? " (max 5000 shown)" : ""));
        });
    }

    private void attachResultTableContextMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem distItem = new JMenuItem("Query systems within distance…");
        JMenuItem payloadItem = new JMenuItem("View payload…");
        popup.add(distItem);
        popup.add(payloadItem);

        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShow(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShow(e);
            }

            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int viewRow = resultTable.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    return;
                }
                int modelRow = resultTable.convertRowIndexToModel(viewRow);
                resultTable.setRowSelectionInterval(viewRow, viewRow);
                double[] pos = resolveStarPosFromRow(modelRow);
                String payload = resolvePayloadJsonFromRow(modelRow);
                distItem.setEnabled(pos != null);
                distItem.setToolTipText(pos == null
                        ? "Need star position: payload_json, system_address / cache_key / canonical_name, or x,y,z columns"
                        : null);
                payloadItem.setEnabled(payload != null);
                payloadItem.setToolTipText(payload == null
                        ? "Need payload_json or a systems table key column to load JSON"
                        : null);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        distItem.addActionListener(ev -> {
            int vr = resultTable.getSelectedRow();
            if (vr < 0) {
                return;
            }
            int mr = resultTable.convertRowIndexToModel(vr);
            showQueryByDistanceForRow(mr);
        });
        payloadItem.addActionListener(ev -> {
            int vr = resultTable.getSelectedRow();
            if (vr < 0) {
                return;
            }
            int mr = resultTable.convertRowIndexToModel(vr);
            showPayloadViewerForRow(mr);
        });
    }

    private void showQueryByDistanceForRow(int modelRow) {
        double[] pos = resolveStarPosFromRow(modelRow);
        if (pos == null) {
            JOptionPane.showMessageDialog(this,
                    "Could not determine star position for this row.",
                    "Query by distance",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String lastLy = SQLITE_BROWSER_PREFS.get(PREF_LAST_DISTANCE_LY, "50");
        Object inputObj = JOptionPane.showInputDialog(this,
                "Radius (light years) from this row's star position:",
                "Systems within distance",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                lastLy);
        if (inputObj == null) {
            return;
        }
        String trimmed = inputObj.toString().trim();
        if (trimmed.isEmpty()) {
            return;
        }
        double radius;
        try {
            radius = Double.parseDouble(trimmed);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Not a number: " + trimmed, "Query by distance", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (radius <= 0 || Double.isNaN(radius) || Double.isInfinite(radius)) {
            JOptionPane.showMessageDialog(this, "Radius must be a positive number.", "Query by distance", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SQLITE_BROWSER_PREFS.put(PREF_LAST_DISTANCE_LY, trimmed);
        runSystemsWithinDistance(pos[0], pos[1], pos[2], radius);
    }

    private void runSystemsWithinDistance(double x0, double y0, double z0, double radiusLy) {
        if (connection == null) {
            JOptionPane.showMessageDialog(this, "No database connection.", "Query by distance", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final double r2 = radiusLy * radiusLy;
        final String sql = ""
                + "SELECT cache_key, system_name, x, y, z, dist_sq\n"
                + "FROM (\n"
                + "  SELECT cache_key, system_name,\n"
                + "    json_extract(payload_json, '$.starPos[0]') AS x,\n"
                + "    json_extract(payload_json, '$.starPos[1]') AS y,\n"
                + "    json_extract(payload_json, '$.starPos[2]') AS z,\n"
                + "    ((json_extract(payload_json, '$.starPos[0]') - ?) * (json_extract(payload_json, '$.starPos[0]') - ?) +\n"
                + "     (json_extract(payload_json, '$.starPos[1]') - ?) * (json_extract(payload_json, '$.starPos[1]') - ?) +\n"
                + "     (json_extract(payload_json, '$.starPos[2]') - ?) * (json_extract(payload_json, '$.starPos[2]') - ?)) AS dist_sq\n"
                + "  FROM systems\n"
                + "  WHERE json_extract(payload_json, '$.starPos[0]') IS NOT NULL\n"
                + "    AND json_extract(payload_json, '$.starPos[1]') IS NOT NULL\n"
                + "    AND json_extract(payload_json, '$.starPos[2]') IS NOT NULL\n"
                + ") t\n"
                + "WHERE t.dist_sq <= ?\n"
                + "ORDER BY t.dist_sq\n"
                + "LIMIT 5000";
        statusLabel.setText("Running distance query…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setDouble(1, x0);
                    ps.setDouble(2, x0);
                    ps.setDouble(3, y0);
                    ps.setDouble(4, y0);
                    ps.setDouble(5, z0);
                    ps.setDouble(6, z0);
                    ps.setDouble(7, r2);
                    try (ResultSet rs = ps.executeQuery()) {
                        buildTableFromResultSet(rs);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Append after buildTableFromResultSet's invokeLater (row count) runs.
                    SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
                        String t = statusLabel.getText();
                        if (t == null) {
                            t = "";
                        }
                        statusLabel.setText(t + String.format(Locale.ROOT,
                                " — within %.4f ly of (%.4f, %.4f, %.4f)",
                                radiusLy, x0, y0, z0));
                    }));
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Error");
                    JOptionPane.showMessageDialog(EdoSqliteDatabaseFrame.this,
                            c.getMessage(),
                            "SQL Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showPayloadViewerForRow(int modelRow) {
        String json = resolvePayloadJsonFromRow(modelRow);
        if (json == null || json.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "No payload JSON available for this row.",
                    "Payload",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String title = titleFromPayloadJson(json);
        EdoJsonPayloadViewerDialog.show(this, title, json);
    }

    private static String titleFromPayloadJson(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("systemName") && !o.get("systemName").isJsonNull()) {
                return o.get("systemName").getAsString();
            }
        } catch (Exception ignored) {
        }
        return "Payload";
    }

    private String resolvePayloadJsonFromRow(int modelRow) {
        int n = resultModel.getColumnCount();
        Vector<String> names = new Vector<>(n);
        for (int c = 0; c < n; c++) {
            names.add(resultModel.getColumnName(c));
        }
        int pj = findColumnIndex(names, "payload_json");
        if (pj >= 0) {
            Object v = resultModel.getValueAt(modelRow, pj);
            String s = cellToString(v);
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        int sa = findColumnIndex(names, "system_address");
        if (sa >= 0) {
            Object v = resultModel.getValueAt(modelRow, sa);
            String fetched = fetchPayloadBySystemAddress(v);
            if (fetched != null) {
                return fetched;
            }
        }
        int ck = findColumnIndex(names, "cache_key");
        if (ck >= 0) {
            Object v = resultModel.getValueAt(modelRow, ck);
            String fetched = fetchPayloadByCacheKey(cellToString(v));
            if (fetched != null) {
                return fetched;
            }
        }
        int cn = findColumnIndex(names, "canonical_name");
        if (cn >= 0) {
            Object v = resultModel.getValueAt(modelRow, cn);
            String fetched = fetchPayloadByCanonicalName(cellToString(v));
            if (fetched != null) {
                return fetched;
            }
        }
        return null;
    }

    private String fetchPayloadBySystemAddress(Object systemAddress) {
        if (connection == null || systemAddress == null) {
            return null;
        }
        Long addr = toLong(systemAddress);
        if (addr == null) {
            return null;
        }
        String sql = "SELECT payload_json FROM systems WHERE system_address = ? ORDER BY updated_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, addr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private String fetchPayloadByCacheKey(String cacheKey) {
        if (connection == null || cacheKey == null || cacheKey.isBlank()) {
            return null;
        }
        String sql = "SELECT payload_json FROM systems WHERE cache_key = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cacheKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private String fetchPayloadByCanonicalName(String name) {
        if (connection == null || name == null || name.isBlank()) {
            return null;
        }
        String sql = "SELECT payload_json FROM systems WHERE canonical_name = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private double[] resolveStarPosFromRow(int modelRow) {
        int n = resultModel.getColumnCount();
        Vector<String> names = new Vector<>(n);
        for (int c = 0; c < n; c++) {
            names.add(resultModel.getColumnName(c));
        }
        int ix = findColumnIndex(names, "x");
        int iy = findColumnIndex(names, "y");
        int iz = findColumnIndex(names, "z");
        if (ix >= 0 && iy >= 0 && iz >= 0) {
            Double x = toDouble(resultModel.getValueAt(modelRow, ix));
            Double y = toDouble(resultModel.getValueAt(modelRow, iy));
            Double z = toDouble(resultModel.getValueAt(modelRow, iz));
            if (x != null && y != null && z != null) {
                return new double[] { x, y, z };
            }
        }
        String payload = resolvePayloadJsonFromRow(modelRow);
        if (payload != null) {
            double[] fromJson = parseStarPosFromPayloadJson(payload);
            if (fromJson != null) {
                return fromJson;
            }
        }
        return null;
    }

    private static double[] parseStarPosFromPayloadJson(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (!o.has("starPos")) {
                return null;
            }
            JsonArray a = o.getAsJsonArray("starPos");
            if (a == null || a.size() < 3) {
                return null;
            }
            return new double[] {
                    a.get(0).getAsDouble(),
                    a.get(1).getAsDouble(),
                    a.get(2).getAsDouble()
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static int findColumnIndex(Vector<String> cols, String... candidates) {
        for (int i = 0; i < cols.size(); i++) {
            String c = cols.get(i);
            if (c == null) {
                continue;
            }
            String lc = c.toLowerCase(Locale.ROOT);
            for (String cand : candidates) {
                if (lc.equals(cand.toLowerCase(Locale.ROOT))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String cellToString(Object v) {
        if (v == null) {
            return null;
        }
        return String.valueOf(v);
    }

    private static Double toDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return Double.parseDouble(o.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Opens the same SQLite file as the running overlay's {@link SystemCache} singleton, or brings an existing window to front.
     */
    public static void showDefaultOrBringToFront(Component parent) {
        Path path = SystemCache.getInstance().getCacheDbPath();
        showOrBringToFront(parent, path);
    }

    public static void showOrBringToFront(Component parent, Path dbPath) {
        SwingUtilities.invokeLater(() -> {
            if (openInstance != null && openInstance.isDisplayable()) {
                openInstance.toFront();
                openInstance.requestFocus();
                return;
            }
            try {
                openInstance = new EdoSqliteDatabaseFrame(dbPath);
                openInstance.setLocationRelativeTo(parent);
                openInstance.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                        "Could not open SQLite browser:\n" + ex.getMessage(),
                        "SQLite Database",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
