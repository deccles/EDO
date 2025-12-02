package org.dce.ed.edsm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EdsmQueryTool extends JFrame {
	private String autoCompleteOriginalText;
	
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

    // Shared "system name" fields across tabs
    private final List<JTextField> systemNameFields = new ArrayList<>();
    private JTextField systemTabSystemField;
    private JTextField bodiesTabSystemField;
    private JTextField trafficTabSystemField;

    // Sphere search XYZ fields (System tab)
    private JTextField sphereXField;
    private JTextField sphereYField;
    private JTextField sphereZField;


    private String currentSystemName;

    // Autocomplete UI
    private Timer autoCompleteTimer;
    private JPopupMenu autoCompletePopup;
    private JList<String> autoCompleteList;
    private JTextField autoCompleteTargetField;
    private String pendingAutoCompletePrefix;

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

        // Look up commander position at startup and populate all system fields
        initCommanderSystemAtStartup();
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

        systemTabSystemField = new JTextField(30);
        registerSystemNameField(systemTabSystemField);

        JTextField systemsField = new JTextField(30);

        JTextField xField = new JTextField(6);
        JTextField yField = new JTextField(6);
        JTextField zField = new JTextField(6);
        JTextField radiusField = new JTextField(6);

        sphereXField = xField;
        sphereYField = yField;
        sphereZField = zField;
        
        JButton getSystemButton = new JButton("Get System");
        JButton getSystemsButton = new JButton("Get Systems");
        JButton showSystemButton = new JButton("Show System (info + primary star)");
        JButton sphereSystemsButton = new JButton("Search Sphere");

        // ----- Section: Single system -----
        JPanel singleSystemPanel = new JPanel();
        singleSystemPanel.setLayout(new BoxLayout(singleSystemPanel, BoxLayout.Y_AXIS));
        singleSystemPanel.setBorder(BorderFactory.createTitledBorder("Single system by name"));

        singleSystemPanel.add(makeLabeledWithLocate(systemTabSystemField, "System name:"));
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

        JPanel multiButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        multiButtons.add(getSystemsButton);
        multiSystemPanel.add(multiButtons);

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

        JPanel sphereButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        sphereButtons.add(sphereSystemsButton);
        spherePanel.add(sphereButtons);

        // ----- Assemble tab -----
        panel.add(singleSystemPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(multiSystemPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(spherePanel);

        // ----- Actions -----
        getSystemButton.addActionListener(e -> {
            String name = systemTabSystemField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            setGlobalSystemName(name);
            runQueryAsync("getSystem(" + name + ")", () -> {
                SystemResponse resp = client.getSystem(name);
                String json = toJsonOrMessage(resp);
                // Copy coords into the sphere search fields on the EDT
                SwingUtilities.invokeLater(() -> updateSphereCoordsFromJson(json));
                return json;
            });
        });

        showSystemButton.addActionListener(e -> {
            String name = systemTabSystemField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            setGlobalSystemName(name);
            runQueryAsync("showSystem(" + name + ")", () -> {
                ShowSystemResponse resp = client.showSystem(name);
                String json = toJsonOrMessage(resp);
                SwingUtilities.invokeLater(() -> updateSphereCoordsFromJson(json));
                return json;
            });
        });

        getSystemsButton.addActionListener(e -> {
            String names = systemsField.getText().trim();
            if (names.isEmpty()) {
                appendOutput("Please enter one or more system names.\n");
                return;
            }
            runQueryAsync("getSystems(" + names + ")", () -> {
                // Split on comma and trim each name
                String[] parts = Arrays.stream(names.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
                if (parts.length == 0) {
                    return "(no valid system names provided)";
                }
                SystemResponse[] resp = client.getSystems(parts);
                return toJsonOrMessage(resp);
            });
        });

        sphereSystemsButton.addActionListener(e -> {
            String rText = radiusField.getText().trim();
            if (rText.isEmpty()) {
                appendOutput("Please enter a radius.\n");
                return;
            }

            String centerName = systemTabSystemField.getText().trim();
            if (centerName.isEmpty()) {
                appendOutput("Please enter a system name (or use Locate) for the sphere center.\n");
                return;
            }

            try {
                int radius = Integer.parseInt(rText);

                runQueryAsync("sphereSystemsLocal(" + centerName + "," + radius + ")", () -> {
                    SphereSystemsResponse[] resp = client.sphereSystemsLocal(centerName, radius);
                    return toJsonOrMessage(resp);
                });
            } catch (NumberFormatException ex) {
                appendOutput("Invalid number format for radius.\n");
            }
        });
        return panel;
    }
    /**
     * Parse EDSM system JSON and, if it has coords.x/y/z, copy them into the sphere search fields.
     */
    private void updateSphereCoordsFromJson(String json) {
        if (json == null) {
            return;
        }
        json = json.trim();
        if (json.isEmpty()) {
            return;
        }

        // Only try to parse if it looks like JSON, avoid "(no result / empty response)" etc.
        char c = json.charAt(0);
        if (c != '{' && c != '[') {
            return;
        }

        try {
            JsonElement root = JsonParser.parseString(json);
            JsonObject obj = null;

            if (root.isJsonObject()) {
                obj = root.getAsJsonObject();
            } else if (root.isJsonArray() && root.getAsJsonArray().size() > 0) {
                JsonElement first = root.getAsJsonArray().get(0);
                if (first.isJsonObject()) {
                    obj = first.getAsJsonObject();
                }
            }

            if (obj == null) {
                return;
            }

            JsonObject coords = obj.getAsJsonObject("coords");
            if (coords == null) {
                return;
            }

            if (sphereXField != null && coords.has("x")) {
                sphereXField.setText(coords.get("x").getAsString());
            }
            if (sphereYField != null && coords.has("y")) {
                sphereYField.setText(coords.get("y").getAsString());
            }
            if (sphereZField != null && coords.has("z")) {
                sphereZField.setText(coords.get("z").getAsString());
            }
        } catch (Exception ignored) {
            // If JSON isn't what we expect, just don't update the sphere fields.
        }
    }

    private JPanel createBodiesTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        bodiesTabSystemField = new JTextField(30);
        registerSystemNameField(bodiesTabSystemField);

        JTextField systemIdField = new JTextField(20);

        JButton showBodiesByNameButton = new JButton("Show Bodies");
        JButton showBodiesByIdButton = new JButton("Show Bodies (by ID)");

        // ----- Section: By system name -----
        JPanel byNamePanel = new JPanel();
        byNamePanel.setLayout(new BoxLayout(byNamePanel, BoxLayout.Y_AXIS));
        byNamePanel.setBorder(BorderFactory.createTitledBorder("Bodies by system name"));

        byNamePanel.add(makeLabeledWithLocate(bodiesTabSystemField, "System name:"));
        byNamePanel.add(Box.createVerticalStrut(6));

        JPanel bodiesByNameButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        bodiesByNameButtons.add(showBodiesByNameButton);
        byNamePanel.add(bodiesByNameButtons);


        // ----- Section: By system ID -----
        JPanel byIdPanel = new JPanel();
        byIdPanel.setLayout(new BoxLayout(byIdPanel, BoxLayout.Y_AXIS));
        byIdPanel.setBorder(BorderFactory.createTitledBorder("Bodies by system ID"));

        byIdPanel.add(makeLabeled(systemIdField, "System ID:"));
        byIdPanel.add(Box.createVerticalStrut(6));

        JPanel bodiesByIdButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        bodiesByIdButtons.add(showBodiesByIdButton);
        byIdPanel.add(bodiesByIdButtons);

        // Assemble
        panel.add(byNamePanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(byIdPanel);

        // Actions
        showBodiesByNameButton.addActionListener(e -> {
            String name = bodiesTabSystemField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            setGlobalSystemName(name);
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

        trafficTabSystemField = new JTextField(30);
        registerSystemNameField(trafficTabSystemField);

        JButton trafficButton = new JButton("System Traffic");
        JButton deathsButton = new JButton("System Deaths");
        JButton stationsButton = new JButton("System Stations");
        JButton logsButton = new JButton("System Logs");

        // ----- Section: System activity -----
        JPanel activityPanel = new JPanel();
        activityPanel.setLayout(new BoxLayout(activityPanel, BoxLayout.Y_AXIS));
        activityPanel.setBorder(BorderFactory.createTitledBorder("System activity"));

        activityPanel.add(makeLabeledWithLocate(trafficTabSystemField, "System name:"));
        activityPanel.add(Box.createVerticalStrut(6));

        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));

        buttonRow.add(trafficButton);
        buttonRow.add(Box.createHorizontalStrut(8));
        buttonRow.add(deathsButton);
        buttonRow.add(Box.createHorizontalStrut(16));
        buttonRow.add(stationsButton);
        buttonRow.add(Box.createHorizontalStrut(8));
        buttonRow.add(logsButton);
        buttonRow.add(Box.createHorizontalGlue());

        activityPanel.add(buttonRow);

        activityPanel.add(Box.createVerticalStrut(4));

        panel.add(activityPanel);

        // Actions
        trafficButton.addActionListener(e -> {
            String name = trafficTabSystemField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            setGlobalSystemName(name);
            runQueryAsync("showTraffic(" + name + ")", () -> {
                TrafficResponse resp = client.showTraffic(name);
                return toJsonOrMessage(resp);
            });
        });

        deathsButton.addActionListener(e -> {
            String name = trafficTabSystemField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            setGlobalSystemName(name);
            runQueryAsync("showDeaths(" + name + ")", () -> {
                DeathsResponse resp = client.showDeaths(name);
                return toJsonOrMessage(resp);
            });
        });

        stationsButton.addActionListener(e -> {
            String name = trafficTabSystemField.getText().trim();
            if (name.isEmpty()) {
                appendOutput("Please enter a system name.\n");
                return;
            }
            setGlobalSystemName(name);
            runQueryAsync("getSystemStations(" + name + ")", () -> {
                SystemStationsResponse resp = client.getSystemStations(name);
                return toJsonOrMessage(resp);
            });
        });

        logsButton.addActionListener(e -> {
            String name = trafficTabSystemField.getText().trim();
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

            setGlobalSystemName(name);
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
        JButton cmdrCreditsButton = new JButton("Get Commander Credits");

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

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

        row.add(cmdrLogsButton);
        row.add(Box.createHorizontalStrut(8));
        row.add(cmdrLastPosButton);
        row.add(Box.createHorizontalStrut(16));
        row.add(cmdrRanksButton);
        row.add(Box.createHorizontalStrut(8));
        row.add(cmdrCreditsButton);
        row.add(Box.createHorizontalGlue());

        queriesPanel.add(row);

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
                if (resp != null && resp.getSystem() != null && !resp.getSystem().isEmpty()) {
                    String sys = resp.getSystem();
                    // Update all tabs when manually requested
                    SwingUtilities.invokeLater(() -> setGlobalSystemName(sys));
                }
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

        cmdrCreditsButton.addActionListener(e -> {
            if (!ensureCommanderPrefs()) {
                appendOutput("Commander preferences not set.\n");
                return;
            }
            String[] creds = loadCommanderPrefs();
            String apiKey = creds[0];
            String commanderName = creds[1];

            runQueryAsync("getCmdrCredits()", () -> {
                CmdrCreditsResponse resp = client.getCmdrCredits(apiKey, commanderName);
                return toJsonOrMessage(resp);
            });
        });

        return panel;
    }

    // ============================================================
    // STARTUP COMMANDER SYSTEM
    // ============================================================

    private void initCommanderSystemAtStartup() {
        SwingUtilities.invokeLater(() -> {
            if (!ensureCommanderPrefs()) {
                appendOutput("Commander preferences not set; cannot auto-populate system at startup.\n");
                return;
            }
            String[] creds = loadCommanderPrefs();
            String apiKey = creds[0];
            String commanderName = creds[1];

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    try {
                        CmdrLastPositionResponse resp = client.getCmdrLastPosition(apiKey, commanderName);
                        if (resp != null) {
                            return resp.getSystem();
                        }
                    } catch (IOException | InterruptedException ex) {
                        return null;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        String system = get();
                        if (system != null && !system.isEmpty()) {
                            appendOutput("Startup: commander last known system is " + system + "\n");
                            setGlobalSystemName(system);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }.execute();
        });
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
                    // Update all tabs when locate is pressed
                    SwingUtilities.invokeLater(() -> setGlobalSystemName(system));
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

    /**
     * Register a system-name field so:
     * - it participates in global updates
     * - Enter updates all tabs
     * - typing drives autocomplete (non-intrusively)
     */
    private void registerSystemNameField(JTextField field) {
        systemNameFields.add(field);

        // Single-line height
        Dimension pref = field.getPreferredSize();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        // When user hits Enter in any system field, propagate to all tabs
        field.addActionListener(e -> {
            String text = field.getText().trim();
            if (!text.isEmpty()) {
                setGlobalSystemName(text);
            }
        });

        // Typing -> schedule autocomplete
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                handleSystemNameTyping(field);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                handleSystemNameTyping(field);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                handleSystemNameTyping(field);
            }
        });

        // ESC hides autocomplete popup
        InputMap im = field.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = field.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "autoCompleteHide");
        am.put("autoCompleteHide", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                hideAutoComplete();
            }
        });

        // DOWN arrow -> move focus into the suggestion list (if visible)
        im.put(KeyStroke.getKeyStroke("DOWN"), "autoCompleteDown");
        am.put("autoCompleteDown", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (autoCompletePopup == null) {
                    return;
                }
                if (!autoCompletePopup.isVisible()) {
                    return;
                }
                if (autoCompleteList == null) {
                    return;
                }
                if (autoCompleteList.getModel().getSize() == 0) {
                    return;
                }

                // Remember which field we came from and its text
                autoCompleteTargetField = field;
                autoCompleteOriginalText = field.getText();

                autoCompleteList.setSelectedIndex(0);
                autoCompleteList.ensureIndexIsVisible(0);
                autoCompleteList.requestFocusInWindow();
            }
        });
    }

    private void handleSystemNameTyping(JTextField field) {
        String text = field.getText().trim();
        if (text.length() < 3) {
            pendingAutoCompletePrefix = null;
            hideAutoComplete();
            return;
        }

        autoCompleteTargetField = field;
        pendingAutoCompletePrefix = text;

        if (autoCompleteTimer == null) {
            autoCompleteTimer = new Timer(250, e -> runAutoComplete());
            autoCompleteTimer.setRepeats(false);
        }
        autoCompleteTimer.restart();
    }

    private void runAutoComplete() {
        final JTextField targetField = autoCompleteTargetField;
        final String prefix = pendingAutoCompletePrefix;

        if (targetField == null || prefix == null || prefix.length() < 3) {
            return;
        }

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                try {
                    return fetchSystemNameSuggestions(prefix);
                } catch (Exception e) {
                    return Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    List<String> suggestions = get();
                    // If user kept typing and prefix changed, drop stale results
                    if (prefix.equals(pendingAutoCompletePrefix)) {
                        // Optional debug:
                        // appendOutput("Autocomplete \"" + prefix + "\" -> " + suggestions.size() + " suggestions\n");
                        showAutoCompleteSuggestions(targetField, suggestions);
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }
    
 // Used only for autocomplete JSON parsing
    private static class AutoCompleteSystem {
        String name;
    }

    private List<String> fetchSystemNameSuggestions(String prefix) throws Exception {
        // EDSM API: /api-v1/systems?systemName=prefix (prefix can be "start of a name")
        String encoded = URLEncoder.encode(prefix, StandardCharsets.UTF_8.name());
        String urlStr = "https://www.edsm.net/api-v1/systems?systemName=" + encoded;

        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            if (is == null) {
                return Collections.emptyList();
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String body = sb.toString().trim();
            if (body.isEmpty() || body.equals("[]")) {
                return Collections.emptyList();
            }

            AutoCompleteSystem[] systems = gson.fromJson(body, AutoCompleteSystem[].class);
            if (systems == null || systems.length == 0) {
                return Collections.emptyList();
            }

            List<String> names = new ArrayList<>();
            for (AutoCompleteSystem s : systems) {
                if (s == null || s.name == null || s.name.isEmpty()) {
                    continue;
                }
                names.add(s.name);
            }

            // Deduplicate & cap how many we show
            return names.stream()
                    .distinct()
                    .limit(20)
                    .collect(Collectors.toList());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void showAutoCompleteSuggestions(JTextField field, List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            hideAutoComplete();
            return;
        }

        if (autoCompletePopup == null) {
            autoCompletePopup = new JPopupMenu();
            autoCompletePopup.setFocusable(false); // popup itself doesn't need focus

            autoCompleteList = new JList<>();
            autoCompleteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            autoCompleteList.setFocusable(true);   // must be focusable for arrow keys

            autoCompleteList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                        applyAutoCompleteSelection();
                    }
                }
            });

            JScrollPane sp = new JScrollPane(autoCompleteList);
            sp.setBorder(null);
            sp.setFocusable(false);
            autoCompletePopup.add(sp);

            // Keyboard navigation when the list DOES have focus
            InputMap lim = autoCompleteList.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap lam = autoCompleteList.getActionMap();

            // ENTER -> accept selection
            lim.put(KeyStroke.getKeyStroke("ENTER"), "autoCompleteAccept");
            lam.put("autoCompleteAccept", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    applyAutoCompleteSelection();
                }
            });

            // ESC -> hide popup, return focus to field
            lim.put(KeyStroke.getKeyStroke("ESCAPE"), "autoCompleteEscape");
            lam.put("autoCompleteEscape", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    hideAutoComplete();
                    if (autoCompleteTargetField != null) {
                        autoCompleteTargetField.requestFocusInWindow();
                        autoCompleteTargetField.setCaretPosition(
                                autoCompleteTargetField.getText().length()
                        );
                    }
                }
            });

            // UP -> move up; if already at first item, go back to field and restore original text
            lim.put(KeyStroke.getKeyStroke("UP"), "autoCompleteUp");
            lam.put("autoCompleteUp", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int idx = autoCompleteList.getSelectedIndex();
                    if (idx > 0) {
                        int newIdx = idx - 1;
                        autoCompleteList.setSelectedIndex(newIdx);
                        autoCompleteList.ensureIndexIsVisible(newIdx);
                    } else {
                        // At first item: return to text field
                        if (autoCompleteTargetField != null) {
                            String restore = autoCompleteOriginalText;
                            if (restore == null) {
                                restore = autoCompleteTargetField.getText();
                            }
                            autoCompleteTargetField.setText(restore);
                            autoCompleteTargetField.requestFocusInWindow();
                            autoCompleteTargetField.setCaretPosition(restore.length());
                        }
                        autoCompleteOriginalText = null;
                        hideAutoComplete();
                    }
                }
            });

            // DOWN (when list already has focus) -> move down within the list
            lim.put(KeyStroke.getKeyStroke("DOWN"), "autoCompleteDownInList");
            lam.put("autoCompleteDownInList", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int size = autoCompleteList.getModel().getSize();
                    if (size == 0) {
                        return;
                    }
                    int idx = autoCompleteList.getSelectedIndex();
                    if (idx < 0) {
                        idx = 0;
                    }
                    if (idx < size - 1) {
                        int newIdx = idx + 1;
                        autoCompleteList.setSelectedIndex(newIdx);
                        autoCompleteList.ensureIndexIsVisible(newIdx);
                    }
                    // If already at last item, do nothing (no wrap)
                }
            });
        }

        autoCompleteList.setListData(suggestions.toArray(new String[0]));
        autoCompleteList.setVisibleRowCount(Math.min(8, suggestions.size()));

        int width = Math.max(field.getWidth(), 200);
        int listHeight = autoCompleteList.getPreferredScrollableViewportSize().height;
        if (listHeight <= 0) {
            listHeight = autoCompleteList.getPreferredSize().height;
        }
        if (listHeight <= 0) {
            listHeight = 120; // fallback
        }

        autoCompletePopup.setPopupSize(width, listHeight + 4);

        // Show popup just under the field
        autoCompletePopup.show(field, 0, field.getHeight());

        // Default: keep typing in the field until user hits Down
        field.requestFocusInWindow();
        field.setCaretPosition(field.getText().length());
    }

    private void applyAutoCompleteSelection() {
        if (autoCompleteTargetField == null || autoCompleteList == null) {
            return;
        }
        String selected = autoCompleteList.getSelectedValue();
        if (selected == null || selected.isEmpty()) {
            return;
        }
        autoCompleteTargetField.setText(selected);
        setGlobalSystemName(selected);
        hideAutoComplete();
    }

    private void hideAutoComplete() {
        if (autoCompletePopup != null && autoCompletePopup.isVisible()) {
            autoCompletePopup.setVisible(false);
        }
    }

    private void setGlobalSystemName(String name) {
        if (name == null) {
            return;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        currentSystemName = trimmed;
        for (JTextField f : systemNameFields) {
            if (f != null) {
                f.setText(trimmed);
            }
        }
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
