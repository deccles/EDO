package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.dce.ed.ui.PollyTtsCached;

/**
 * Preferences dialog for the overlay.
 */
public class PreferencesDialog extends JDialog {

    // Overlay-tab fields so OK can read them
    private JCheckBox overlayTransparentCheckBox;

    // Logging-tab fields so OK can read them
    private JCheckBox autoDetectCheckBox;
    private JTextField customPathField;

    // Speech-tab fields so OK can read them
    private JCheckBox speechEnabledCheckBox;
    private JComboBox<String> speechEngineCombo;
    private JComboBox<String> speechVoiceCombo;
    private JTextField speechRegionField;
    private JTextField speechAwsProfileField;
    private JTextField speechCacheDirField;
    private JTextField speechSampleRateField;

    public PreferencesDialog(OverlayFrame owner) {
        super(owner, "Overlay Preferences", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(560, 380));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", createGeneralPanel());
        tabs.addTab("Overlay", createOverlayPanel());
        tabs.addTab("Logging", createLoggingPanel());
        tabs.addTab("Speech", createSpeechPanel());

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

        // Transparency toggle
        JLabel transparencyLabel = new JLabel("Make overlay mouse-click transparent:");
        overlayTransparentCheckBox = new JCheckBox();
        overlayTransparentCheckBox.setOpaque(false);

        // Load current transparency preference
        boolean transparent = OverlayPreferences.isOverlayTransparent();
        overlayTransparentCheckBox.setSelected(transparent);

        content.add(transparencyLabel, gbc);
        gbc.gridx = 1;
        content.add(overlayTransparentCheckBox, gbc);

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

        customPathField = new JTextField(28);
        customPathField.setText(OverlayPreferences.getCustomLogDir());

        JButton browseButton = new JButton("Browse.");
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

    private JPanel createSpeechPanel() {
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

        // Enabled
        JLabel enabledLabel = new JLabel("Enable speech (Amazon Polly):");
        speechEnabledCheckBox = new JCheckBox();
        speechEnabledCheckBox.setOpaque(false);
        speechEnabledCheckBox.setSelected(OverlayPreferences.isSpeechEnabled());

        content.add(enabledLabel, gbc);
        gbc.gridx = 1;
        content.add(speechEnabledCheckBox, gbc);

        // Engine (Standard only by default)
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel engineLabel = new JLabel("Engine:");
        content.add(engineLabel, gbc);

        gbc.gridx = 1;
        speechEngineCombo = new JComboBox<>(new String[] { "standard", "neural" });
        speechEngineCombo.setSelectedItem(OverlayPreferences.getSpeechEngine());
        content.add(speechEngineCombo, gbc);

        // Voice (keep list small and “safe”)
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel voiceLabel = new JLabel("Voice (Standard):");
        content.add(voiceLabel, gbc);

        gbc.gridx = 1;
        speechVoiceCombo = new JComboBox<>(PollyTtsCached.STANDARD_US_ENGLISH_VOICES);
        speechVoiceCombo.setSelectedItem(OverlayPreferences.getSpeechVoiceId());
        content.add(speechVoiceCombo, gbc);

        // Region
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel regionLabel = new JLabel("AWS Region:");
        content.add(regionLabel, gbc);

        gbc.gridx = 1;
        speechRegionField = new JTextField(12);
        speechRegionField.setText(OverlayPreferences.getSpeechAwsRegion());
        content.add(speechRegionField, gbc);

        // Profile
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel profileLabel = new JLabel("AWS profile (optional):");
        content.add(profileLabel, gbc);

        gbc.gridx = 1;
        speechAwsProfileField = new JTextField(18);
        speechAwsProfileField.setText(OverlayPreferences.getSpeechAwsProfile());
        content.add(speechAwsProfileField, gbc);

        // Cache dir
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel cacheDirLabel = new JLabel("Cache directory:");
        content.add(cacheDirLabel, gbc);

        gbc.gridx = 1;
        JPanel cachePanel = new JPanel(new BorderLayout(4, 0));
        cachePanel.setOpaque(false);

        speechCacheDirField = new JTextField(28);
        speechCacheDirField.setText(OverlayPreferences.getSpeechCacheDir().toString());

        JButton browseCacheButton = new JButton("Browse.");
        browseCacheButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select speech cache folder");
            String existing = speechCacheDirField.getText().trim();
            if (!existing.isEmpty()) {
                File f = new File(existing);
                if (f.isDirectory()) {
                    chooser.setCurrentDirectory(f);
                }
            }
            int result = chooser.showOpenDialog(PreferencesDialog.this);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                speechCacheDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        cachePanel.add(speechCacheDirField, BorderLayout.CENTER);
        cachePanel.add(browseCacheButton, BorderLayout.EAST);
        content.add(cachePanel, gbc);

        // Sample rate (PCM)
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel rateLabel = new JLabel("PCM sample rate (Hz):");
        content.add(rateLabel, gbc);

        gbc.gridx = 1;
        speechSampleRateField = new JTextField(8);
        speechSampleRateField.setText(Integer.toString(OverlayPreferences.getSpeechSampleRateHz()));
        content.add(speechSampleRateField, gbc);

        // Enable/disable everything but the checkbox based on enabled
        Runnable updateEnabled = () -> {
            boolean enabled = speechEnabledCheckBox.isSelected();
            speechEngineCombo.setEnabled(enabled);
            speechVoiceCombo.setEnabled(enabled);
            speechRegionField.setEnabled(enabled);
            speechAwsProfileField.setEnabled(enabled);
            speechCacheDirField.setEnabled(enabled);
            browseCacheButton.setEnabled(enabled);
            speechSampleRateField.setEnabled(enabled);
        };
        speechEnabledCheckBox.addActionListener(e -> updateEnabled.run());
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
        // Overlay tab
        if (overlayTransparentCheckBox != null) {
            OverlayPreferences.setOverlayTransparent(overlayTransparentCheckBox.isSelected());
        }

        // Logging tab
        if (autoDetectCheckBox != null && customPathField != null) {
            boolean auto = autoDetectCheckBox.isSelected();
            OverlayPreferences.setAutoLogDir(auto);
            if (!auto) {
                OverlayPreferences.setCustomLogDir(customPathField.getText().trim());
            }
        }

        // Speech tab
        if (speechEnabledCheckBox != null) {
            OverlayPreferences.setSpeechEnabled(speechEnabledCheckBox.isSelected());
        }

        if (speechEngineCombo != null && speechEngineCombo.getSelectedItem() != null) {
            OverlayPreferences.setSpeechEngine(speechEngineCombo.getSelectedItem().toString());
        }

        if (speechVoiceCombo != null && speechVoiceCombo.getSelectedItem() != null) {
            OverlayPreferences.setSpeechVoiceId(speechVoiceCombo.getSelectedItem().toString());
        }

        if (speechRegionField != null) {
            OverlayPreferences.setSpeechAwsRegion(speechRegionField.getText().trim());
        }

        if (speechAwsProfileField != null) {
            OverlayPreferences.setSpeechAwsProfile(speechAwsProfileField.getText().trim());
        }

        if (speechCacheDirField != null) {
            OverlayPreferences.setSpeechCacheDir(speechCacheDirField.getText().trim());
        }

        if (speechSampleRateField != null) {
            String s = speechSampleRateField.getText().trim();
            try {
                int hz = Integer.parseInt(s);
                OverlayPreferences.setSpeechSampleRateHz(hz);
            } catch (Exception e) {
                // ignore, keep previous/default
            }
        }

        // Other tabs can be wired into OverlayPreferences later as needed.
    }
}
