package org.dce.ed;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class BiologyTabPanel extends JPanel {

    public BiologyTabPanel() {
        super(new BorderLayout());

        setOpaque(false);

        JLabel label = new JLabel("Biology / Exobiology information will go here.");
        add(label, BorderLayout.NORTH);
    }
    public void applyUiFontPreferences() {
        applyUiFont(OverlayPreferences.getUiFont());
    }

    public void applyUiFont(java.awt.Font font) {
        // Biology requirements TBD; keep hook so it can use the same UI font preferences.
        if (font != null) {
            setFont(font);
        }
        revalidate();
        repaint();
    }
}
