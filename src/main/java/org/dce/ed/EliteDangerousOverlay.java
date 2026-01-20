package org.dce.ed;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.dce.ed.logreader.RescanJournalsMain;
import org.dce.ed.tts.PollyTtsCached;
import org.dce.ed.tts.TtsSprintf;
import org.dce.ed.ui.ConsoleMonitor;
import org.dce.ed.util.AppIconUtil;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class EliteDangerousOverlay implements NativeKeyListener {

    private static final String PREF_WINDOW_X = "windowX";
    private static final String PREF_WINDOW_Y = "windowY";
    private static final String PREF_WINDOW_WIDTH = "windowWidth";
    private static final String PREF_WINDOW_HEIGHT = "windowHeight";
    public static String clientKey = "EDO";

    private static final String MAVEN_GROUP_ID = "org.dce";
    private static final String MAVEN_ARTIFACT_ID = "EliteDangerousOverlay";

    private final Preferences prefs;
    private final OverlayFrame overlayFrame;

    public EliteDangerousOverlay() {
        this.prefs = Preferences.userNodeForPackage(EliteDangerousOverlay.class);
        this.overlayFrame = new OverlayFrame();

        AppIconUtil.applyAppIcon(overlayFrame, "/org/dce/ed/edsm/locate_icon.png");
    }

    public static void main(String[] args) {

        System.out.println("EDO Overlay version: " + getAppVersion());

        ConsoleMonitor consoleMonitor = ConsoleMonitor.getInstance(1000);
        consoleMonitor.redirectOutput();
        
        String commander = "villanous";

        TtsSprintf ttsSprintf = new TtsSprintf(new PollyTtsCached());
        ttsSprintf.speakf("Welcome commander");

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            RescanJournalsMain.rescanJournals(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            EliteDangerousOverlay app = new EliteDangerousOverlay();
            app.start();
        });
    }

    private static String getAppVersion() {
        // 1) If you set Implementation-Version in the manifest, this will be populated.
        try {
            String v = EliteDangerousOverlay.class.getPackage().getImplementationVersion();
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        } catch (Exception ignored) {
        }

        // 2) Maven embeds pom.properties inside the JAR.
        String pomPropsPath = "/META-INF/maven/" + MAVEN_GROUP_ID + "/" + MAVEN_ARTIFACT_ID + "/pom.properties";
        try (InputStream in = EliteDangerousOverlay.class.getResourceAsStream(pomPropsPath)) {
            if (in == null) {
                return "(unknown)";
            }
            Properties props = new Properties();
            props.load(in);
            String v = props.getProperty("version");
            if (v == null || v.isBlank()) {
                return "(unknown)";
            }
            return v.trim();
        } catch (Exception ignored) {
            return "(unknown)";
        }
    }

    private void start() {
        overlayFrame.showOverlay();

        // Save bounds and clean up on close
        overlayFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
//                overlayFrame.saveBoundsToPreferences(
//                        PREF_WINDOW_X,
//                        PREF_WINDOW_Y,
//                        PREF_WINDOW_WIDTH,
//                        PREF_WINDOW_HEIGHT
//                );

                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
                    // Not critical if this fails during shutdown
                    ex.printStackTrace();
                }
            }
        });

        // Quiet JNativeHook logging
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            ex.printStackTrace();
            return;
        }

        GlobalScreen.addNativeKeyListener(this);
    }

    //
    // Global key listener: F9 toggles overlay pass-through
    //
    @Override
    public void nativeKeyPressed(com.github.kwhat.jnativehook.keyboard.NativeKeyEvent e) {
        int toggleKey = OverlayPreferences.getPassThroughToggleKeyCode();
        if (toggleKey > 0 && e.getKeyCode() == toggleKey) {
            overlayFrame.togglePassThrough();
        }
    }

    @Override
    public void nativeKeyReleased(com.github.kwhat.jnativehook.keyboard.NativeKeyEvent e) {
        // not used
    }

    @Override
    public void nativeKeyTyped(com.github.kwhat.jnativehook.keyboard.NativeKeyEvent e) {
        // not used
    }
}
