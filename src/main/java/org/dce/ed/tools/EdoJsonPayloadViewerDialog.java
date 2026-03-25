package org.dce.ed.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Drill-down tree for a Gson-serialized JSON payload (e.g. {@code CachedSystem} in SQLite cache).
 */
final class EdoJsonPayloadViewerDialog extends JDialog {

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private EdoJsonPayloadViewerDialog(java.awt.Frame owner, String title, JsonElement root) {
        super(owner, title, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(880, 620));
        setLocationByPlatform(true);

        String rootLabel = "CachedSystem";
        if (root.isJsonObject()) {
            JsonObject jo = root.getAsJsonObject();
            if (jo.has("systemName") && !jo.get("systemName").isJsonNull()) {
                rootLabel = jo.get("systemName").getAsString();
            }
        }
        DefaultMutableTreeNode treeRoot = buildTree(rootLabel, root);
        JTree tree = new JTree(new DefaultTreeModel(treeRoot));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        applyExplorerStyleTree(tree);

        JTextArea detail = new JTextArea();
        detail.setEditable(false);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detail.setTabSize(2);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = tree.getSelectionPath();
                if (path == null) {
                    return;
                }
                Object last = path.getLastPathComponent();
                if (!(last instanceof DefaultMutableTreeNode)) {
                    return;
                }
                JsonTreeNodeData data = (JsonTreeNodeData) ((DefaultMutableTreeNode) last).getUserObject();
                detail.setText(data.toPrettyJson());
                detail.setCaretPosition(0);
            }
        });

        tree.expandRow(0);
        if (treeRoot.getChildCount() > 0) {
            tree.expandRow(1);
        }

        JButton copyBtn = new JButton("Copy subtree JSON");
        copyBtn.addActionListener(ev -> {
            TreePath path = tree.getSelectionPath();
            if (path == null) {
                JOptionPane.showMessageDialog(this, "Select a node first.", "Copy", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Object last = path.getLastPathComponent();
            if (last instanceof DefaultMutableTreeNode) {
                JsonTreeNodeData data = (JsonTreeNodeData) ((DefaultMutableTreeNode) last).getUserObject();
                String text = data.toPrettyJson();
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            }
        });

        JButton expandBodies = new JButton("Expand bodies");
        expandBodies.addActionListener(ev -> expandBodiesNodes(tree, treeRoot));

        JPanel south = new JPanel();
        south.add(copyBtn);
        south.add(expandBodies);

        JScrollPane treeScroll = new JScrollPane(tree);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, new JScrollPane(detail));
        split.setResizeWeight(0.38);
        split.setOneTouchExpandable(true);
        split.setDividerSize(6);

        JPanel north = new JPanel(new BorderLayout());
        north.add(new JLabel("Navigate the tree; details update on the right. Bodies are under \"bodies\"."), BorderLayout.WEST);

        add(north, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        pack();
    }

    /**
     * Shows a modal dialog with a tree built from JSON text.
     */
    static void show(java.awt.Component parent, String title, String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            JOptionPane.showMessageDialog(parent, "No payload text to display.", title, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(jsonText);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Invalid JSON: " + ex.getMessage(),
                    title,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        java.awt.Frame frame = JOptionPane.getFrameForComponent(parent);
        String dialogTitle = title != null && !title.isBlank() ? title : "Payload";

        LookAndFeel previousLaf = UIManager.getLookAndFeel();
        boolean switchedWindowsLaf = false;
        if (isWindowsOs()) {
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                switchedWindowsLaf = true;
            } catch (Exception ignored) {
            }
        }
        try {
            EdoJsonPayloadViewerDialog dlg = new EdoJsonPayloadViewerDialog(frame, dialogTitle, root);
            dlg.setLocationRelativeTo(parent);
            dlg.setVisible(true);
        } finally {
            if (switchedWindowsLaf) {
                try {
                    UIManager.setLookAndFeel(previousLaf);
                } catch (Exception ignored) {
                }
                Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
                if (w != null) {
                    SwingUtilities.updateComponentTreeUI(w);
                }
            }
        }
    }

    private static boolean isWindowsOs() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * Explorer-like tree: angled lines, system UI font, comfortable row height.
     */
    private static void applyExplorerStyleTree(JTree tree) {
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setRowHeight(22);
        tree.setScrollsOnExpand(true);
        if (isWindowsOs()) {
            tree.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        } else {
            tree.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        }
    }

    private static void expandBodiesNodes(JTree tree, DefaultMutableTreeNode root) {
        DefaultMutableTreeNode bodies = findFirstNamedChild(root, "bodies");
        if (bodies == null) {
            return;
        }
        TreePath path = new TreePath(bodies.getPath());
        tree.expandPath(path);
        for (int i = 0; i < bodies.getChildCount(); i++) {
            DefaultMutableTreeNode ch = (DefaultMutableTreeNode) bodies.getChildAt(i);
            tree.expandPath(new TreePath(ch.getPath()));
        }
    }

    private static DefaultMutableTreeNode findFirstNamedChild(DefaultMutableTreeNode node, String key) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode ch = (DefaultMutableTreeNode) node.getChildAt(i);
            Object uo = ch.getUserObject();
            if (uo instanceof JsonTreeNodeData) {
                if (key.equals(((JsonTreeNodeData) uo).key)) {
                    return ch;
                }
            }
        }
        return null;
    }

    private static DefaultMutableTreeNode buildTree(String key, JsonElement el) {
        JsonTreeNodeData data = new JsonTreeNodeData(key, el);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);

        if (el.isJsonNull()) {
            return node;
        }
        if (el.isJsonPrimitive()) {
            return node;
        }
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement c = arr.get(i);
                String childKey = labelForArrayElement(c, i);
                node.add(buildTree(childKey, c));
            }
            return node;
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                node.add(buildTree(e.getKey(), e.getValue()));
            }
            return node;
        }
        return node;
    }

    private static String labelForArrayElement(JsonElement c, int i) {
        if (c.isJsonObject()) {
            JsonObject o = c.getAsJsonObject();
            if (o.has("bodyName") && !o.get("bodyName").isJsonNull()) {
                return "[" + i + "] " + o.get("bodyName").getAsString();
            }
            if (o.has("name") && !o.get("name").isJsonNull()) {
                return "[" + i + "] " + o.get("name").getAsString();
            }
        }
        return "[" + i + "]";
    }

    private static final class JsonTreeNodeData {
        final String key;
        final JsonElement element;

        JsonTreeNodeData(String key, JsonElement element) {
            this.key = key;
            this.element = element;
        }

        @Override
        public String toString() {
            if (element.isJsonNull()) {
                return key + ": null";
            }
            if (element.isJsonPrimitive()) {
                JsonPrimitive p = element.getAsJsonPrimitive();
                String s = previewPrimitive(p);
                if (s.length() > 120) {
                    s = s.substring(0, 117) + "…";
                }
                return key + ": " + s;
            }
            if (element.isJsonArray()) {
                return key + " [" + element.getAsJsonArray().size() + "]";
            }
            if (element.isJsonObject()) {
                return key + " {" + element.getAsJsonObject().size() + " keys}";
            }
            return key;
        }

        private static String previewPrimitive(JsonPrimitive p) {
            if (p.isBoolean()) {
                return Boolean.toString(p.getAsBoolean());
            }
            if (p.isNumber()) {
                return p.getAsString();
            }
            return p.getAsString();
        }

        String toPrettyJson() {
            return PRETTY.toJson(element);
        }
    }
}
