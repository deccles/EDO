package org.dce.ed;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class RouteTabPanel extends JPanel {

    public RouteTabPanel() {
        super(new BorderLayout());

        // Keep this panel transparent
        setOpaque(false);

        JLabel label = new JLabel("Route information will go here.");
        add(label, BorderLayout.NORTH);
    }
}
