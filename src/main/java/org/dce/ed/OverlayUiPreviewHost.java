package org.dce.ed;

import java.awt.Font;

/**
 * Small abstraction so PreferencesDialog can live-preview settings whether the overlay
 * is hosted by an undecorated pass-through window or a normal decorated window.
 */
public interface OverlayUiPreviewHost {

    boolean isPassThroughEnabled();

    void applyUiFontPreferences();

    void applyUiFontPreview(Font font);

    void applyOverlayBackgroundFromPreferences(boolean passThroughMode);

    void applyOverlayBackgroundPreview(boolean passThroughMode, int rgb, int transparencyPercent);
}
