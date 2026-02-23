package org.dce.ed.ui;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.plaf.TableHeaderUI;

/**
 * TableHeaderUI that never paints an opaque background, so the header stays
 * transparent in pass-through overlay mode. Use with setOpaque(false) and
 * setBackground(TRANSPARENT) on the JTableHeader.
 */
public final class TransparentTableHeaderUI extends BasicTableHeaderUI {

    public static TableHeaderUI createUI(JComponent c) {
        return new TransparentTableHeaderUI();
    }

    @Override
    public void update(Graphics g, JComponent c) {
        // Skip the default background fill so the header stays transparent.
        paint(g, c);
    }
}
