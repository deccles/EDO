package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Preferences dialog for the overlay.
 */
public class PreferencesDialog extends JDialog {

    // Logging-tab fields so OK can read them
    private JCheckBox autoDetectCheckBox;
    private JTextField customPathField;

    public PreferencesDialog(OverlayFrame owner) {
        super(owner, "Overlay Preferences", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(460, 340));

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
        panel.setOpaque(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

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
        panel.setOpaque(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

        JLabel dummyLabel = new JLabel("Overlay options (stub, to be expanded later).");
        content.add(dummyLabel, gbc);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    /**
     * Logging tab: choose between auto-detected live folder and a custom test folder.
     */
    private JPanel createLoggingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setOpaque(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

        // --- Auto-detect checkbox ---
        JLabel journalLabel = new JLabel("Use auto-detected ED log folder:");
        autoDetectCheckBox = new JCheckBox();
        autoDetectCheckBox.setOpaque(false);

        // Load current prefs
        boolean auto = OverlayPreferences.isAutoLogDir();
        autoDetectCheckBox.setSelected(auto);

        content.add(journalLabel, gbc);
        gbc.gridx = 1;
        content.add(autoDetectCheckBox, gbc);

        // --- Custom path field + browse button ---
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel pathLabel = new JLabel("Custom journal folder (for testing):");
        content.add(pathLabel, gbc);

        gbc.gridx = 1;
        JPanel pathPanel = new JPanel(new BorderLayout(4, 0));
        pathPanel.setOpaque(false);

        customPathField = new JTextField(24);
        customPathField.setText(OverlayPreferences.getCustomLogDir());

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Elite Dangerous journal folder");
            String existing = customPathField.getText().trim();
            if (!existing.isEmpty()) {
                File f = new File(existing);
                if (f.isDirectory()) {
                    chooser.setCurrentDirectory(f);
                }
            }
            int result = chooser.showOpenDialog(PreferencesDialog.this);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                customPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        pathPanel.add(customPathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        content.add(pathPanel, gbc);

        // Enable/disable fields based on auto-detect state
        Runnable updateEnabled = () -> {
            boolean useAuto = autoDetectCheckBox.isSelected();
            customPathField.setEnabled(!useAuto);
            browseButton.setEnabled(!useAuto);
        };
        autoDetectCheckBox.addActionListener(e -> updateEnabled.run());
        updateEnabled.run();

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
            applyAndSavePreferences();
            dispose();
        });

        cancel.addActionListener(e -> dispose());

        panel.add(cancel);
        panel.add(ok);
        return panel;
    }

    private void applyAndSavePreferences() {
        if (autoDetectCheckBox != null && customPathField != null) {
            boolean auto = autoDetectCheckBox.isSelected();
            OverlayPreferences.setAutoLogDir(auto);
            if (!auto) {
                OverlayPreferences.setCustomLogDir(customPathField.getText().trim());
            }
        }
        // Other tabs can be wired into OverlayPreferences later as needed.
    }
}
