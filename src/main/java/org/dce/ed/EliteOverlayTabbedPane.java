package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Custom transparent "tabbed pane" for the overlay.
 * Does not extend JTabbedPane to avoid opaque background painting.
 *
 * Tabs: Route, System, Biology.
 */
public class EliteOverlayTabbedPane extends JPanel {

    private static final String CARD_ROUTE = "ROUTE";
    private static final String CARD_SYSTEM = "SYSTEM";
    private static final String CARD_BIOLOGY = "BIOLOGY";
    private static final String CARD_LOG = "LOG";

    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    public EliteOverlayTabbedPane() {
        super(new BorderLayout());
        setOpaque(false);

        // ----- Tab bar (row of buttons) -----
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        tabBar.setOpaque(false);

        ButtonGroup group = new ButtonGroup();

        JButton routeButton = createTabButton("Route");
        JButton systemButton = createTabButton("System");
        JButton biologyButton = createTabButton("Biology");
        JButton logButton = createTabButton("Log");
        
        group.add(routeButton);
        group.add(systemButton);
        group.add(biologyButton);
        group.add(logButton);
        
        tabBar.add(routeButton);
        tabBar.add(systemButton);
        tabBar.add(biologyButton);
        tabBar.add(logButton);
        
        // ----- Card area with the actual tab contents -----
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(400, 1000));

        // Create tab content panels
        RouteTabPanel routeTab = new RouteTabPanel();
        SystemTabPanel systemTab = new SystemTabPanel();
        BiologyTabPanel biologyTab = new BiologyTabPanel();
        LogTabPanel logTab = new LogTabPanel();

        cardPanel.add(routeTab, CARD_ROUTE);
        cardPanel.add(systemTab, CARD_SYSTEM);
        cardPanel.add(biologyTab, CARD_BIOLOGY);
        cardPanel.add(logTab, CARD_LOG);

        systemButton.setSelected(true);
        systemTab.refreshFromCache();
        
        // Wire up buttons to show cards
        routeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_ROUTE);
            }
        });

        systemButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_SYSTEM);
            }
        });

        biologyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_BIOLOGY);
            }
        });

        logButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_LOG);
            }
        });
        
        // Select Route tab by default
        systemButton.doClick();

        add(tabBar, BorderLayout.NORTH);
        
        // Hook live journal monitoring into System tab (existing behavior)
        try {
            org.dce.ed.logreader.LiveJournalMonitor monitor =
                    org.dce.ed.logreader.LiveJournalMonitor.getInstance();

            monitor.addListener(event -> {
                // Log tab (if you added a live handler there)
//                 logTab.handleLogEvent(event);

                // System tab
                systemTab.handleLogEvent(event);
            });

//            monitor.start(); // if your monitor requires an explicit start
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        // Start watcher that syncs tabs with in-game Galaxy/System map
        GuiFocusWatcher watcher = new GuiFocusWatcher(this);
        Thread watcherThread = new Thread(watcher, "ED-GuiFocusWatcher");
        watcherThread.setDaemon(true);
        watcherThread.start();

        add(cardPanel, BorderLayout.CENTER);
    }

    private JButton createTabButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        // Slightly translucent dark background so tabs are legible but not huge blocks
        button.setOpaque(true);
        button.setBackground(new Color(50, 50, 50, 220));
        button.setForeground(Color.WHITE);
        return button;
    }

    private void showRouteTabFromStatusWatcher() {
        SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, CARD_ROUTE));
    }

    private void showSystemTabFromStatusWatcher() {
        SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, CARD_SYSTEM));
    }

    /**
     * Watches Elite Dangerous Status.json and switches tabs when the player
     * opens the Galaxy Map (Route tab) or System Map (System tab).
     */
    private static class GuiFocusWatcher implements Runnable {

        private static final long POLL_INTERVAL_MS = 200L;

        private final EliteOverlayTabbedPane parent;
        private final Path statusPath;
        private final Gson gson = new Gson();

        private volatile boolean running = true;
        private int lastGuiFocus = -1;

        GuiFocusWatcher(EliteOverlayTabbedPane parent) {
            this.parent = parent;

            String home = System.getProperty("user.home");
            this.statusPath = Path.of(
                    home,
                    "Saved Games",
                    "Frontier Developments",
                    "Elite Dangerous",
                    "Status.json");
        }

        @Override
        public void run() {
            while (running) {
                try {
                    pollOnce();
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    // Status.json may not exist yet or be briefly locked; ignore and retry
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        private void pollOnce() throws IOException {
            if (!Files.exists(statusPath)) {
                return;
            }

            try (Reader reader = Files.newBufferedReader(statusPath, StandardCharsets.UTF_8)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("GuiFocus")) {
                    return;
                }

                int guiFocus = root.get("GuiFocus").getAsInt();
                if (guiFocus != lastGuiFocus) {
                    handleGuiFocusChange(guiFocus);
                    lastGuiFocus = guiFocus;
                }
            }
        }

        private void handleGuiFocusChange(int guiFocus) {
            // 6 = Galaxy Map -> Route tab
            if (guiFocus == 6) {
                parent.showRouteTabFromStatusWatcher();
            }
            // 7 = System Map -> System tab
            else if (guiFocus == 7) {
                parent.showSystemTabFromStatusWatcher();
            }
        }
    }
}
