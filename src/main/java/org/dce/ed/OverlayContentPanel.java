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

        boolean opaque = !OverlayPreferences.isOverlayTransparent();
        setOpaque(opaque);
        setLayout(new BorderLayout());

        tabbedPane = new EliteOverlayTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
    }

    public EliteOverlayTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    
    public void applyOverlayTransparency(boolean transparent) {
        boolean opaque = !transparent;
        setOpaque(opaque);
        setBackground(transparent ? new java.awt.Color(0, 0, 0, 0) : java.awt.Color.black);

        tabbedPane.applyOverlayTransparency(transparent);

        revalidate();
        repaint();
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
