package org.dce.ed.ui;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Table header that paints only the header renderer (no LAF background),
 * so pass-through transparency shows the backing color.
 */
public final class TransparentTableHeader extends JTableHeader {

    private static final long serialVersionUID = 1L;

    public TransparentTableHeader(TableColumnModel cm) {
        super(cm);
        setOpaque(false);
        setBackground(EdoUi.Internal.TRANSPARENT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        TableColumnModel cm = getColumnModel();
        int n = cm.getColumnCount();
        if (n <= 0) return;
        javax.swing.JTable tbl = getTable();
        TableCellRenderer renderer = getDefaultRenderer();
        if (renderer == null || tbl == null) return;
        boolean ltr = getComponentOrientation().isLeftToRight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        for (int i = 0; i < n; i++) {
            int col = ltr ? i : (n - 1 - i);
            TableColumn tc = cm.getColumn(col);
            TableCellRenderer colRenderer = tc.getHeaderRenderer();
            if (colRenderer == null) colRenderer = renderer;
            java.awt.Rectangle r = getHeaderRect(col);
            // Clear cell to transparent so backing shows through (no LAF fill)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setComposite(AlphaComposite.SrcOver);
            java.awt.Component cell = colRenderer.getTableCellRendererComponent(tbl, tc.getHeaderValue(), false, false, -1, col);
            cell.setBounds(0, 0, r.width, r.height);
            Graphics2D cellG = (Graphics2D) g2.create(r.x, r.y, r.width, r.height);
            cell.paint(cellG);
            cellG.dispose();
        }
        g2.setColor(EdoUi.ED_ORANGE_TRANS);
        int y = getHeight() - 1;
        g2.drawLine(0, y, getWidth(), y);
        g2.dispose();
    }
}
