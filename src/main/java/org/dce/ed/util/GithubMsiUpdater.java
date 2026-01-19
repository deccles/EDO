package org.dce.ed.util;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class GithubMsiUpdater {

	private static final String APP_FOLDER_NAME = "EDO Overlay";
	private static final String APP_EXE_NAME = "EDO Overlay.exe";
	
    private static final String OWNER = "deccles";
    private static final String REPO = "EDO";

    private static final String GROUP_ID = "org.dce";
    private static final String ARTIFACT_ID = "EliteDangerousOverlay";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);

    private GithubMsiUpdater() {
    }

    public static void checkAndUpdate(Component parent) {
        Cursor oldCursor = parent != null ? parent.getCursor() : null;
        if (parent != null) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private String currentVersion;
            private String latestVersion;
            private String latestHtmlUrl;
            private String msiName;
            private String msiUrl;

            private Exception failure;

            @Override
            protected Void doInBackground() {
                try {
                    currentVersion = readCurrentVersion();
                    JsonObject latest = fetchLatestReleaseJson();
                    latestVersion = readVersionFromTag(latest);
                    latestHtmlUrl = getString(latest, "html_url");

                    JsonObject msiAsset = findMsiAsset(latest);
                    if (msiAsset != null) {
                        msiName = getString(msiAsset, "name");
                        msiUrl = getString(msiAsset, "browser_download_url");
                    }

                } catch (Exception ex) {
                    failure = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (parent != null && oldCursor != null) {
                    parent.setCursor(oldCursor);
                }

                if (failure != null) {
                    JOptionPane.showMessageDialog(parent,
                            "Unable to check for updates:\n" + safeMessage(failure),
                            "Update Check Failed",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (latestVersion == null || latestVersion.isBlank()) {
                    JOptionPane.showMessageDialog(parent,
                            "Unable to determine latest release version from GitHub.",
                            "Update Check Failed",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (msiUrl == null || msiUrl.isBlank()) {
                    JOptionPane.showMessageDialog(parent,
                            "Latest release was found (" + latestVersion + "), but no MSI asset was attached.\n"
                                    + "Open the Releases page and download the MSI manually:\n"
                                    + (latestHtmlUrl != null ? latestHtmlUrl : "https://github.com/" + OWNER + "/" + REPO + "/releases"),
                            "No MSI Found",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // If we couldn't read current version, still allow updating.
                boolean newer = true;
                if (currentVersion != null && !currentVersion.isBlank()) {
                    newer = compareVersions(latestVersion, currentVersion) > 0;
                }

                if (!newer) {
                    JOptionPane.showMessageDialog(parent,
                            "You're already on the latest version.\n\n"
                                    + "Current: " + nullToUnknown(currentVersion) + "\n"
                                    + "Latest: " + latestVersion,
                            "No Update Available",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                int choice = JOptionPane.showConfirmDialog(parent,
                        "Update available.\n\n"
                                + "Current: " + nullToUnknown(currentVersion) + "\n"
                                + "Latest: " + latestVersion + "\n\n"
                                + "Download and run installer now?\n\n"
                                + "MSI: " + msiName,
                        "Update Available",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }

                downloadAndInstall(parent, msiUrl, msiName);
            }
        };

        worker.execute();
    }

    private static void downloadAndInstall(Component parent, String msiUrl, String msiName) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Downloading Update", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel label = new JLabel("Downloading " + (msiName != null ? msiName : "installer") + "...");
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);

        dialog.getContentPane().setLayout(new java.awt.BorderLayout(10, 10));
        dialog.getContentPane().add(label, java.awt.BorderLayout.NORTH);
        dialog.getContentPane().add(bar, java.awt.BorderLayout.CENTER);
        dialog.setSize(420, 110);
        dialog.setLocationRelativeTo(parent);

        SwingWorker<Path, Integer> dlWorker = new SwingWorker<Path, Integer>() {

            private Exception failure;

            @Override
            protected Path doInBackground() {
                try {
                    return downloadFile(msiUrl, bar, label);
                } catch (Exception ex) {
                    failure = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                dialog.dispose();

                if (failure != null) {
                    JOptionPane.showMessageDialog(parent,
                            "Unable to download update:\n" + safeMessage(failure),
                            "Download Failed",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Path downloaded;
                try {
                    downloaded = get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent,
                            "Unable to download update:\n" + safeMessage(ex),
                            "Download Failed",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (downloaded == null || !Files.isRegularFile(downloaded)) {
                    JOptionPane.showMessageDialog(parent,
                            "Download did not produce a valid MSI file.",
                            "Download Failed",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    // Make a small temp cmd that installs, then relaunches.
                    Path script = createInstallAndRelaunchScript(downloaded);

                    // Kick it off detached, then exit the overlay so files aren't locked.
                    new ProcessBuilder("cmd", "/c", "\"" + script.toAbsolutePath().toString() + "\"")
                    .start();

                    System.exit(0);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent,
                            "Downloaded installer, but couldn't launch it:\n" + safeMessage(ex) + "\n\n"
                                    + "File:\n" + downloaded.toAbsolutePath(),
                            "Install Launch Failed",
                            JOptionPane.ERROR_MESSAGE);
                }

            }
        };

        dlWorker.execute();
        dialog.setVisible(true);
    }

    private static Path downloadFile(String url, JProgressBar bar, JLabel label) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(HTTP_TIMEOUT)
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "EDO-Overlay-Updater")
                .GET()
                .build();

        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        int status = resp.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " downloading MSI");
        }

        long len = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (len > 0) {
            SwingUtilities.invokeLater(() -> {
                bar.setIndeterminate(false);
                bar.setMinimum(0);
                bar.setMaximum(1000);
            });
        } else {
            SwingUtilities.invokeLater(() -> bar.setIndeterminate(true));
        }

        Path tmp = Files.createTempFile("EDO-Overlay-", ".msi");
        tmp.toFile().deleteOnExit();

        try (InputStream in = resp.body()) {
            byte[] buf = new byte[128 * 1024];
            long readTotal = 0;

            try (var out = Files.newOutputStream(tmp)) {
                int r;
                while ((r = in.read(buf)) >= 0) {
                    if (r == 0) {
                        continue;
                    }
                    out.write(buf, 0, r);
                    readTotal += r;

                    if (len > 0) {
                        final long rt = readTotal;
                        SwingUtilities.invokeLater(() -> {
                            int v = (int) Math.min(1000L, (rt * 1000L) / len);
                            bar.setValue(v);
                            label.setText("Downloading... " + (v / 10) + "%");
                        });
                    }
                }
            }
        }

        return tmp;
    }

    private static JsonObject fetchLatestReleaseJson() throws Exception {
        String api = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(HTTP_TIMEOUT)
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(api))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "EDO-Overlay-Updater")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = resp.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " calling GitHub releases/latest");
        }

        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    private static JsonObject findMsiAsset(JsonObject release) {
        JsonElement assetsEl = release.get("assets");
        if (assetsEl == null || !assetsEl.isJsonArray()) {
            return null;
        }

        JsonArray assets = assetsEl.getAsJsonArray();
        for (JsonElement el : assets) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject a = el.getAsJsonObject();
            String name = getString(a, "name");
            if (name == null) {
                continue;
            }
            if (name.toLowerCase(Locale.ROOT).endsWith(".msi")) {
                return a;
            }
        }
        return null;
    }

    private static String readVersionFromTag(JsonObject release) {
        String tag = getString(release, "tag_name");
        if (tag == null) {
            return null;
        }
        tag = tag.trim();
        if (tag.startsWith("v") || tag.startsWith("V")) {
            tag = tag.substring(1);
        }
        return tag;
    }

    private static String readCurrentVersion() {
        // Try package manifest first
        try {
            String v = GithubMsiUpdater.class.getPackage().getImplementationVersion();
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        } catch (Exception ignored) {
        }

        // Fall back to pom.properties inside the jar
        String pomPropsPath = "/META-INF/maven/" + GROUP_ID + "/" + ARTIFACT_ID + "/pom.properties";
        try (InputStream in = GithubMsiUpdater.class.getResourceAsStream(pomPropsPath)) {
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            String v = props.getProperty("version");
            return v != null ? v.trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        try {
            return el.getAsString();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Compare dotted version numbers like 0.0.8 vs 0.0.10.
     * Returns >0 if a>b, <0 if a<b, 0 if equal/unknown.
     */
    private static int compareVersions(String a, String b) {
        if (a == null || b == null) {
            return 0;
        }
        String aa = a.trim();
        String bb = b.trim();

        String[] ap = aa.split("[^0-9]+");
        String[] bp = bb.split("[^0-9]+");

        int n = Math.max(ap.length, bp.length);
        for (int i = 0; i < n; i++) {
            int ai = (i < ap.length && !ap[i].isBlank()) ? safeInt(ap[i]) : 0;
            int bi = (i < bp.length && !bp[i].isBlank()) ? safeInt(bp[i]) : 0;

            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }
        return 0;
    }

    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return 0;
        }
    }

    private static String nullToUnknown(String v) {
        if (v == null || v.isBlank()) {
            return "(unknown)";
        }
        return v;
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        if (m == null || m.isBlank()) {
            return t.getClass().getSimpleName();
        }
        return m;
    }
    
    private static Path createInstallAndRelaunchScript(Path msi) throws IOException {
        String temp = System.getenv("TEMP");
        if (temp == null || temp.isBlank()) {
            temp = System.getProperty("java.io.tmpdir");
        }

        Path script = Files.createTempFile(Path.of(temp), "EDO-Overlay-Update-", ".cmd");

        String programFiles = System.getenv("ProgramFiles");
        if (programFiles == null || programFiles.isBlank()) {
            programFiles = "C:\\Program Files";
        }

        Path installDir = Path.of(programFiles, "EDO Overlay");
        Path exe = installDir.resolve("EDO Overlay.exe");

        String scriptText =
                "@echo off\r\n" +
                "setlocal\r\n" +
                "echo Waiting for EDO Overlay to exit...\r\n" +
                "timeout /t 2 /nobreak >nul\r\n" +

                // Ensure no running instances remain
                "taskkill /IM \"EDO Overlay.exe\" /F >nul 2>&1\r\n" +

                "echo Installing update...\r\n" +
                "msiexec /i \"" + msi.toAbsolutePath() + "\" /passive /norestart\r\n" +

                "echo Relaunching EDO Overlay...\r\n" +
                "if exist \"" + exe.toAbsolutePath() + "\" (\r\n" +
                "  start \"\" \"" + exe.toAbsolutePath() + "\"\r\n" +
                ") else (\r\n" +
                "  start \"\" \"" + installDir.toAbsolutePath() + "\"\r\n" +
                ")\r\n" +
                "endlocal\r\n";

        Files.writeString(script, scriptText, java.nio.charset.Charset.forName("windows-1252"));
        script.toFile().deleteOnExit();
        return script;
    }
    
}
