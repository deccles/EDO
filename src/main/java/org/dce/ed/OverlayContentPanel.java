package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class OverlayContentPanel extends JPanel {

    private final OverlayFrame overlayFrame;

    public OverlayContentPanel(OverlayFrame overlayFrame) {
        this.overlayFrame = overlayFrame;

        // IMPORTANT: don't let Swing fill this with a solid background
        setOpaque(false);
        setLayout(new BorderLayout());

        EliteOverlayTabbedPane tabbedPane = new EliteOverlayTabbedPane();
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(new java.awt.Color(0, 0, 0, 0));
        add(tabbedPane, BorderLayout.CENTER);
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
