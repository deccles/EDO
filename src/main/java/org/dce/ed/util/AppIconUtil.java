package org.dce.ed.util;


import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public final class AppIconUtil {

    private AppIconUtil() {
        // utility
    }

    /**
     * Sets both the JFrame/window icon and the OS taskbar icon (when supported).
     *
     * @param window any top-level window (JFrame, JDialog, etc.)
     * @param resourcePath classpath resource path, e.g. "/org/dce/ed/icons/app_icon.png"
     */
    public static void applyAppIcon(Window window, String resourcePath) {
        Image icon = loadIcon(resourcePath);
        if (icon == null) {
            return;
        }

        // Window title-bar/icon (also used by Alt-Tab and some task switchers)
        window.setIconImage(icon);

        // Taskbar icon (Windows/macOS supported; Linux varies)
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(icon);
            }
        }
    }

    private static Image loadIcon(String resourcePath) {
        try (InputStream in = AppIconUtil.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                // fallback: sometimes helpful if you're debugging resource paths
                return Toolkit.getDefaultToolkit().getImage(resourcePath);
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
