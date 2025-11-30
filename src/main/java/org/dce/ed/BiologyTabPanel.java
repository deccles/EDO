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
}
