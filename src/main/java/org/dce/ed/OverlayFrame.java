package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
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
    private final OverlayContentPanel contentPanel;

    // Crosshair overlay and timer to show mouse position in pass-through mode
    private final CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
    private final Timer crosshairTimer;

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

        // Install crosshair overlay as glass pane (draw-only, no mouse handling)
        setGlassPane(crosshairOverlay);
        crosshairOverlay.setVisible(false); // off until we detect pass-through + hover

        // Poll global mouse position and update crosshair
        crosshairTimer = new Timer(40, e -> updateCrosshair());
        crosshairTimer.start();

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
        contentPanel = new OverlayContentPanel(this);
        add(contentPanel, BorderLayout.CENTER);

        // Load saved bounds if available; otherwise use defaults
        loadBoundsFromPreferences(prefs, PREF_KEY_X, PREF_KEY_Y, PREF_KEY_WIDTH, PREF_KEY_HEIGHT);

        // Add custom resize handler for edges/corners.
        // IMPORTANT: attach recursively so resizing works even when cursor is over child components.
        int dragThickness = calcBorderDragThicknessPx();
        ResizeHandler resizeHandler = new ResizeHandler(this, dragThickness);
        installResizeHandlerRecursive(getRootPane(), resizeHandler);
        installResizeHandlerRecursive(getContentPane(), resizeHandler);
    }

    private static int calcBorderDragThicknessPx() {
        // 96 DPI is typical "100%" baseline on Windows.
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        float scale = dpi / 96.0f;

        // 18px @ 100% feels good; clamp for sanity.
        int px = Math.round(18.0f * scale);
        if (px < 12) {
            px = 12;
        }
        if (px > 36) {
            px = 36;
        }
        return px;
    }

    private static void installResizeHandlerRecursive(Component c, ResizeHandler handler) {
        if (c == null) {
            return;
        }

        c.addMouseListener(handler);
        c.addMouseMotionListener(handler);

        if (c instanceof Container) {
            Container cont = (Container) c;
            for (Component child : cont.getComponents()) {
                installResizeHandlerRecursive(child, handler);
            }
        }
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

    public void applyUiFontPreferences() {
        contentPanel.applyUiFontPreferences();
        revalidate();
        repaint();
    }

    public void applyUiFontPreview(java.awt.Font font) {
        if (font == null) {
            return;
        }
        contentPanel.applyUiFont(font);
        revalidate();
        repaint();
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
        System.out.println("Read " + x + " " + y + " " + w + " " + h);
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
     * Centralized close method: saves bounds then exits.
     */
    public void closeOverlay() {
        saveBoundsToPreferences(PREF_KEY_X, PREF_KEY_Y, PREF_KEY_WIDTH, PREF_KEY_HEIGHT);
        dispose();
        System.exit(0);
    }

    /**
     * Mouse handler that provides resize handles on edges and corners
     * for the undecorated frame.
     */
    private static class ResizeHandler extends MouseAdapter {

        private final int borderDragThickness;

        private final OverlayFrame frame;
        private int dragCursor = Cursor.DEFAULT_CURSOR;
        private boolean dragging = false;

        // Mouse position at press time (screen coords)
        private int dragOffsetX;
        private int dragOffsetY;

        // Frame bounds at press time
        private int dragWidth;
        private int dragHeight;
        private int dragStartX;
        private int dragStartY;

        ResizeHandler(OverlayFrame frame, int borderDragThickness) {
            this.frame = frame;
            this.borderDragThickness = borderDragThickness;
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
                dragStartX = frame.getX();
                dragStartY = frame.getY();
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

            // Always base on the ORIGINAL frame position & size
            int newX = dragStartX;
            int newY = dragStartY;
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
                    newX = dragStartX + dx;
                    newW = dragWidth - dx;
                    break;
                case Cursor.N_RESIZE_CURSOR:
                    newY = dragStartY + dy;
                    newH = dragHeight - dy;
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    newX = dragStartX + dx;
                    newW = dragWidth - dx;
                    newY = dragStartY + dy;
                    newH = dragHeight - dy;
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    newY = dragStartY + dy;
                    newH = dragHeight - dy;
                    newW = dragWidth + dx;
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    newX = dragStartX + dx;
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

            boolean left = x < borderDragThickness;
            boolean right = x >= w - borderDragThickness;
            boolean top = y < borderDragThickness;
            boolean bottom = y >= h - borderDragThickness;

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

    private void updateCrosshair() {
        // If window isn't showing, don't bother
        if (!isShowing()) {
            crosshairOverlay.setVisible(false);
            return;
        }

        // Only show crosshair when pass-through is enabled
        if (!passThroughEnabled) {
            crosshairOverlay.setVisible(false);
            return;
        }

        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi == null) {
            crosshairOverlay.setVisible(false);
            return;
        }

        Point mouseOnScreen = pi.getLocation();
        Point frameOnScreen;
        try {
            frameOnScreen = getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            crosshairOverlay.setVisible(false);
            return;
        }

        int relX = mouseOnScreen.x - frameOnScreen.x;
        int relY = mouseOnScreen.y - frameOnScreen.y;

        // Inside the overlay bounds?
        if (relX >= 0 && relY >= 0 && relX < getWidth() && relY < getHeight()) {
            crosshairOverlay.setCrosshairPoint(new Point(relX, relY));
            if (!crosshairOverlay.isVisible()) {
                crosshairOverlay.setVisible(true);
            }
        } else {
            crosshairOverlay.setVisible(false);
        }
    }

    private static class CrosshairOverlay extends JComponent {

        private Point crosshairPoint;

        CrosshairOverlay() {
            setOpaque(false);
        }

        void setCrosshairPoint(Point p) {
            this.crosshairPoint = p;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (crosshairPoint == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // ED-style orange with some transparency
                g2.setColor(new Color(255, 140, 0, 200));

                int x = crosshairPoint.x;
                int y = crosshairPoint.y;

                // Vertical line
                g2.drawLine(x, 0, x, getHeight());
                // Horizontal line
                g2.drawLine(0, y, getWidth(), y);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public boolean contains(int x, int y) {
            // Critical: don't intercept mouse events.
            return false;
        }
    }
}
