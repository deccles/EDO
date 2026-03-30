package org.dce.ed.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Themed {@link JSplitPane} UI for the Mining tab: divider uses the main accent color (semi-opaque) instead
 * of the LAF default.
 * <p>
 * {@link java.awt.Container#update} clears the divider with {@code clearRect} before {@code paint} for some
 * peers; over a transparent overlay that reads as twin hairlines or a hollow band. We override {@code update}
 * to match {@link javax.swing.JComponent}: only {@code paint}, no clear.
 */
public final class EdoMiningSplitPaneUi {

    /** Main-text alpha for the draggable divider bar (0–255). Lower = more transparent. */
    private static final int DIVIDER_MAIN_TEXT_ALPHA = 110;

    private EdoMiningSplitPaneUi() {
    }

    public static void install(JSplitPane split) {
        if (split == null) {
            return;
        }
        split.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                ThemedDivider d = new ThemedDivider(this);
                d.setBorder(BorderFactory.createEmptyBorder());
                applyDividerTheme(d);
                return d;
            }
        });
    }

    /**
     * Updates divider background when {@link EdoUi.User#MAIN_TEXT} / derived colors change.
     */
    public static void applyDividerTheme(JSplitPane split) {
        if (split == null) {
            return;
        }
        if (split.getUI() instanceof BasicSplitPaneUI) {
            BasicSplitPaneDivider d = ((BasicSplitPaneUI) split.getUI()).getDivider();
            if (d != null) {
                applyDividerTheme(d);
            }
        }
    }

    static void applyDividerTheme(BasicSplitPaneDivider divider) {
        if (divider == null) {
            return;
        }
        divider.setBackground(EdoUi.Internal.mainTextAlpha(DIVIDER_MAIN_TEXT_ALPHA));
        divider.setBorder(BorderFactory.createEmptyBorder());
        divider.repaint();
    }

    private static final class ThemedDivider extends BasicSplitPaneDivider {

        private static final long serialVersionUID = 1L;

        ThemedDivider(BasicSplitPaneUI ui) {
            super(ui);
        }

        @Override
        public void update(Graphics g) {
            paint(g);
        }

        @Override
        public void paint(Graphics g) {
            Dimension size = getSize();
            if (size.width <= 0 || size.height <= 0) {
                return;
            }
            Color bg = getBackground();
            if (bg == null) {
                bg = EdoUi.Internal.mainTextAlpha(DIVIDER_MAIN_TEXT_ALPHA);
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(bg);
                g2.fillRect(0, 0, size.width, size.height);
            } finally {
                g2.dispose();
            }
            // Do not call BasicSplitPaneDivider.paint / Container.paint: that forwards to lightweight
            // children (one-touch arrows, etc.) which draw extra glyphs on top of the bar.
            Border border = getBorder();
            if (border != null) {
                border.paintBorder(this, g, 0, 0, size.width, size.height);
            }
        }
    }
}
