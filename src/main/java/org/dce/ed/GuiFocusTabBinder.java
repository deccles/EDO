package org.dce.ed;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Watches Elite Dangerous Status.json and switches tabs when the Galaxy/System
 * maps are opened in-game.
 *
 * GuiFocus values (relevant ones):
 *   6 = Galaxy Map  -> Route tab
 *   7 = System Map  -> System tab
 */
public class GuiFocusTabBinder implements Runnable {

    private static final long POLL_INTERVAL_MS = 200L;

    private final Path statusPath;
    private final JTabbedPane tabbedPane;
    private final int routeTabIndex;
    private final int systemTabIndex;
    private final Gson gson = new Gson();

    private volatile boolean running = true;
    private int lastGuiFocus = -1;
    private Thread thread;

    /**
     * @param statusPath     Path to Status.json
     * @param tabbedPane     Your main JTabbedPane (with Route/System tabs)
     * @param routeTabIndex  Index of the Route tab in the JTabbedPane
     * @param systemTabIndex Index of the System tab in the JTabbedPane
     */
    public GuiFocusTabBinder(Path statusPath,
                             JTabbedPane tabbedPane,
                             int routeTabIndex,
                             int systemTabIndex) {

        this.statusPath = statusPath;
        this.tabbedPane = tabbedPane;
        this.routeTabIndex = routeTabIndex;
        this.systemTabIndex = systemTabIndex;
    }

    /**
     * Convenience for the typical Status.json location on Windows.
     */
    public static Path defaultStatusPath() {
        String home = System.getProperty("user.home");
        return Path.of(home,
                "Saved Games",
                "Frontier Developments",
                "Elite Dangerous",
                "Status.json");
    }

    public void start() {
        if (thread != null && thread.isAlive()) {
            return;
        }

        running = true;
        thread = new Thread(this, "ED-GuiFocusTabBinder");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
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
                // Status.json might not exist yet or be briefly locked; ignore and retry
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
            if (root == null) {
                return;
            }

            if (!root.has("GuiFocus")) {
                return;
            }

            int guiFocus = root.get("GuiFocus").getAsInt();
            if (guiFocus != lastGuiFocus) {
                handleGuiFocusChange(lastGuiFocus, guiFocus);
                lastGuiFocus = guiFocus;
            }
        }
    }

    private void handleGuiFocusChange(int oldFocus, int newFocus) {
        // Galaxy Map opened
        if (oldFocus != 6 && newFocus == 6) {
            selectTab(routeTabIndex);
        }

        // System Map opened
        if (oldFocus != 7 && newFocus == 7) {
            selectTab(systemTabIndex);
        }

        // You can add "closed" handling here if you ever want to switch back
        // when GuiFocus returns to 0 or something else.
    }

    private void selectTab(int index) {
        if (index < 0) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (index >= 0 && index < tabbedPane.getTabCount()) {
                tabbedPane.setSelectedIndex(index);
            }
        });
    }
}
