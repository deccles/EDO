package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
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

/**
 * Preferences dialog for the overlay.
 */
public class PreferencesDialog extends JDialog {

	public final String clientKey;
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

    // Fonts-tab fields
    private JComboBox<String> uiFontNameCombo;
    private JSpinner uiFontSizeSpinner;

    // Mining-tab fields
    private JTextField prospectorMaterialsField;
    private JSpinner prospectorMinPropSpinner;
    private JSpinner prospectorMinAvgValueSpinner;

    // Mining tab: value estimation (used by Mining tab only)
    private JSpinner miningTonsLowSpinner;
    private JSpinner miningTonsMediumSpinner;
    private JSpinner miningTonsHighSpinner;
    private JSpinner miningTonsCoreSpinner;

    private boolean okPressed;
    private final Font originalUiFont;
    private final boolean originalOverlayTransparent;

    public static final String[] STANDARD_US_ENGLISH_VOICES = new String[] {
            "Joanna",
            "Matthew",
            "Ivy",
            "Justin",
            "Kendra",
            "Kimberly",
            "Joey",
            "Salli"
    };
    
    public PreferencesDialog(OverlayFrame owner, String clientKey) {
        super(owner, "Overlay Preferences", true);
        this.clientKey = clientKey;
        this.originalUiFont = OverlayPreferences.getUiFont();
        this.originalOverlayTransparent = OverlayPreferences.isOverlayTransparent();
        this.okPressed = false;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(560, 380));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overlay", createOverlayPanel());
        tabs.addTab("Logging", createLoggingPanel());
        tabs.addTab("Speech", createSpeechPanel());
        tabs.addTab("Fonts", createFontsPanel());
        tabs.addTab("Mining", createMiningPanel());

        add(tabs, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);

        // If the user closes the dialog or hits Cancel, revert any live preview.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                revertLivePreviewIfNeeded();
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                revertLivePreviewIfNeeded();
            }
        });
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
        JLabel transparencyLabel = new JLabel("Make overlay transparent:");
        overlayTransparentCheckBox = new JCheckBox();
        overlayTransparentCheckBox.setOpaque(false);

        // Load current transparency preference
        overlayTransparentCheckBox.setSelected(originalOverlayTransparent);

        overlayTransparentCheckBox.addActionListener(e -> applyLiveOverlayTransparencyPreview());

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
        boolean auto = OverlayPreferences.isAutoLogDir(clientKey);
        autoDetectCheckBox.setSelected(auto);

        content.add(journalLabel, gbc);
        gbc.gridx = 1;
        content.add(autoDetectCheckBox, gbc);

        // --- Custom path field + browse button ---
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel pathLabel = new JLabel("Custom journal folder:");
        content.add(pathLabel, gbc);

        gbc.gridx = 1;
        JPanel pathPanel = new JPanel(new BorderLayout(4, 0));
        pathPanel.setOpaque(false);

        customPathField = new JTextField(28);
        customPathField.setText(OverlayPreferences.getCustomLogDir(clientKey));

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

    
    private JPanel createFontsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setOpaque(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Font family
        JLabel fontLabel = new JLabel("Font:");
        content.add(fontLabel, gbc);

        gbc.gridx = 1;
        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        uiFontNameCombo = new JComboBox<>(families);
        uiFontNameCombo.setSelectedItem(OverlayPreferences.getUiFontName());
        uiFontNameCombo.setPrototypeDisplayValue("Segoe UI Semibold");
        content.add(uiFontNameCombo, gbc);

        // Font size
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel sizeLabel = new JLabel("Size:");
        content.add(sizeLabel, gbc);

        gbc.gridx = 1;
        int sz = OverlayPreferences.getUiFontSize();
        uiFontSizeSpinner = new JSpinner(new SpinnerNumberModel(sz, 8, 72, 1));
        ((JSpinner.DefaultEditor) uiFontSizeSpinner.getEditor()).getTextField().setColumns(4);
        content.add(uiFontSizeSpinner, gbc);

        // Preview
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;

        uiFontNameCombo.addActionListener(e -> updatePreviewLabelFont());
        uiFontSizeSpinner.addChangeListener(e -> updatePreviewLabelFont());

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createMiningPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setOpaque(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 6, 6, 6);

        JLabel materialsLabel = new JLabel("Prospector materials (comma separated):");
        content.add(materialsLabel, gbc);

        gbc.gridx = 1;
        prospectorMaterialsField = new JTextField(32);
        prospectorMaterialsField.setText(OverlayPreferences.getProspectorMaterialsCsv());
        content.add(prospectorMaterialsField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel minPropLabel = new JLabel("Minimum proportion (%):");
        content.add(minPropLabel, gbc);

        gbc.gridx = 1;
        double current = OverlayPreferences.getProspectorMinProportionPercent();
        prospectorMinPropSpinner = new JSpinner(new SpinnerNumberModel(current, 0.0, 100.0, 1.0));
        ((JSpinner.DefaultEditor) prospectorMinPropSpinner.getEditor()).getTextField().setColumns(6);
        content.add(prospectorMinPropSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel minAvgValueLabel = new JLabel("Minimum galactic average value (Cr/t):");
        content.add(minAvgValueLabel, gbc);

        gbc.gridx = 1;
        int currentAvg = OverlayPreferences.getProspectorMinAvgValueCrPerTon();
        prospectorMinAvgValueSpinner = new JSpinner(new SpinnerNumberModel(currentAvg, 0, 10_000_000, 1000));
        ((JSpinner.DefaultEditor) prospectorMinAvgValueSpinner.getEditor()).getTextField().setColumns(8);
        content.add(prospectorMinAvgValueSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel hint = new JLabel("Tip: leave materials blank to announce ANY material above the thresholds.");
        content.add(hint, gbc);

        // --- Estimation settings used by the Mining tab ---
        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel estHeader = new JLabel("Mining tab value estimation (tons):");
        content.add(estHeader, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lowTonsLabel = new JLabel("Content=Low total tons:");
        content.add(lowTonsLabel, gbc);

        gbc.gridx = 1;
        miningTonsLowSpinner = new JSpinner(new SpinnerNumberModel(OverlayPreferences.getMiningEstimateTonsLow(), 0.0, 200.0, 1.0));
        ((JSpinner.DefaultEditor) miningTonsLowSpinner.getEditor()).getTextField().setColumns(6);
        content.add(miningTonsLowSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel medTonsLabel = new JLabel("Content=Medium total tons:");
        content.add(medTonsLabel, gbc);

        gbc.gridx = 1;
        miningTonsMediumSpinner = new JSpinner(new SpinnerNumberModel(OverlayPreferences.getMiningEstimateTonsMedium(), 0.0, 200.0, 1.0));
        ((JSpinner.DefaultEditor) miningTonsMediumSpinner.getEditor()).getTextField().setColumns(6);
        content.add(miningTonsMediumSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel highTonsLabel = new JLabel("Content=High total tons:");
        content.add(highTonsLabel, gbc);

        gbc.gridx = 1;
        miningTonsHighSpinner = new JSpinner(new SpinnerNumberModel(OverlayPreferences.getMiningEstimateTonsHigh(), 0.0, 200.0, 1.0));
        ((JSpinner.DefaultEditor) miningTonsHighSpinner.getEditor()).getTextField().setColumns(6);
        content.add(miningTonsHighSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel coreTonsLabel = new JLabel("Core total tons:");
        content.add(coreTonsLabel, gbc);

        gbc.gridx = 1;
        miningTonsCoreSpinner = new JSpinner(new SpinnerNumberModel(OverlayPreferences.getMiningEstimateTonsCore(), 0.0, 200.0, 1.0));
        ((JSpinner.DefaultEditor) miningTonsCoreSpinner.getEditor()).getTextField().setColumns(6);
        content.add(miningTonsCoreSpinner, gbc);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private void updatePreviewLabelFont() {
        Font f = buildSelectedUiFont();
        applyLivePreview(f);
    }

    private Font buildSelectedUiFont() {
        String name = (String) uiFontNameCombo.getSelectedItem();
        int size = 17;
        try {
            size = ((Number) uiFontSizeSpinner.getValue()).intValue();
        } catch (Exception e) {
            // ignore
        }
        if (name == null || name.isBlank()) {
            name = originalUiFont.getName();
        }
        return new Font(name, Font.PLAIN, size);
    }

    private void applyLivePreview(Font font) {
        if (getOwner() instanceof OverlayFrame) {
            ((OverlayFrame) getOwner()).applyUiFontPreview(font);
        }
    }

    private void applyLivePreviewToOverlay() {
        if (!(getOwner() instanceof OverlayFrame)) {
            return;
        }

        String name = (String) uiFontNameCombo.getSelectedItem();
        int size = 17;
        try {
            size = ((Number) uiFontSizeSpinner.getValue()).intValue();
        } catch (Exception e) {
            // ignore
        }

        Font font = new Font(name, Font.PLAIN, size);
        ((OverlayFrame) getOwner()).applyUiFontPreview(font);
    }

    private void revertLivePreviewIfNeeded() {
        if (okPressed) {
            return;
        }
        if (!(getOwner() instanceof OverlayFrame)) {
            return;
        }
        OverlayFrame f = (OverlayFrame) getOwner();
        f.applyUiFontPreview(originalUiFont);
        f.applyOverlayTransparency(originalOverlayTransparent);
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
        speechVoiceCombo = new JComboBox<>(STANDARD_US_ENGLISH_VOICES);
        speechVoiceCombo.setSelectedItem(OverlayPreferences.getSpeechVoiceName());
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
            okPressed = true;
            applyAndSavePreferences();
            if (getOwner() instanceof OverlayFrame) {
                OverlayFrame f = (OverlayFrame) getOwner();
                f.applyOverlayTransparency(overlayTransparentCheckBox != null && overlayTransparentCheckBox.isSelected());
                f.applyUiFontPreferences();
            }
            dispose();
        });

        cancel.addActionListener(e -> {
            revertLivePreviewIfNeeded();
            dispose();
        });

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
            OverlayPreferences.setAutoLogDir(clientKey, auto);
            if (!auto) {
                OverlayPreferences.setCustomLogDir(clientKey, customPathField.getText().trim());
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

                // Fonts
        if (uiFontNameCombo != null) {
            Object sel = uiFontNameCombo.getSelectedItem();
            if (sel != null) {
                OverlayPreferences.setUiFontName(sel.toString());
            }
        }
        if (uiFontSizeSpinner != null) {
            try {
                int sz = ((Number) uiFontSizeSpinner.getValue()).intValue();
                OverlayPreferences.setUiFontSize(sz);
            } catch (Exception e) {
                // ignore
            }
        }

        // Mining
        if (prospectorMaterialsField != null) {
            OverlayPreferences.setProspectorMaterialsCsv(prospectorMaterialsField.getText());
        }
        if (prospectorMinPropSpinner != null) {
            try {
                double p = ((Number) prospectorMinPropSpinner.getValue()).doubleValue();
                OverlayPreferences.setProspectorMinProportionPercent(p);
            } catch (Exception e) {
                // ignore
            }
        }

        if (prospectorMinAvgValueSpinner != null) {
            try {
                int v = ((Number) prospectorMinAvgValueSpinner.getValue()).intValue();
                OverlayPreferences.setProspectorMinAvgValueCrPerTon(v);
            } catch (Exception e) {
                // ignore
            }
        }

        if (miningTonsLowSpinner != null) {
            try {
                double v = ((Number) miningTonsLowSpinner.getValue()).doubleValue();
                OverlayPreferences.setMiningEstimateTonsLow(v);
            } catch (Exception e) {
                // ignore
            }
        }
        if (miningTonsMediumSpinner != null) {
            try {
                double v = ((Number) miningTonsMediumSpinner.getValue()).doubleValue();
                OverlayPreferences.setMiningEstimateTonsMedium(v);
            } catch (Exception e) {
                // ignore
            }
        }
        if (miningTonsHighSpinner != null) {
            try {
                double v = ((Number) miningTonsHighSpinner.getValue()).doubleValue();
                OverlayPreferences.setMiningEstimateTonsHigh(v);
            } catch (Exception e) {
                // ignore
            }
        }
        if (miningTonsCoreSpinner != null) {
            try {
                double v = ((Number) miningTonsCoreSpinner.getValue()).doubleValue();
                OverlayPreferences.setMiningEstimateTonsCore(v);
            } catch (Exception e) {
                // ignore
            }
        }

// Other tabs can be wired into OverlayPreferences later as needed.
    }
    private void applyLiveOverlayTransparencyPreview() {
        if (!(getOwner() instanceof OverlayFrame)) {
            return;
        }
        boolean transparent = overlayTransparentCheckBox != null && overlayTransparentCheckBox.isSelected();
        ((OverlayFrame) getOwner()).applyOverlayTransparency(transparent);
    }

}