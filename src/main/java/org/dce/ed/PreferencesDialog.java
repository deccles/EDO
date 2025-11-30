package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Simple Preferences dialog for the overlay.
 * Currently just a stub with a few example fields.
 * Real settings can be hooked into your prefs system later.
 */
public class PreferencesDialog extends JDialog {

    public PreferencesDialog(OverlayFrame owner) {
        super(owner, "Overlay Preferences", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(420, 320));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", createGeneralPanel());
        tabs.addTab("Overlay", createOverlayPanel());
        tabs.addTab("Logging", createLoggingPanel());

        add(tabs, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new java.awt.GridBagLayout());

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);

        JLabel startWithWindowsLabel = new JLabel("Start overlay with Windows (stub):");
        JCheckBox startWithWindows = new JCheckBox();
        startWithWindows.setOpaque(false);

        content.add(startWithWindowsLabel, gbc);
        gbc.gridx = 1;
        content.add(startWithWindows, gbc);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createOverlayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new java.awt.GridBagLayout());

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);

        JLabel opacityLabel = new JLabel("Overlay opacity (0.0 - 1.0):");
        JTextField opacityField = new JTextField("0.8", 6);

        content.add(opacityLabel, gbc);
        gbc.gridx = 1;
        content.add(opacityField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel mouseHint = new JLabel("Mouse behavior options will go here.");
        content.add(mouseHint, gbc);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createLoggingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new java.awt.GridBagLayout());

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);

        JLabel journalLabel = new JLabel("Use auto-detected ED log folder:");
        JCheckBox autoDetect = new JCheckBox();
        autoDetect.setSelected(true);
        autoDetect.setOpaque(false);

        content.add(journalLabel, gbc);
        gbc.gridx = 1;
        content.add(autoDetect, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel pathLabel = new JLabel("(Custom path fields / browse button can go here.)");
        content.add(pathLabel, gbc);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setOpaque(false);

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> {
            // TODO: persist settings
            dispose();
        });

        cancel.addActionListener(e -> dispose());

        panel.add(cancel);
        panel.add(ok);
        return panel;
    }
}
