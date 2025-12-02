package org.dce.ed.edsm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.util.prefs.Preferences;

public class EdsmQueryTool extends JFrame {

    private static final String PREF_KEY_EDSM_API = "edsmApiKey";
    private static final String PREF_KEY_EDSM_CMDR = "edsmCommanderName";

    // Put your PNG at: src/main/resources/org/dce/ed/edsm/locate_icon.png
    private static final String LOCATE_ICON_PATH = "/org/dce/ed/edsm/locate_icon.png";
    private static final int LOCATE_ICON_SIZE = 16; // keep icon around text height
    private static final ImageIcon LOCATE_ICON = loadLocateIcon();

    private final EdsmClient client;
    private final Gson gson;

    private final JTextArea outputArea;
    private final JLabel commanderStatusLabel;

    public EdsmQueryTool() {
        super("EDSM Query Tool");

        this.client = new EdsmClient();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ----- Header (title + commander status) -----
        JLabel titleLabel = new JLabel("Elite Dangerous Star Map (EDSM) Query Tool");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        commanderStatusLabel = new JLabel();
        updateCommanderStatusLabel();

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(commanderStatusLabel, BorderLayout.EAST);

        // ----- Tabs -----
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("System", createSystemTab());
        tabs.addTab("Bodies", createBodiesTab());
        tabs.addTab("Traffic / Logs", createTrafficTab());
        tabs.addTab("Commander", createCommanderTab());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(tabs, BorderLayout.CENTER);

        // ----- Output area -----
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setPreferredSize(new Dimension(800, 400));

        add(topPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private static ImageIcon loadLocateIcon() {
        java.net.URL url = EdsmQueryTool.class.getResource(LOCATE_ICON_PATH);
        if (url != null) {
            ImageIcon raw = new ImageIcon(url);
            int w = raw.getIconWidth();
            int h = raw.getIconHeight();
            if (w > 0 && h > 0) {
                int size = LOCATE_ICON_SIZE;
                Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
            return raw;
        }
        return null;
    }

    // ============================================================
    // TABS
    // ============================================================

    private JPanel createSystemTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField systemNameField = new JTextField(30);
        JTextField systemsField = new JTextField(30);

        JTextField xField = new JTextField(6);
        JTextField yField = new JTextField(6);
        JTextField zField = new JTextField(6);
        JTextField radiusField = new JTextField(6);

        JButton getSystemButton = new JButton("Get System");
        JButton getSystemsButton = new JButton("Get Systems");
        JButton showSystemButton = new JButton("Show System (info + primary star)");
        JButton sphereSystemsButton = new JButton("Search Sphere");

        // ----- Section: Single system -----
        JPanel singleSystemPanel = new JPanel();
        singleSystemPanel.setLayout(new BoxLayout(singleSystemPanel, BoxLayout.Y_AXIS));
        singleSystemPanel.setBorder(BorderFactory.createTitledBorder("Single system by name"));

        singleSystemPanel.add(makeLabeledWithLocate(systemNameField, "System name:"));
        singleSystemPanel.add(Box.createVerticalStrut(6));

        JPanel singleButtons = new JPanel();
        singleButtons.setLayout(new BoxLayout(singleButtons, BoxLayout.X_AXIS));
        singleButtons.add(getSystemButton);
        singleButtons.add(Box.createHorizontalStrut(8));
        singleButtons.add(showSystemButton);
        singleButtons.add(Box.createHorizontalGlue());

        singleSystemPanel.add(singleButtons);

        // ----- Section: Multiple systems -----
        JPanel multiSystemPanel = new JPanel();
        multiSystemPanel.setLayout(new BoxLayout(multiSystemPanel, BoxLayout.Y_AXIS));
        multiSystemPanel.setBorder(BorderFactory.createTitledBorder("Multiple systems"));

        multiSystemPanel.add(makeLabeled(systemsField, "System names (comma-separated):"));
        multiSystemPanel.add(Box.createVerticalStrut(6));
        multiSystemPanel.add(getSystemsButton);

        // ----- Section: Sphere search -----
        JPanel spherePanel = new JPanel();
        spherePanel.setLayout(new BoxLayout(spherePanel, BoxLayout.Y_AXIS));
        spherePanel.setBorder(BorderFactory.createTitledBorder("Sphere search (around point)"));

        JPanel coordsPanel = new JPanel();
        coordsPanel.setLayout(new BoxLayout(coordsPanel, BoxLayout.X_AXIS));
        coordsPanel.add(new JLabel("X:"));
        coordsPanel.add(xField);
        coordsPanel.add(Box.createHorizontalStrut(4));
        coordsPanel.add(new JLabel("Y:"));
        coordsPanel.add(yField);
        coordsPanel.add(Box.createHorizontalStrut(4));
        coordsPanel.add(new JLabel("Z:"));
        coordsPanel.add(zField);
        coordsPanel.add(Box.createHorizontalStrut(8));
        coordsPanel.add(new JLabel("Radius (ly):"));
        coordsPanel.add(radiusField);

        spherePanel.add(coordsPanel);
        spherePanel.add(Box.createVerticalStrut(6));
        spherePanel.add(sphereSystemsButton);

        // ----- Assemble tab -----
        panel.add(singleSystemPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(multiSystemPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(spherePanel);

        // ----- Actions -----
        getSystemButton.addActionListener(e -> {
            String name = systemNameField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            runQueryAsync("getSystem(" + name + ")", () -> {
                SystemResponse resp = client.getSystem(name);
                return toJsonOrMessage(resp);
            });
        });

        showSystemButton.addActionListener(e -> {
            String name = systemNameField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            runQueryAsync("showSystem(" + name + ")", () -> {
                ShowSystemResponse resp = client.showSystem(name);
                return toJsonOrMessage(resp);
            });
        });

        getSystemsButton.addActionListener(e -> {
            String names = systemsField.getText().trim();
            if (names.isEmpty()) {
                appendOutput("Please enter one or more system names.\n");
                return;
            }
            runQueryAsync("getSystems(" + names + ")", () -> {
                SystemResponse[] resp = client.getSystems(names);
                return toJsonOrMessage(resp);
            });
        });

        sphereSystemsButton.addActionListener(e -> {
            String xText = xField.getText().trim();
            String yText = yField.getText().trim();
            String zText = zField.getText().trim();
            String rText = radiusField.getText().trim();

            if (xText.isEmpty() || yText.isEmpty() || zText.isEmpty() || rText.isEmpty()) {
                appendOutput("Please enter X, Y, Z and radius.\n");
                return;
            }

            try {
                double x = Double.parseDouble(xText);
                double y = Double.parseDouble(yText);
                double z = Double.parseDouble(zText);
                int radius = Integer.parseInt(rText);

                runQueryAsync("sphereSystems(" + x + "," + y + "," + z + "," + radius + ")", () -> {
                    SphereSystemsResponse[] resp = client.sphereSystems(x, y, z, radius);
                    return toJsonOrMessage(resp);
                });
            } catch (NumberFormatException ex) {
                appendOutput("Invalid number format for coordinates or radius.\n");
            }
        });

        return panel;
    }

    private JPanel createBodiesTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField systemNameField = new JTextField(30);
        JTextField systemIdField = new JTextField(20);

        JButton showBodiesByNameButton = new JButton("Show Bodies");
        JButton showBodiesByIdButton = new JButton("Show Bodies (by ID)");

        // ----- Section: By system name -----
        JPanel byNamePanel = new JPanel();
        byNamePanel.setLayout(new BoxLayout(byNamePanel, BoxLayout.Y_AXIS));
        byNamePanel.setBorder(BorderFactory.createTitledBorder("Bodies by system name"));

        byNamePanel.add(makeLabeledWithLocate(systemNameField, "System name:"));
        byNamePanel.add(Box.createVerticalStrut(6));
        byNamePanel.add(showBodiesByNameButton);

        // ----- Section: By system ID -----
        JPanel byIdPanel = new JPanel();
        byIdPanel.setLayout(new BoxLayout(byIdPanel, BoxLayout.Y_AXIS));
        byIdPanel.setBorder(BorderFactory.createTitledBorder("Bodies by system ID"));

        byIdPanel.add(makeLabeled(systemIdField, "System ID:"));
        byIdPanel.add(Box.createVerticalStrut(6));
        byIdPanel.add(showBodiesByIdButton);

        // Assemble
        panel.add(byNamePanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(byIdPanel);

        // Actions
        showBodiesByNameButton.addActionListener(e -> {
            String name = systemNameField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            runQueryAsync("showBodies(systemName=" + name + ")", () -> {
                BodiesResponse resp = client.showBodies(name);
                return toJsonOrMessage(resp);
            });
        });

        showBodiesByIdButton.addActionListener(e -> {
            String idText = systemIdField.getText().trim();
            if (idText.isEmpty()) {
                appendOutput("Please enter a system ID.\n");
                return;
            }
            try {
                long id = Long.parseLong(idText);
                runQueryAsync("showBodies(systemId=" + id + ")", () -> {
                    BodiesResponse resp = client.showBodies(id);
                    return toJsonOrMessage(resp);
                });
            } catch (NumberFormatException ex) {
                appendOutput("Invalid system ID (must be a number).\n");
            }
        });

        return panel;
    }

    private JPanel createTrafficTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField systemNameField = new JTextField(30);
        JButton trafficButton = new JButton("System Traffic");
        JButton deathsButton = new JButton("System Deaths");
        JButton stationsButton = new JButton("System Stations");
        JButton logsButton = new JButton("System Logs");

        // ----- Section: System activity -----
        JPanel activityPanel = new JPanel();
        activityPanel.setLayout(new BoxLayout(activityPanel, BoxLayout.Y_AXIS));
        activityPanel.setBorder(BorderFactory.createTitledBorder("System activity"));

        activityPanel.add(makeLabeledWithLocate(systemNameField, "System name:"));
        activityPanel.add(Box.createVerticalStrut(6));

        JPanel buttonRow1 = new JPanel();
        buttonRow1.setLayout(new BoxLayout(buttonRow1, BoxLayout.X_AXIS));
        buttonRow1.add(trafficButton);
        buttonRow1.add(Box.createHorizontalStrut(8));
        buttonRow1.add(deathsButton);
        buttonRow1.add(Box.createHorizontalGlue());

        JPanel buttonRow2 = new JPanel();
        buttonRow2.setLayout(new BoxLayout(buttonRow2, BoxLayout.X_AXIS));
        buttonRow2.add(stationsButton);
        buttonRow2.add(Box.createHorizontalStrut(8));
        buttonRow2.add(logsButton);
        buttonRow2.add(Box.createHorizontalGlue());

        activityPanel.add(buttonRow1);
        activityPanel.add(Box.createVerticalStrut(4));
        activityPanel.add(buttonRow2);

        panel.add(activityPanel);

        // Actions
        trafficButton.addActionListener(e -> {
            String name = systemNameField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            runQueryAsync("showTraffic(" + name + ")", () -> {
                TrafficResponse resp = client.showTraffic(name);
                return toJsonOrMessage(resp);
            });
        });

        deathsButton.addActionListener(e -> {
            String name = systemNameField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            runQueryAsync("showDeaths(" + name + ")", () -> {
                DeathsResponse resp = client.showDeaths(name);
                return toJsonOrMessage(resp);
            });
        });

        stationsButton.addActionListener(e -> {
            String name = systemNameField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            runQueryAsync("getSystemStations(" + name + ")", () -> {
                SystemStationsResponse resp = client.getSystemStations(name);
                return toJsonOrMessage(resp);
            });
        });

        logsButton.addActionListener(e -> {
            String name = systemNameField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }

            if (!ensureCommanderPrefs()) {
                appendOutput("Commander preferences not set; cannot query system logs.\n");
                return;
            }

            String[] creds = loadCommanderPrefs();
            String apiKey = creds[0];
            String commanderName = creds[1];

            runQueryAsync("systemLogs(" + name + ")", () -> {
                LogsResponse resp = client.systemLogs(apiKey, commanderName, name);
                return toJsonOrMessage(resp);
            });
        });

        return panel;
    }

    private JPanel createCommanderTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton prefsButton = new JButton("EDSM Preferences…");
        JButton cmdrLogsButton = new JButton("Get Commander Logs");
        JButton cmdrLastPosButton = new JButton("Get Commander Last Position");
        JButton cmdrRanksButton = new JButton("Get Commander Ranks/Stats");

        // ----- Section: Credentials -----
        JPanel credsPanel = new JPanel();
        credsPanel.setLayout(new BoxLayout(credsPanel, BoxLayout.Y_AXIS));
        credsPanel.setBorder(BorderFactory.createTitledBorder("Credentials"));

        JLabel hintLabel = new JLabel("Set your EDSM API key and commander name used for all commander lookups.");
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, 11f));

        credsPanel.add(hintLabel);
        credsPanel.add(Box.createVerticalStrut(4));
        credsPanel.add(prefsButton);

        // ----- Section: Commander queries -----
        JPanel queriesPanel = new JPanel();
        queriesPanel.setLayout(new BoxLayout(queriesPanel, BoxLayout.Y_AXIS));
        queriesPanel.setBorder(BorderFactory.createTitledBorder("Commander queries"));

        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.add(cmdrLogsButton);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(cmdrLastPosButton);
        row1.add(Box.createHorizontalGlue());

        JPanel row2 = new JPanel();
        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));
        row2.add(cmdrRanksButton);
        row2.add(Box.createHorizontalGlue());

        queriesPanel.add(row1);
        queriesPanel.add(Box.createVerticalStrut(4));
        queriesPanel.add(row2);

        panel.add(credsPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(queriesPanel);

        // Actions
        prefsButton.addActionListener(e -> showCommanderPreferencesDialog());

        cmdrLogsButton.addActionListener(e -> {
            if (!ensureCommanderPrefs()) {
                appendOutput("Commander preferences not set.\n");
                return;
            }
            String[] creds = loadCommanderPrefs();
            String apiKey = creds[0];
            String commanderName = creds[1];

            runQueryAsync("getCmdrLogs()", () -> {
                LogsResponse resp = client.getCmdrLogs(apiKey, commanderName);
                return toJsonOrMessage(resp);
            });
        });

        cmdrLastPosButton.addActionListener(e -> {
            if (!ensureCommanderPrefs()) {
                appendOutput("Commander preferences not set.\n");
                return;
            }
            String[] creds = loadCommanderPrefs();
            String apiKey = creds[0];
            String commanderName = creds[1];

            runQueryAsync("getCmdrLastPosition()", () -> {
                CmdrLastPositionResponse resp = client.getCmdrLastPosition(apiKey, commanderName);
                return toJsonOrMessage(resp);
            });
        });

        cmdrRanksButton.addActionListener(e -> {
            if (!ensureCommanderPrefs()) {
                appendOutput("Commander preferences not set.\n");
                return;
            }
            String[] creds = loadCommanderPrefs();
            String apiKey = creds[0];
            String commanderName = creds[1];

            runQueryAsync("getCmdrRanks()", () -> {
                CmdrRanksResponse resp = client.getCmdrRanks(apiKey, commanderName);
                return toJsonOrMessage(resp);
            });
        });

        return panel;
    }

    // ============================================================
    // PREFERENCES (API key + commander name)
    // ============================================================

    private boolean ensureCommanderPrefs() {
        String[] creds = loadCommanderPrefs();
        String apiKey = creds[0];
        String commanderName = creds[1];

        if (apiKey.isEmpty() || commanderName.isEmpty()) {
            showCommanderPreferencesDialog();
            creds = loadCommanderPrefs();
            apiKey = creds[0];
            commanderName = creds[1];
        }

        return !apiKey.isEmpty() && !commanderName.isEmpty();
    }

    private String[] loadCommanderPrefs() {
        Preferences prefs = Preferences.userNodeForPackage(EdsmQueryTool.class);
        String apiKey = prefs.get(PREF_KEY_EDSM_API, "").trim();
        String commanderName = prefs.get(PREF_KEY_EDSM_CMDR, "").trim();
        return new String[]{apiKey, commanderName};
    }

    private void saveCommanderPrefs(String apiKey, String commanderName) {
        Preferences prefs = Preferences.userNodeForPackage(EdsmQueryTool.class);
        if (apiKey != null) {
            prefs.put(PREF_KEY_EDSM_API, apiKey.trim());
        }
        if (commanderName != null) {
            prefs.put(PREF_KEY_EDSM_CMDR, commanderName.trim());
        }
    }

    private void updateCommanderStatusLabel() {
        String[] creds = loadCommanderPrefs();
        String apiKey = creds[0];
        String commanderName = creds[1];

        String statusText;
        if (commanderName.isEmpty() && apiKey.isEmpty()) {
            statusText = "Commander: not configured";
        } else if (commanderName.isEmpty()) {
            statusText = "Commander: (name missing)";
        } else if (apiKey.isEmpty()) {
            statusText = "Commander: " + commanderName + " (API key missing)";
        } else {
            statusText = "Commander: " + commanderName;
        }

        commanderStatusLabel.setText(statusText);
    }

    private void showCommanderPreferencesDialog() {
        String[] creds = loadCommanderPrefs();
        String currentKey = creds[0];
        String currentCmdr = creds[1];

        JTextField apiField = new JTextField(currentKey, 30);
        JTextField cmdrField = new JTextField(currentCmdr, 30);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(makeLabeled(apiField, "EDSM API Key:"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeLabeled(cmdrField, "Commander Name:"));

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "EDSM Preferences",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            saveCommanderPrefs(apiField.getText(), cmdrField.getText());
            updateCommanderStatusLabel();
        }
    }

    // ============================================================
    // UI HELPERS
    // ============================================================

    private JComponent makeLabeled(JTextField field, String labelText) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        JLabel label = new JLabel(labelText);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        p.add(label);

        // Keep this a single-line height row
        Dimension pref = field.getPreferredSize();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        p.add(field);
        return p;
    }

    /**
     * Label + text field + icon "locate" button.
     * If the icon can't be loaded, falls back to text "Locate".
     */
    private JComponent makeLabeledWithLocate(JTextField field, String labelText) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

        JLabel label = new JLabel(labelText);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

        JButton locateButton;
        if (LOCATE_ICON != null) {
            locateButton = new JButton(LOCATE_ICON);
            locateButton.setBorderPainted(false);
            locateButton.setContentAreaFilled(false);
            locateButton.setFocusPainted(false);

            Dimension iconSize = new Dimension(LOCATE_ICON_SIZE + 4, LOCATE_ICON_SIZE + 4);
            locateButton.setPreferredSize(iconSize);
            locateButton.setMaximumSize(iconSize);
            locateButton.setMinimumSize(iconSize);
        } else {
            locateButton = new JButton("Locate");
        }

        locateButton.setToolTipText("Fill with current commander system from EDSM");

        // Keep text field a single-line height row
        Dimension pref = field.getPreferredSize();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        locateButton.addActionListener(e -> {
            if (!ensureCommanderPrefs()) {
                appendOutput("Commander preferences not set; cannot locate current system.\n");
                return;
            }

            String[] creds = loadCommanderPrefs();
            String apiKey = creds[0];
            String commanderName = creds[1];

            runQueryAsync("getCmdrLastPosition() for locate", () -> {
                CmdrLastPositionResponse resp = client.getCmdrLastPosition(apiKey, commanderName);
                if (resp == null) {
                    return "(no result from getCmdrLastPosition)";
                }
                String system = resp.getSystem();
                if (system != null && !system.isEmpty()) {
                    SwingUtilities.invokeLater(() -> field.setText(system));
                    return "Located commander in system: " + system;
                } else {
                    return "(getCmdrLastPosition returned no system)";
                }
            });
        });

        p.add(label);
        p.add(field);
        p.add(Box.createHorizontalStrut(4));
        p.add(locateButton);

        return p;
    }

    // ============================================================
    // ASYNC + OUTPUT
    // ============================================================

    private void runQueryAsync(String label, QuerySupplier supplier) {
        appendOutput("=== " + label + " ===\n");
        appendOutput("Running query...\n");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return supplier.get();
                } catch (Exception ex) {
                    return "ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    appendOutput(result);
                    appendOutput("\n\n");
                } catch (Exception ex) {
                    appendOutput("ERROR retrieving result: " + ex.getMessage() + "\n");
                }
            }
        }.execute();
    }

    private String toJsonOrMessage(Object obj) {
        if (obj == null) {
            return "(no result / empty response)";
        }
        return gson.toJson(obj);
    }

    private void appendOutput(String text) {
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private interface QuerySupplier {
        String get() throws Exception;
    }

    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EdsmQueryTool tool = new EdsmQueryTool();
            tool.setVisible(true);
        });
    }
}
