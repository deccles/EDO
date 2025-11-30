package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Custom "title bar" for the undecorated overlay frame.
 * - Shows a title
 * - Lets you drag the window
 * - Provides a custom-painted close button (red box with X)
 * - Provides a custom-painted gear button (opens Preferences)
 */
public class TitleBarPanel extends JPanel {
    private static final int TOP_RESIZE_STRIP = 6;
    
    private final OverlayFrame frame;
    private Point dragOffset;
    private final CloseButton closeButton;
    private final SettingsButton settingsButton;

    public TitleBarPanel(OverlayFrame frame, String title) {
        this.frame = frame;

        setOpaque(true);
        setBackground(new Color(32, 32, 32, 230));
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(titleLabel);

        closeButton = new CloseButton();
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    frame.closeOverlay();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setHover(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setHover(false);
            }
        });

        settingsButton = new SettingsButton();
        settingsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    PreferencesDialog dialog = new PreferencesDialog(frame);
                    dialog.setLocationRelativeTo(frame);
                    dialog.setVisible(true);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                settingsButton.setHover(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                settingsButton.setHover(false);
            }
        });

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        rightPanel.setOpaque(false);
        rightPanel.add(settingsButton);
        rightPanel.add(closeButton);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        // Tall enough that nothing gets clipped even with DPI scaling
        setPreferredSize(new Dimension(100, 32));

        // Drag-to-move behavior
        // How many pixels from the very top we reserve for "resize", not "drag"
        MouseAdapter dragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                // If we’re in the very top strip, this is a “resize zone” – let ResizeHandler handle it.
                if (e.getY() <= TOP_RESIZE_STRIP) {
                    dragOffset = null;
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }

                // Normal title-bar drag: use screen coords relative to frame origin
                java.awt.Point screen = e.getLocationOnScreen();
                dragOffset = new Point(screen.x - frame.getX(), screen.y - frame.getY());
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset = null;
                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset == null) {
                    return;
                }

                int newX = e.getXOnScreen() - dragOffset.x;
                int newY = e.getYOnScreen() - dragOffset.y;
                frame.setLocation(newX, newY);
            }
        };


        addMouseListener(dragListener);
        addMouseMotionListener(dragListener);


        addMouseListener(dragListener);
        addMouseMotionListener(dragListener);
    }

    /**
     * Hide/show the title bar controls when pass-through mode changes.
     * When pass-through is enabled, both the close and gear icons are hidden.
     */
    public void setPassThrough(boolean passThrough) {
        closeButton.setVisible(!passThrough);
        settingsButton.setVisible(!passThrough);
        revalidate();
        repaint();
    }

    /**
     * Simple custom close button: red box with a white X.
     * Drawn with vector shapes.
     */
    private static class CloseButton extends JPanel {

        private boolean hover = false;

        CloseButton() {
            setOpaque(false);
            setPreferredSize(new Dimension(24, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setHover(boolean hover) {
            this.hover = hover;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color base = new Color(150, 20, 20, 230);
                Color hoverColor = new Color(200, 40, 40, 230);
                g2.setColor(hover ? hoverColor : base);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);

                g2.setColor(new Color(255, 255, 255, 180));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.setColor(Color.WHITE);
                int pad = 6;
                g2.drawLine(pad, pad, w - pad - 1, h - pad - 1);
                g2.drawLine(w - pad - 1, pad, pad, h - pad - 1);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Custom gear icon button (vector-drawn, SVG-style).
     * Clicking opens the Preferences dialog.
     */
    private static class SettingsButton extends JPanel {

        private boolean hover = false;

        SettingsButton() {
            setOpaque(false);
            setPreferredSize(new Dimension(24, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setHover(boolean hover) {
            this.hover = hover;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color base = new Color(80, 80, 80, 230);
                Color hoverColor = new Color(110, 110, 140, 230);
                g2.setColor(hover ? hoverColor : base);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);

                g2.setColor(new Color(220, 220, 255, 180));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                // Draw gear body
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(1.7f));

                int cx = w / 2;
                int cy = h / 2;
                int size = Math.min(w, h) - 8; // leave padding inside the button
                int rOuter = size / 2;
                int rInner = rOuter - 3;

                // Translate to center for simpler math
                g2.translate(cx, cy);

                // Outer gear circle
                g2.drawOval(-rOuter, -rOuter, 2 * rOuter, 2 * rOuter);

                // Inner hub
                g2.fillOval(-rInner, -rInner, 2 * rInner, 2 * rInner);

                // Teeth: 8 small rectangles around the rim
                int teeth = 8;
                double angleStep = 2.0 * Math.PI / teeth;
                int toothWidth = 4;
                int toothHeight = 3;

                for (int i = 0; i < teeth; i++) {
                    double angle = i * angleStep;
                    AffineTransform old = g2.getTransform();
                    g2.rotate(angle);

                    // Draw one tooth centered horizontally above the outer circle
                    int yTop = -rOuter - toothHeight + 1;
                    g2.fillRoundRect(-toothWidth / 2, yTop, toothWidth, toothHeight, 2, 2);

                    g2.setTransform(old);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
