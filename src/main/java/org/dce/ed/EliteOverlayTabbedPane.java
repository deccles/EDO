package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.LiveJournalMonitor;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FssDiscoveryScanEvent;
import org.dce.ed.logreader.event.StartJumpEvent;
import org.dce.ed.logreader.event.StatusEvent;

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

    private static final int TAB_HOVER_DELAY_MS = 500;

    private static final Color TAB_ORANGE = new Color(255, 140, 0, 220);
    private static final Color TAB_WHITE = new Color(255, 255, 255, 230);

    // Restores the original "bigger" tab look (padding inside the outline)
    private static final Insets TAB_PADDING = new Insets(4, 10, 4, 10);

    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    private final RouteTabPanel routeTab;
    private final SystemTabPanel systemTab;
    private final BiologyTabPanel biologyTab;

    private JButton routeButton;
    private JButton systemButton;
    private JButton biologyButton;

    public EliteOverlayTabbedPane() {
        super(new BorderLayout());

        boolean opaque = !OverlayPreferences.isOverlayTransparent();

        setOpaque(opaque);

        // ----- Tab bar (row of buttons) -----
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        tabBar.setOpaque(opaque);

        ButtonGroup group = new ButtonGroup();

        routeButton = createTabButton("Route");
        systemButton = createTabButton("System");
        biologyButton = createTabButton("Biology");

        group.add(routeButton);
        group.add(systemButton);
        group.add(biologyButton);

        tabBar.add(routeButton);
        tabBar.add(systemButton);
        tabBar.add(biologyButton);

        // ----- Card area with the actual tab contents -----
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(opaque);
        cardPanel.setBackground(Color.black);
        cardPanel.setPreferredSize(new Dimension(400, 1000));

        // Create tab content panels
        this.routeTab = new RouteTabPanel();
        this.systemTab = new SystemTabPanel();
        this.biologyTab = new BiologyTabPanel();
        this.biologyTab.setSystemTabPanel(systemTab);

        cardPanel.add(routeTab, CARD_ROUTE);
        cardPanel.add(systemTab, CARD_SYSTEM);
        cardPanel.add(biologyTab, CARD_BIOLOGY);

        systemButton.setSelected(true);
        applyTabButtonStyle(routeButton);
        applyTabButtonStyle(systemButton);
        applyTabButtonStyle(biologyButton);
        systemTab.refreshFromCache();

        // Wire up buttons to show cards
        routeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTab(CARD_ROUTE, routeButton);
            }
        });

        systemButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTab(CARD_SYSTEM, systemButton);
            }
        });

        biologyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTab(CARD_BIOLOGY, biologyButton);
            }
        });

        // Hover-to-switch: resting over a tab for a short time activates it
        installHoverSwitch(routeButton, TAB_HOVER_DELAY_MS, () -> routeButton.doClick());
        installHoverSwitch(systemButton, TAB_HOVER_DELAY_MS, () -> systemButton.doClick());
        installHoverSwitch(biologyButton, TAB_HOVER_DELAY_MS, () -> biologyButton.doClick());

        // Select Route tab by default
        systemButton.doClick();

        add(tabBar, BorderLayout.NORTH);

        // Hook live journal monitoring into tabs (existing behavior)
        try {
            LiveJournalMonitor monitor = LiveJournalMonitor.getInstance(EliteDangerousOverlay.clientKey);

            monitor.addListener(event -> {

                this.handleLogEvent(event);

                if (event instanceof StatusEvent) {
                    StatusEvent flagEvent = (StatusEvent) event;

                    if (flagEvent.isFsdCharging()) {
                        showRouteTabFromStatusWatcher();
                    }
                }

                systemTab.handleLogEvent(event);
                routeTab.handleLogEvent(event);
                biologyTab.handleLogEvent(event);
            });

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

    private void handleLogEvent(EliteLogEvent event) {
        if (event instanceof FsdJumpEvent
                || event instanceof FssDiscoveryScanEvent) {
            showSystemTabFromStatusWatcher();
        }
        if (event instanceof StartJumpEvent) {
            showRouteTabFromStatusWatcher();
        }
    }

    /**
     * Attach a generic hover handler to a button; when the mouse rests over
     * the button for the given delay, the action is invoked on the EDT.
     */
    private static void installHoverSwitch(JButton button, int delayMs, Runnable action) {
        TabHoverPoller.register(button, delayMs, action);
    }

    /**
     * Global tab hover poller: periodically polls the global mouse position and,
     * if it is resting on any registered tab button longer than the configured
     * delay, invokes that tab's action (typically button.doClick()).
     *
     * This works even when the overlay is in OS pass-through mode because it
     * does not depend on Swing mouse events.
     */
    private static class TabHoverPoller implements ActionListener {

        private static final int POLL_INTERVAL_MS = 40;

        private static final List<Entry> entries = new ArrayList<>();
        private static final Timer pollTimer;

        static {
            TabHoverPoller listener = new TabHoverPoller();
            pollTimer = new Timer(POLL_INTERVAL_MS, listener);
            pollTimer.start();
        }

        private static class Entry {
            final JButton button;
            final int delayMs;
            final Runnable action;

            long hoverStartMs = -1L;
            boolean firedForCurrentHover = false;

            Entry(JButton button, int delayMs, Runnable action) {
                this.button = button;
                this.delayMs = delayMs;
                this.action = action;
            }
        }

        static void register(JButton button, int delayMs, Runnable action) {
            entries.add(new Entry(button, delayMs, action));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (entries.isEmpty()) {
                return;
            }

            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            if (pointerInfo == null) {
                resetAll();
                return;
            }

            Point mouseOnScreen = pointerInfo.getLocation();
            long now = System.currentTimeMillis();

            for (Entry entry : entries) {
                JButton button = entry.button;
                if (button == null || !button.isShowing()) {
                    entry.hoverStartMs = -1L;
                    entry.firedForCurrentHover = false;
                    continue;
                }

                Point buttonLoc;
                try {
                    buttonLoc = button.getLocationOnScreen();
                } catch (IllegalStateException ex) {
                    entry.hoverStartMs = -1L;
                    entry.firedForCurrentHover = false;
                    continue;
                }

                Rectangle bounds = new Rectangle(
                        buttonLoc.x,
                        buttonLoc.y,
                        button.getWidth(),
                        button.getHeight()
                );

                if (bounds.contains(mouseOnScreen)) {
                    if (entry.hoverStartMs < 0L) {
                        entry.hoverStartMs = now;
                        entry.firedForCurrentHover = false;
                    } else if (!entry.firedForCurrentHover && now - entry.hoverStartMs >= entry.delayMs) {
                        if (entry.action != null) {
                            SwingUtilities.invokeLater(entry.action);
                        }
                        entry.firedForCurrentHover = true;
                    }
                } else {
                    entry.hoverStartMs = -1L;
                    entry.firedForCurrentHover = false;
                }
            }
        }

        private static void resetAll() {
            for (Entry entry : entries) {
                entry.hoverStartMs = -1L;
                entry.firedForCurrentHover = false;
            }
        }
    }

    private JButton createTabButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));

        // Slightly translucent dark background so tabs are legible but not huge blocks
        button.setOpaque(!OverlayPreferences.isOverlayTransparent());
        button.setBackground(new Color(50, 50, 50, 220));

        applyTabButtonStyle(button);
        return button;
    }

    private javax.swing.border.Border createTabBorder(Color c) {
        return javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(c, 1, true),
                javax.swing.BorderFactory.createEmptyBorder(
                        TAB_PADDING.top,
                        TAB_PADDING.left,
                        TAB_PADDING.bottom,
                        TAB_PADDING.right
                )
        );
    }

    private void selectTab(String cardName, JButton selectedButton) {
        if (routeButton != null) {
            routeButton.setSelected(selectedButton == routeButton);
        }
        if (systemButton != null) {
            systemButton.setSelected(selectedButton == systemButton);
        }
        if (biologyButton != null) {
            biologyButton.setSelected(selectedButton == biologyButton);
        }

        applyTabButtonStyle(routeButton);
        applyTabButtonStyle(systemButton);
        applyTabButtonStyle(biologyButton);

        cardLayout.show(cardPanel, cardName);
    }

    private void applyTabButtonStyle(JButton button) {
        if (button == null) {
            return;
        }

        Color c = button.isSelected() ? TAB_WHITE : TAB_ORANGE;

        // This restores size/padding compared to a bare LineBorder.
        button.setMargin(TAB_PADDING);
        button.setForeground(c);
        button.setBorder(createTabBorder(c));
    }

    private void showRouteTabFromStatusWatcher() {
        SwingUtilities.invokeLater(() -> selectTab(CARD_ROUTE, routeButton));
    }

    private void showSystemTabFromStatusWatcher() {
        SwingUtilities.invokeLater(() -> selectTab(CARD_SYSTEM, systemButton));
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

    private static class HoverSwitchHandler extends MouseAdapter {

        private final Timer hoverTimer;
        private final Runnable action;

        HoverSwitchHandler(int delayMs, Runnable action) {
            this.action = action;
            this.hoverTimer = new Timer(delayMs, e -> {
                if (this.action != null) {
                    this.action.run();
                }
            });
            this.hoverTimer.setRepeats(false);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            hoverTimer.restart();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            hoverTimer.restart();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hoverTimer.stop();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            hoverTimer.stop();
        }
    }

    public void applyUiFontPreferences() {
        systemTab.applyUiFontPreferences();
        routeTab.applyUiFontPreferences();
        biologyTab.applyUiFontPreferences();
        revalidate();
        repaint();
    }

    public void applyUiFont(Font font) {
        systemTab.applyUiFont(font);
        routeTab.applyUiFont(font);
        biologyTab.applyUiFont(font);
        revalidate();
        repaint();
    }
}
