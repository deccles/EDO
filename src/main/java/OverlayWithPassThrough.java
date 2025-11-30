import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

/**
 * Simple example of a full-screen overlay with a toggleable
 * mouse pass-through mode using JNA on Windows.
 *
 * - Transparent, always-on-top window
 * - Renders some sample overlay text
 * - Button toggles pass-through: when ON, clicks go through to the window below
 */
public class OverlayWithPassThrough extends JWindow {

    private HWND hwnd;
    private boolean passThroughEnabled;

    public OverlayWithPassThrough() {
        super((JFrame) null);
        initUi();
    }

    private void initUi() {
        // Use the full screen size from the default toolkit.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        setBounds(new Rerctangle(0, 0, screenSize.width, screenSize.height));

        // Fully transparent background
        setBackground(new Color(0, 0, 0, 0));

        // Always on top so it sits over Elite Dangerous
        setAlwaysOnTop(true);

        // Do not try to take focus when shown
        setFocusableWindowState(false);

        // Layout: overlay content + small control area
        setLayout(new BorderLayout());

        // Main overlay content (whatever you want to draw)
        OverlayContent overlayContent = new OverlayContent();
        overlayContent.setOpaque(false);
        add(overlayContent, BorderLayout.CENTER);

        // Simple control panel with toggle button (put at TOP to be sure it's visible)
        JPanel controls = new JPanel();
        controls.setOpaque(false);
        JButton toggleButton = new JButton("Pass-Through: OFF");
        controls.add(toggleButton);
        add(controls, BorderLayout.NORTH);

        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passThroughEnabled = !passThroughEnabled;
                applyPassThrough(passThroughEnabled);
                toggleButton.setText("Pass-Through: " + (passThroughEnabled ? "ON" : "OFF"));
            }
        });

        // Show window first, then grab native handle
        setVisible(true);

        // Must be visible to have a native window handle
        try {
            Pointer ptr = Native.getWindowPointer(this);
            if (ptr == null) {
                System.err.println("Failed to obtain native window pointer for overlay window.");
            } else {
                hwnd = new HWND(ptr);
                // Ensure WS_EX_LAYERED is at least set initially
                applyPassThrough(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Apply or remove mouse pass-through (click-through) mode.
     * When enabled, WS_EX_TRANSPARENT is set so mouse events fall through.
     */
    private void applyPassThrough(boolean enable) {
        if (hwnd == null) {
            return;
        }

        int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);

        if (enable) {
            exStyle = exStyle | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
        } else {
            // Keep WS_EX_LAYERED for transparency, just clear TRANSPARENT
            exStyle = exStyle | WinUser.WS_EX_LAYERED;
            exStyle = exStyle & ~WinUser.WS_EX_TRANSPARENT;
        }

        User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);
    }

    /**
     * Simple overlay content component that draws sample text.
     * Replace this with whatever HUD / info you want.
     */
    private static class OverlayContent extends JComponent {

        @Override
        public Dimension getPreferredSize() {
            // Not super important when we explicitly size the window,
            // but keep something reasonable.
            return new Dimension(800, 600);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();

            // Slightly translucent dark background strip for readability
            g.setColor(new Color(255, 0, 0, 128));
            g.fillRoundRect(50, 80, width - 100, 140, 20, 20);

            // Title text
            g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
            g.setColor(Color.WHITE);
            g.drawString("Java Overlay Example", 70, 120);

            // Subtitle / description
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 16f));
            g.drawString("This is a transparent overlay window.", 70, 150);
            g.drawString("Use the button at the top to toggle mouse pass-through.", 70, 175);
            g.drawString("In pass-through mode, clicks will go to whatever is behind this window.", 70, 200);
        }
    }

    public static void main(String[] args) {
        // Run Swing UI on EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new OverlayWithPassThrough();
            }
        });
    }
}
