package org.dce.ed;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.dce.ed.logreader.RescanJournalsMain;
import org.dce.ed.tts.PollyTtsCached;
import org.dce.ed.tts.TtsSprintf;
import org.dce.ed.ui.ConsoleMonitor;
import org.dce.ed.util.AppIconUtil;
import org.dce.ed.util.GithubMsiUpdater;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
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

    private final OverlayContentPanel contentPanel;
    private final OverlayFrame passThroughFrame;
    private final DecoratedOverlayDialog decoratedDialog;

    private volatile boolean passThroughMode;

    public EliteDangerousOverlay() {
        this.prefs = Preferences.userNodeForPackage(EliteDangerousOverlay.class);
        this.passThroughMode = true;
        this.contentPanel = new OverlayContentPanel(() -> passThroughMode);

        this.passThroughFrame = new OverlayFrame(contentPanel);
        this.passThroughFrame.setPassThroughEnabled(true);

        this.decoratedDialog = new DecoratedOverlayDialog(passThroughFrame, contentPanel, clientKey);
        this.decoratedDialog.setOnRequestSwitchToPassThrough(() -> SwingUtilities.invokeLater(() -> setPassThroughMode(true)));

        UIManager.put("TitlePane.background", new Color(30, 30, 30));
        UIManager.put("TitlePane.foreground", Color.ORANGE);
        
        AppIconUtil.applyAppIcon(passThroughFrame, "/org/dce/ed/edsm/locate_icon.png");
        GithubMsiUpdater.checkForUpdatesOnStartup(passThroughFrame);
    }

    public static void main(String[] args) throws IOException {

        System.out.println("EDO Overlay version: " + getAppVersion());

        ConsoleMonitor consoleMonitor = ConsoleMonitor.getInstance(1000);
        consoleMonitor.redirectOutput();

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
        passThroughFrame.showOverlay();
     // Pre-warm the decorated window so the first F9 toggle doesn't jump.
        prewarmDecoratedDialog();

        // Save bounds and clean up on close
        passThroughFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
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

    private void setPassThroughMode(boolean enablePassThrough) {
    	if (this.passThroughMode == enablePassThrough) {
    		return;
    	}

    	// Determine current bounds from the currently-visible window.
    	java.awt.Rectangle bounds = this.passThroughMode
    			? passThroughFrame.getBounds()
    			: decoratedDialog.getBounds();

    	java.awt.Window fromWindow = this.passThroughMode ? passThroughFrame : decoratedDialog;
    	java.awt.Window toWindow = enablePassThrough ? passThroughFrame : decoratedDialog;

    	// 1) Prep + show the target window first (reduces compositor "blank" / pop).
    	toWindow.setBounds(bounds);

    	if (toWindow == passThroughFrame) {
    		passThroughFrame.setPassThroughEnabled(true);
    	}

    	toWindow.setVisible(true);
    	toWindow.toFront();

    	// 2) Reparent content with minimal churn.
    	contentPanel.setVisible(false);

    	if (contentPanel.getParent() != null) {
    		contentPanel.getParent().remove(contentPanel);
    	}

    	if (toWindow == passThroughFrame) {
    		passThroughFrame.add(contentPanel, java.awt.BorderLayout.CENTER);
    	} else {
    		// attachContent() should remove+add contentPanel into the decorated frame.
    		passThroughFrame.setPassThroughEnabled(false);
    		passThroughFrame.setVisible(false);

    		decoratedDialog.setBounds(bounds);      // set bounds FIRST
    		decoratedDialog.attachContent();        // then attach content
    		decoratedDialog.applyOverlayBackgroundFromPreferences(false);
    		decoratedDialog.setVisible(true);
    		decoratedDialog.toFront();
    	}

    	contentPanel.setVisible(true);

    	// 3) Apply visuals after attach (prevents a flash of old background/font).
    	if (toWindow instanceof OverlayUiPreviewHost) {
    		OverlayUiPreviewHost host = (OverlayUiPreviewHost) toWindow;
    		host.applyOverlayBackgroundFromPreferences(enablePassThrough);
    		host.applyUiFontPreferences();
    	}

    	toWindow.validate();
    	toWindow.repaint();

    	// 4) Hide the old window last (avoid flicker).
    	fromWindow.setVisible(false);

    	// 5) Final state.
    	this.passThroughMode = enablePassThrough;

    	// If we're switching to the decorated window, make sure pass-through is disabled.
    	if (!enablePassThrough) {
    		passThroughFrame.setPassThroughEnabled(false);
    	}
    }

    //
    // Global key listener: F9 toggles between click-through overlay and a normal decorated window.
    //
    @Override
    public void nativeKeyPressed(com.github.kwhat.jnativehook.keyboard.NativeKeyEvent e) {
        int toggleKey = OverlayPreferences.getPassThroughToggleKeyCode();
        if (toggleKey > 0 && e.getKeyCode() == toggleKey) {
            SwingUtilities.invokeLater(() -> setPassThroughMode(!passThroughMode));
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
    
    private void prewarmDecoratedDialog() {
    	SwingUtilities.invokeLater(() -> {
    		// Make sure the peer is created.
    		decoratedDialog.addNotify();

    		// Put it somewhere not visible.
    		java.awt.Rectangle b = passThroughFrame.getBounds();
    		decoratedDialog.setBounds(b.x, b.y + 3000, b.width, b.height);

    		// Show once at essentially invisible opacity (decorated window stays opaque for normal use).
    		try {
    			decoratedDialog.setOpacity(0.01f);
    		} catch (Exception ignored) {
    		}

    		decoratedDialog.setVisible(true);

    		// Immediately hide again; restore opacity.
    		decoratedDialog.setVisible(false);
    		try {
    			decoratedDialog.setOpacity(1.0f);
    		} catch (Exception ignored) {
    		}
    	});
    }

}
