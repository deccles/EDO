package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

/**
 * Custom transparent "tabbed pane" for the overlay.
 * Does not extend JTabbedPane to avoid opaque background painting.
 *
 * Tabs: Route, System, Biology.
 */
public class EliteOverlayTabbedPane extends JPanel {

    private static final String CARD_ROUTE = "ROUTE";
    private static final String CARD_SYSTEM = "SYSTEM";
    private static final String CARD_BIOLOGY = "BIOLOGY";
    private static final String CARD_LOG = "LOG";

    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    public EliteOverlayTabbedPane() {
        super(new BorderLayout());
        setOpaque(false);

        // ----- Tab bar (row of buttons) -----
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        tabBar.setOpaque(false);

        ButtonGroup group = new ButtonGroup();

        JButton routeButton = createTabButton("Route");
        JButton systemButton = createTabButton("System");
        JButton biologyButton = createTabButton("Biology");
        JButton logButton = createTabButton("Log");
        
        group.add(routeButton);
        group.add(systemButton);
        group.add(biologyButton);
        group.add(logButton);
        
        tabBar.add(routeButton);
        tabBar.add(systemButton);
        tabBar.add(biologyButton);
        tabBar.add(logButton);
        
        // ----- Card area with the actual tab contents -----
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(400, 1000));

        RouteTabPanel routeTab = new RouteTabPanel();
        SystemTabPanel systemTab = new SystemTabPanel();
        BiologyTabPanel biologyTab = new BiologyTabPanel();
        LogTabPanel logTab = new LogTabPanel();
        
        cardPanel.add(routeTab, CARD_ROUTE);
        cardPanel.add(systemTab, CARD_SYSTEM);
        cardPanel.add(biologyTab, CARD_BIOLOGY);
        cardPanel.add(logTab, CARD_LOG);

        // Wire up buttons to show cards
        routeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_ROUTE);
            }
        });
        systemButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_SYSTEM);
            }
        });
        biologyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_BIOLOGY);
            }
        });

        logButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(cardPanel, CARD_LOG);
            }
        });
        
        // Select Route tab by default
        routeButton.doClick();

        add(tabBar, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
    }

    private JButton createTabButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        // Slightly translucent dark background so tabs are legible but not huge blocks
        button.setOpaque(true);
        button.setBackground(new Color(50, 50, 50, 220));
        button.setForeground(Color.WHITE);
        return button;
    }
}
