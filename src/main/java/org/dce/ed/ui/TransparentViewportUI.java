package org.dce.ed.ui;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ViewportUI;
import javax.swing.plaf.basic.BasicViewportUI;

/**
 * ViewportUI that does not paint an opaque background (overrides update to skip the fill),
 * so the column header viewport stays transparent in pass-through overlay mode.
 */
public final class TransparentViewportUI extends BasicViewportUI {

    public static ViewportUI createUI(JComponent c) {
        return new TransparentViewportUI();
    }

    @Override
    public void update(Graphics g, JComponent c) {
        // Skip the default background fill so the viewport stays transparent.
        paint(g, c);
    }
}
