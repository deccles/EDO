package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class OverlayContentPanel extends JPanel {

    private final OverlayFrame overlayFrame;
    private final EliteOverlayTabbedPane tabbedPane;

    public OverlayContentPanel(OverlayFrame overlayFrame) {
        this.overlayFrame = overlayFrame;

        java.awt.Color bg = OverlayPreferences.buildOverlayBackgroundColor(
                OverlayPreferences.getOverlayBackgroundColor(),
                OverlayPreferences.getOverlayTransparencyPercent()
        );
        setOpaque(bg.getAlpha() >= 255);
        setBackground(bg);
        setLayout(new BorderLayout());

        tabbedPane = new EliteOverlayTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
    }

    public EliteOverlayTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    public void applyOverlayBackground(java.awt.Color bg) {
        if (bg == null) {
            bg = java.awt.Color.black;
        }

        setOpaque(bg.getAlpha() >= 255);
        setBackground(bg);

        tabbedPane.applyOverlayBackground(bg);

        revalidate();
        repaint();
    }

    public void applyOverlayTransparency(boolean transparent) {
        // Legacy wrapper
        java.awt.Color bg = OverlayPreferences.buildOverlayBackgroundColor(
                OverlayPreferences.getOverlayBackgroundColor(),
                transparent ? 100 : OverlayPreferences.getOverlayTransparencyPercent()
        );
        applyOverlayBackground(bg);
    }

public void applyUiFontPreferences() {
        tabbedPane.applyUiFontPreferences();
        revalidate();
        repaint();
    }

    public void applyUiFont(java.awt.Font font) {
        tabbedPane.applyUiFont(font);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 1000);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Because isOpaque() is false, super.paintComponent(...)
        // will NOT fill the background, so we can safely call it
        // (and it keeps child painting behavior correct).
        super.paintComponent(g);

        // If you ever want a VERY light tint, you could draw a
        // semi-transparent rectangle here. For true transparency,
        // leave this empty.
    }
}
