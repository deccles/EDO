package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

public class OverlayFrame extends JFrame {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 1000;
    private static final int DEFAULT_X = 50;
    private static final int DEFAULT_Y = 50;

    private static final int MIN_WIDTH = 260;
    private static final int MIN_HEIGHT = 200;

    private static final String PREF_KEY_X = "overlay.x";
    private static final String PREF_KEY_Y = "overlay.y";
    private static final String PREF_KEY_WIDTH = "overlay.width";
    private static final String PREF_KEY_HEIGHT = "overlay.height";

    private final Preferences prefs = Preferences.userNodeForPackage(OverlayFrame.class);

    private HWND hwnd;
    private boolean passThroughEnabled;

    private final TitleBarPanel titleBar;

    public OverlayFrame() {
        super("Elite Dangerous Overlay");

        // Need transparency -> undecorated
        setUndecorated(true);

        // Check translucency support (informational)
        Window window = this;
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                               .getDefaultScreenDevice();
        if (!gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
            System.err.println("WARNING: Per-pixel translucency not supported on this device.");
        }

        // Transparent window background
        setBackground(new java.awt.Color(0, 0, 0, 0));

        // Root + content transparent
        getRootPane().setOpaque(false);
        JComponent content = (JComponent) getContentPane();
        content.setOpaque(false);
        content.setBackground(new java.awt.Color(0, 0, 0, 0));

        // Subtle border so you can see the edges
        getRootPane().setBorder(new LineBorder(
                new java.awt.Color(200, 200, 255, 180),
                1,
                true
        ));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        setLayout(new BorderLayout());
        setResizable(true);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        // Save bounds on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeOverlay();
            }
        });

        // Custom title bar (draggable, close button)
        titleBar = new TitleBarPanel(this, "Elite Dangerous Overlay");
        add(titleBar, BorderLayout.NORTH);

        // Transparent content panel with tabbed pane
        OverlayContentPanel contentPanel = new OverlayContentPanel(this);
        add(contentPanel, BorderLayout.CENTER);

        // Load saved bounds if available; otherwise use defaults
        loadBoundsFromPreferences(prefs, PREF_KEY_X, PREF_KEY_Y, PREF_KEY_WIDTH, PREF_KEY_HEIGHT);

     // Add custom resize handler for edges/corners
        ResizeHandler resizeHandler = new ResizeHandler(this);
        JComponent root = getRootPane();
        root.addMouseListener(resizeHandler);
        root.addMouseMotionListener(resizeHandler);

        // Also attach to the title bar so top-edge drags on the bar can resize
        titleBar.addMouseListener(resizeHandler);
        titleBar.addMouseMotionListener(resizeHandler);
    }

    public void showOverlay() {
        setVisible(true);

        try {
            Pointer ptr = Native.getWindowPointer(this);
            if (ptr == null) {
                System.err.println("Failed to obtain native window pointer for overlay window.");
            } else {
                hwnd = new HWND(ptr);
                applyPassThrough(false);
                System.out.println(
                        "Overlay size: " + getWidth() + "x" + getHeight()
                                + " at (" + getX() + "," + getY() + ")"
                );
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void togglePassThrough() {
        passThroughEnabled = !passThroughEnabled;
        applyPassThrough(passThroughEnabled);
        titleBar.setPassThrough(passThroughEnabled); // hide/show X
        System.out.println("Pass-through " + (passThroughEnabled ? "ENABLED" : "DISABLED"));
        repaint();
    }

    public boolean isPassThroughEnabled() {
        return passThroughEnabled;
    }

    private void applyPassThrough(boolean enable) {
        if (hwnd == null) {
            return;
        }

        int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);

        if (enable) {
            exStyle = exStyle | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
        } else {
            exStyle = exStyle | WinUser.WS_EX_LAYERED;
            exStyle = exStyle & ~WinUser.WS_EX_TRANSPARENT;
        }

        User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);
    }

    public void loadBoundsFromPreferences(
            Preferences prefs,
            String keyX,
            String keyY,
            String keyWidth,
            String keyHeight
    ) {
        int x = prefs.getInt(keyX, DEFAULT_X);
        int y = prefs.getInt(keyY, DEFAULT_Y);
        int w = prefs.getInt(keyWidth, DEFAULT_WIDTH);
        int h = prefs.getInt(keyHeight, DEFAULT_HEIGHT);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (w > screenSize.width) {
            w = screenSize.width;
        }
        if (h > screenSize.height) {
            h = screenSize.height;
        }

        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (x + w > screenSize.width) {
            x = screenSize.width - w;
        }
        if (y + h > screenSize.height) {
            y = screenSize.height - h;
        }
        System.out.println("Read " + x +" " + y + " " + w + " " + h);
        setBounds(x, y, w, h);
    }

    public void saveBoundsToPreferences(
            String keyX,
            String keyY,
            String keyWidth,
            String keyHeight
    ) {
        prefs.putInt(keyX, getX());
        prefs.putInt(keyY, getY());
        prefs.putInt(keyWidth, getWidth());
        prefs.putInt(keyHeight, getHeight());
        
        System.out.println("Saved : " + getX() + " " + getY() + " " + getWidth() + " " + getHeight());
    }

    /**
     * Mouse handler that provides resize handles on edges and corners
     * for the undecorated frame.
     */

    /**
     * Centralized close method: saves bounds then exits.
     */
    public void closeOverlay() {
        saveBoundsToPreferences(PREF_KEY_X, PREF_KEY_Y, PREF_KEY_WIDTH, PREF_KEY_HEIGHT);
        dispose();
        System.exit(0);
    }

    private static class ResizeHandler extends MouseAdapter {

        private static final int BORDER_DRAG_THICKNESS = 12;

        private final OverlayFrame frame;
        private int dragCursor = Cursor.DEFAULT_CURSOR;
        private boolean dragging = false;
        private int dragOffsetX;
        private int dragOffsetY;
        private int dragWidth;
        private int dragHeight;

        ResizeHandler(OverlayFrame frame) {
            this.frame = frame;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (dragging) {
                return;
            }
            int cursor = calcCursor(e);
            frame.setCursor(Cursor.getPredefinedCursor(cursor));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (!dragging) {
                frame.setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            dragCursor = calcCursor(e);
            if (dragCursor != Cursor.DEFAULT_CURSOR && SwingUtilities.isLeftMouseButton(e)) {
                dragging = true;
                dragOffsetX = e.getXOnScreen();
                dragOffsetY = e.getYOnScreen();
                dragWidth = frame.getWidth();
                dragHeight = frame.getHeight();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
            frame.setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging) {
                return;
            }

            int dx = e.getXOnScreen() - dragOffsetX;
            int dy = e.getYOnScreen() - dragOffsetY;

            int newX = frame.getX();
            int newY = frame.getY();
            int newW = dragWidth;
            int newH = dragHeight;

            switch (dragCursor) {
                case Cursor.E_RESIZE_CURSOR:
                    newW = dragWidth + dx;
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    newH = dragHeight + dy;
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    newW = dragWidth + dx;
                    newH = dragHeight + dy;
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    newX = frame.getX() + dx;
                    newW = dragWidth - dx;
                    break;
                case Cursor.N_RESIZE_CURSOR:
                    newY = frame.getY() + dy;
                    newH = dragHeight - dy;
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    newX = frame.getX() + dx;
                    newW = dragWidth - dx;
                    newY = frame.getY() + dy;
                    newH = dragHeight - dy;
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    newY = frame.getY() + dy;
                    newH = dragHeight - dy;
                    newW = dragWidth + dx;
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    newX = frame.getX() + dx;
                    newW = dragWidth - dx;
                    newH = dragHeight + dy;
                    break;
                default:
                    break;
            }

            // Enforce minimum size
            if (newW < frame.getMinimumSize().width) {
                int diff = frame.getMinimumSize().width - newW;
                if (dragCursor == Cursor.W_RESIZE_CURSOR ||
                    dragCursor == Cursor.NW_RESIZE_CURSOR ||
                    dragCursor == Cursor.SW_RESIZE_CURSOR) {
                    newX -= diff;
                }
                newW = frame.getMinimumSize().width;
            }
            if (newH < frame.getMinimumSize().height) {
                int diff = frame.getMinimumSize().height - newH;
                if (dragCursor == Cursor.N_RESIZE_CURSOR ||
                    dragCursor == Cursor.NE_RESIZE_CURSOR ||
                    dragCursor == Cursor.NW_RESIZE_CURSOR) {
                    newY -= diff;
                }
                newH = frame.getMinimumSize().height;
            }

            frame.setBounds(newX, newY, newW, newH);
        }

        private int calcCursor(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            int w = frame.getWidth();
            int h = frame.getHeight();

            boolean left = x < BORDER_DRAG_THICKNESS;
            boolean right = x >= w - BORDER_DRAG_THICKNESS;
            boolean top = y < BORDER_DRAG_THICKNESS;
            boolean bottom = y >= h - BORDER_DRAG_THICKNESS;

            if (left && top) {
                return Cursor.NW_RESIZE_CURSOR;
            } else if (left && bottom) {
                return Cursor.SW_RESIZE_CURSOR;
            } else if (right && top) {
                return Cursor.NE_RESIZE_CURSOR;
            } else if (right && bottom) {
                return Cursor.SE_RESIZE_CURSOR;
            } else if (left) {
                return Cursor.W_RESIZE_CURSOR;
            } else if (right) {
                return Cursor.E_RESIZE_CURSOR;
            } else if (top) {
                return Cursor.N_RESIZE_CURSOR;
            } else if (bottom) {
                return Cursor.S_RESIZE_CURSOR;
            } else {
                return Cursor.DEFAULT_CURSOR;
            }
        }
    }
}
