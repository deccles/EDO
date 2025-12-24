package org.dce.ed.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ExoStarTypeInspector {

    // ---- Cache POJOs that match .edOverlaySystems.json ----

    public static class CacheSystem {
        String systemName;
        long systemAddress;
        double[] starPos;
        List<CacheBody> bodies;
    }

    public static class CacheBody {
        String name;
        int bodyId;
        String starSystem;
        double[] starPos;

        Double distanceLs;
        Boolean landable;
        Boolean hasBio;
        Boolean hasGeo;
        Boolean highValue;

        String atmoOrType;
        Double surfaceTempK;

        Integer parentStarBodyId; // can be null
        String starType;          // only set for stars (usually)
        Integer numberOfBioSignals;
    }

    // ---- GUI preferences keys ----

    private static final String PREF_CACHE_PATH = "cachePath";
    private static final String PREF_SYSTEM_NAME = "systemName";
    private static final String PREF_BODY_SPEC = "bodySpec";
    private static final String PREF_WIN_X = "winX";
    private static final String PREF_WIN_Y = "winY";
    private static final String PREF_WIN_W = "winW";
    private static final String PREF_WIN_H = "winH";

    // ---- Main ----

    public static void main(String[] args) throws Exception {
        if (args.length >= 3) {
            runCli(args);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            createAndShowGui();
        });
    }

    private static void runCli(String[] args) throws Exception {
        Path cachePath = Path.of(args[0]);
        String systemName = args[1];
        String bodySpec = args[2];

        String report = inspect(cachePath, systemName, bodySpec, true);

        if (report == null || report.isBlank()) {
            return;
        }

        System.out.println(report);
    }

    // ---- GUI ----

    private static void createAndShowGui() {
        Preferences prefs = Preferences.userNodeForPackage(ExoStarTypeInspector.class);

        JFrame frame = new JFrame("Exo Star Type Inspector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel inputs = new JPanel(new BorderLayout(8, 8));
        inputs.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel row1 = new JPanel(new BorderLayout(8, 8));
        JLabel cacheLabel = new JLabel("Cache JSON:");
        JTextField cachePathField = new JTextField(prefs.get(PREF_CACHE_PATH, ""));
        JButton browseBtn = new JButton("Browse...");
        row1.add(cacheLabel, BorderLayout.WEST);
        row1.add(cachePathField, BorderLayout.CENTER);
        row1.add(browseBtn, BorderLayout.EAST);

        JPanel row2 = new JPanel(new BorderLayout(8, 8));
        JLabel systemLabel = new JLabel("System Name:");
        JTextField systemField = new JTextField(prefs.get(PREF_SYSTEM_NAME, ""));
        row2.add(systemLabel, BorderLayout.WEST);
        row2.add(systemField, BorderLayout.CENTER);

        JPanel row3 = new JPanel(new BorderLayout(8, 8));
        JLabel bodyLabel = new JLabel("Body Spec:");
        JTextField bodyField = new JTextField(prefs.get(PREF_BODY_SPEC, ""));
        row3.add(bodyLabel, BorderLayout.WEST);
        row3.add(bodyField, BorderLayout.CENTER);

        JPanel topRows = new JPanel();
        topRows.setLayout(new javax.swing.BoxLayout(topRows, javax.swing.BoxLayout.Y_AXIS));
        topRows.add(row1);
        topRows.add(javax.swing.Box.createVerticalStrut(6));
        topRows.add(row2);
        topRows.add(javax.swing.Box.createVerticalStrut(6));
        topRows.add(row3);

        inputs.add(topRows, BorderLayout.NORTH);

        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setLineWrap(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(output);
        scroll.setPreferredSize(new Dimension(900, 520));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton runBtn = new JButton("Run");
        JButton clearBtn = new JButton("Clear");
        buttons.add(runBtn);
        buttons.add(clearBtn);

        inputs.add(buttons, BorderLayout.SOUTH);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(inputs, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);

        browseBtn.addActionListener((ActionEvent e) -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select cache JSON (e.g., .edOverlaySystems.json)");
            String current = cachePathField.getText();
            if (current != null && !current.isBlank()) {
                fc.setSelectedFile(Path.of(current).toFile());
            }

            int result = fc.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                cachePathField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        Runnable doRun = () -> {
            String cachePathText = safeTrim(cachePathField.getText());
            String systemName = safeTrim(systemField.getText());
            String bodySpec = safeTrim(bodyField.getText());

            if (cachePathText.isBlank() || systemName.isBlank() || bodySpec.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please enter cache path, system name, and body spec.",
                        "Missing Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Save last-used values immediately
            prefs.put(PREF_CACHE_PATH, cachePathText);
            prefs.put(PREF_SYSTEM_NAME, systemName);
            prefs.put(PREF_BODY_SPEC, bodySpec);

            Path cachePath = Path.of(cachePathText);

            try {
                String report = inspect(cachePath, systemName, bodySpec, false);
                output.append(report);
                if (!report.endsWith("\n")) {
                    output.append("\n");
                }
                output.append("\n");
                output.setCaretPosition(output.getDocument().getLength());
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg == null || msg.isBlank()) {
                    msg = ex.getClass().getName();
                }
                JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        };

        runBtn.addActionListener((ActionEvent e) -> {
            doRun.run();
        });

        clearBtn.addActionListener((ActionEvent e) -> {
            output.setText("");
        });

        // Enter to run from any input field
        cachePathField.addActionListener((ActionEvent e) -> {
            doRun.run();
        });
        systemField.addActionListener((ActionEvent e) -> {
            doRun.run();
        });
        bodyField.addActionListener((ActionEvent e) -> {
            doRun.run();
        });

        restoreWindowBounds(frame, prefs);
        frame.pack();
        ensureMinSize(frame, 900, 600);
        frame.setVisible(true);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveWindowBounds(frame, prefs);
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                saveWindowBounds(frame, prefs);
            }
        });
    }

    private static void restoreWindowBounds(JFrame frame, Preferences prefs) {
        int w = prefs.getInt(PREF_WIN_W, -1);
        int h = prefs.getInt(PREF_WIN_H, -1);
        int x = prefs.getInt(PREF_WIN_X, Integer.MIN_VALUE);
        int y = prefs.getInt(PREF_WIN_Y, Integer.MIN_VALUE);

        if (w > 0 && h > 0) {
            frame.setSize(new Dimension(w, h));
        }

        if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
            frame.setLocation(x, y);
        } else {
            frame.setLocationRelativeTo(null);
        }
    }

    private static void saveWindowBounds(Window w, Preferences prefs) {
        if (w == null) {
            return;
        }

        prefs.putInt(PREF_WIN_X, w.getX());
        prefs.putInt(PREF_WIN_Y, w.getY());
        prefs.putInt(PREF_WIN_W, w.getWidth());
        prefs.putInt(PREF_WIN_H, w.getHeight());
    }

    private static void ensureMinSize(JFrame frame, int minW, int minH) {
        Dimension d = frame.getSize();
        int w = d.width;
        int h = d.height;

        if (w < minW) {
            w = minW;
        }
        if (h < minH) {
            h = minH;
        }

        frame.setSize(new Dimension(w, h));
    }

    private static String safeTrim(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    // ---- Core inspect (shared by CLI + GUI) ----

    private static String inspect(Path cachePath, String systemName, String bodySpec, boolean includeUsageOnError) throws IOException {
        List<CacheSystem> systems = loadCache(cachePath);

        CacheSystem system = systems.stream()
                .filter(s -> s != null && s.systemName != null && s.systemName.equals(systemName))
                .findFirst()
                .orElse(null);

        if (system == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("System not found: ").append(systemName).append("\n");
            appendClosestSystemNames(sb, systems, systemName);

            if (includeUsageOnError) {
                sb.append("\nUsage:\n");
                sb.append("  java ExoStarTypeInspector <pathToCacheJson> <systemName> <bodySpec>\n");
            }

            return sb.toString();
        }

        if (system.bodies == null || system.bodies.isEmpty()) {
            return "System has no bodies in cache: " + systemName + "\n";
        }

        String fullBodyName = normalizeBodyName(systemName, bodySpec);
        CacheBody body = findBody(system, fullBodyName, bodySpec);

        if (body == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Body not found.\n");
            sb.append("  systemName: ").append(systemName).append("\n");
            sb.append("  bodySpec:   ").append(bodySpec).append("\n");
            sb.append("  tried full: ").append(fullBodyName).append("\n\n");
            sb.append("Body name suggestions:\n");
            appendBodyNameSuggestions(sb, system, bodySpec);
            return sb.toString();
        }

        StarResolution r = resolveHostStar(system, body);
        return buildReport(system, body, r);
    }

    // ---- Report building ----

    private static String buildReport(CacheSystem system, CacheBody body, StarResolution r) {
        StringBuilder sb = new StringBuilder();
        sb.append("System: ").append(system.systemName).append(" (address=").append(system.systemAddress).append(")\n");
        sb.append("Body matched:\n");
        sb.append("  name:              ").append(body.name).append("\n");
        sb.append("  bodyId:            ").append(body.bodyId).append("\n");
        sb.append("  landable:          ").append(body.landable).append("\n");
        sb.append("  distanceLs:        ").append(body.distanceLs).append("\n");
        sb.append("  atmoOrType:        ").append(body.atmoOrType).append("\n");
        sb.append("  parentStarBodyId:  ").append(body.parentStarBodyId).append("\n");
        sb.append("  starType(on body): ").append(body.starType).append("\n");
        sb.append("\n");

        sb.append("Resolved host star:\n");
        sb.append("  method:            ").append(r.method).append("\n");
        sb.append("  starName:          ").append(r.star != null ? r.star.name : null).append("\n");
        sb.append("  starBodyId:        ").append(r.star != null ? r.star.bodyId : null).append("\n");
        sb.append("  starType:          ").append(r.starType).append("\n");

        return sb.toString();
    }

    // ---- Resolution logic ----

    private static class StarResolution {
        String method;
        CacheBody star;
        String starType;
    }

    private static StarResolution resolveHostStar(CacheSystem system, CacheBody body) {
        StarResolution r = new StarResolution();

        // Case 1: body is a star
        if (body.starType != null && !body.starType.isBlank()) {
            r.method = "body-is-star";
            r.star = body;
            r.starType = body.starType;
            return r;
        }

        // Case 2: parentStarBodyId points to a star body
        Integer psid = body.parentStarBodyId;
        if (psid != null) {
            CacheBody star = findBodyById(system, psid);
            if (star != null && star.starType != null && !star.starType.isBlank()) {
                r.method = "parentStarBodyId";
                r.star = star;
                r.starType = star.starType;
                return r;
            }
        }

        // Case 3: fallback by naming convention
        // Prefer:
        //  - exact system name star
        //  - else systemName + " A"
        //  - else any star with distanceLs==0
        CacheBody bySystemName = findBodyByExactName(system, system.systemName);
        if (bySystemName != null && bySystemName.starType != null && !bySystemName.starType.isBlank()) {
            r.method = "fallback-name-systemName";
            r.star = bySystemName;
            r.starType = bySystemName.starType;
            return r;
        }

        CacheBody byA = findBodyByExactName(system, system.systemName + " A");
        if (byA != null && byA.starType != null && !byA.starType.isBlank()) {
            r.method = "fallback-name-systemName-A";
            r.star = byA;
            r.starType = byA.starType;
            return r;
        }

        CacheBody byDistance = system.bodies.stream()
                .filter(b -> b != null)
                .filter(b -> b.starType != null && !b.starType.isBlank())
                .filter(b -> b.distanceLs != null && b.distanceLs.doubleValue() == 0.0)
                .findFirst()
                .orElse(null);

        r.method = "fallback-first-star-distanceLs==0";
        r.star = byDistance;
        if (byDistance != null) {
            r.starType = byDistance.starType;
        }
        return r;
    }

    // ---- Find helpers ----

    private static CacheBody findBodyById(CacheSystem system, int bodyId) {
        if (system.bodies == null) {
            return null;
        }
        for (CacheBody b : system.bodies) {
            if (b == null) {
                continue;
            }
            if (b.bodyId == bodyId) {
                return b;
            }
        }
        return null;
    }

    private static CacheBody findBodyByExactName(CacheSystem system, String name) {
        if (name == null || system.bodies == null) {
            return null;
        }
        for (CacheBody b : system.bodies) {
            if (b == null || b.name == null) {
                continue;
            }
            if (b.name.equals(name)) {
                return b;
            }
        }
        return null;
    }

    private static CacheBody findBody(CacheSystem system, String fullBodyName, String bodySpec) {
        // 1) exact by full name
        CacheBody exact = findBodyByExactName(system, fullBodyName);
        if (exact != null) {
            return exact;
        }

        // 2) exact by bodySpec as-is (if they provided full name already)
        CacheBody exactSpec = findBodyByExactName(system, bodySpec);
        if (exactSpec != null) {
            return exactSpec;
        }

        // 3) suffix match: ends with " " + bodySpec (so "S171 9 A 2" matches bodySpec "A 2")
        String suffix = " " + bodySpec;
        CacheBody suffixMatch = system.bodies.stream()
                .filter(b -> b != null && b.name != null)
                .filter(b -> b.name.endsWith(suffix))
                .findFirst()
                .orElse(null);

        if (suffixMatch != null) {
            return suffixMatch;
        }

        // 4) loose contains match
        String needle = bodySpec.toLowerCase(Locale.ROOT);
        return system.bodies.stream()
                .filter(b -> b != null && b.name != null)
                .filter(b -> b.name.toLowerCase(Locale.ROOT).contains(needle))
                .findFirst()
                .orElse(null);
    }

    private static void appendBodyNameSuggestions(StringBuilder sb, CacheSystem system, String bodySpec) {
        String needle = bodySpec.toLowerCase(Locale.ROOT);

        List<String> matches = new ArrayList<>();
        for (CacheBody b : system.bodies) {
            if (b == null || b.name == null) {
                continue;
            }
            String n = b.name.toLowerCase(Locale.ROOT);
            if (n.contains(needle) || n.endsWith(" " + needle)) {
                matches.add(b.name);
            }
        }

        matches.stream()
                .sorted()
                .limit(30)
                .forEach(n -> sb.append("  ").append(n).append("\n"));

        if (matches.isEmpty()) {
            sb.append("  (no close name matches found)\n");
        }
    }

    private static void appendClosestSystemNames(StringBuilder sb, List<CacheSystem> systems, String systemName) {
        String needle = systemName.toLowerCase(Locale.ROOT);

        List<String> candidates = systems.stream()
                .filter(Objects::nonNull)
                .map(s -> s.systemName)
                .filter(Objects::nonNull)
                .filter(n -> n.toLowerCase(Locale.ROOT).contains(needle) || needle.contains(n.toLowerCase(Locale.ROOT)))
                .distinct()
                .sorted()
                .limit(20)
                .toList();

        if (candidates.isEmpty()) {
            sb.append("No similar system names found (simple contains match).\n");
            return;
        }

        sb.append("Similar system names:\n");
        for (String n : candidates) {
            sb.append("  ").append(n).append("\n");
        }
    }

    private static String normalizeBodyName(String systemName, String bodySpec) {
        if (bodySpec == null) {
            return systemName;
        }

        String s = bodySpec.trim();
        if (s.isEmpty()) {
            return systemName;
        }

        if (s.startsWith(systemName)) {
            return s;
        }

        return systemName + " " + s;
    }

    // ---- Load ----

    private static List<CacheSystem> loadCache(Path path) throws IOException {
        Gson gson = new GsonBuilder().create();
        try (Reader r = Files.newBufferedReader(path)) {
            CacheSystem[] arr = gson.fromJson(r, CacheSystem[].class);
            List<CacheSystem> out = new ArrayList<>();
            if (arr != null) {
                for (CacheSystem s : arr) {
                    out.add(s);
                }
            }
            return out;
        }
    }
}
