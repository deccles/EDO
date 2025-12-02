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
import java.util.prefs.Preferences;

public class EdsmQueryTool extends JFrame {

    private static final String PREF_KEY_EDSM_API = "edsmApiKey";
    private static final String PREF_KEY_EDSM_CMDR = "edsmCommanderName";

    // Put your PNG at: src/main/resources/org/dce/ed/edsm/icon_locate.png
    private static final String LOCATE_ICON_PATH = "/org/dce/ed/edsm/locate_icon.png";
    private static final ImageIcon LOCATE_ICON = loadLocateIcon();

    private final EdsmClient client;
    private final Gson gson;

    private final JTextArea outputArea;

    public EdsmQueryTool() {
        super("EDSM Query Tool");

        this.client = new EdsmClient();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("System", createSystemTab());
        tabs.addTab("Bodies", createBodiesTab());
        tabs.addTab("Traffic / Logs", createTrafficTab());
        tabs.addTab("Commander", createCommanderTab());

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setPreferredSize(new Dimension(800, 400));

        add(tabs, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private static ImageIcon loadLocateIcon() {
        java.net.URL url = EdsmQueryTool.class.getResource(LOCATE_ICON_PATH);
        if (url != null) {
            return new ImageIcon(url);
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
        JButton getSystemsButton = new JButton("Get Systems (comma-separated)");
        JButton showSystemButton = new JButton("Show System (info + primary star)");
        JButton sphereSystemsButton = new JButton("Sphere Systems");

        // Single-system name with Locate icon button
        panel.add(makeLabeledWithLocate(systemNameField, "System name:"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(getSystemButton);
        panel.add(Box.createVerticalStrut(4));
        panel.add(showSystemButton);
        panel.add(Box.createVerticalStrut(8));

        // Multi-systems (no locate)
        panel.add(makeLabeled(systemsField, "Multiple system names (comma-separated):"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(getSystemsButton);
        panel.add(Box.createVerticalStrut(8));

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
        coordsPanel.add(Box.createHorizontalStrut(4));
        coordsPanel.add(new JLabel("Radius (ly):"));
        coordsPanel.add(radiusField);

        panel.add(coordsPanel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(sphereSystemsButton);

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
                String[] split = names.split(",");
                for (int i = 0; i < split.length; i++) {
                    split[i] = split[i].trim();
                }
                SystemResponse[] resp = client.getSystems(split);
                return toJsonOrMessage(resp);
            });
        });

        sphereSystemsButton.addActionListener(e -> {
            String xText = xField.getText().trim();
            String yText = yField.getText().trim();
            String zText = zField.getText().trim();
            String rText = radiusField.getText().trim();

            if (xText.isEmpty() || yText.isEmpty() || zText.isEmpty() || rText.isEmpty()) {
                appendOutput("Please enter x, y, z and radius.\n");
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

        JButton showBodiesByNameButton = new JButton("Show Bodies (by system name)");
        JButton showBodiesByIdButton = new JButton("Show Bodies (by system ID)");

        panel.add(makeLabeledWithLocate(systemNameField, "System name:"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(showBodiesByNameButton);
        panel.add(Box.createVerticalStrut(8));

        panel.add(makeLabeled(systemIdField, "System ID:"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(showBodiesByIdButton);

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

        panel.add(makeLabeledWithLocate(systemNameField, "System name:"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(trafficButton);
        panel.add(Box.createVerticalStrut(4));
        panel.add(deathsButton);
        panel.add(Box.createVerticalStrut(4));
        panel.add(stationsButton);
        panel.add(Box.createVerticalStrut(4));
        panel.add(logsButton);

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

        JButton prefsButton = new JButton("Preferences…");
        JButton cmdrLogsButton = new JButton("Get Cmdr Logs");
        JButton cmdrLastPosButton = new JButton("Get Cmdr Last Position");
        JButton cmdrRanksButton = new JButton("Get Cmdr Ranks/Stats");

        panel.add(prefsButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(cmdrLogsButton);
        panel.add(Box.createVerticalStrut(4));
        panel.add(cmdrLastPosButton);
        panel.add(Box.createVerticalStrut(4));
        panel.add(cmdrRanksButton);

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
        } else {
            locateButton = new JButton("Locate");
        }

        locateButton.setToolTipText("Fill with current commander system from EDSM");

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
