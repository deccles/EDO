package org.dce.ed.ui;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Poll-based hover-to-copy for a single component, matching {@link SystemTableHoverCopyManager}
 * timing and pass-through gating (hover copy only when the overlay is in mouse pass-through mode).
 */
public final class HoverCopyButtonSupport {

	private static final int POLL_INTERVAL_MS = 100;
	private static final int HOVER_DELAY_MS = 1500;

	private final JComponent target;
	private final Supplier<String> textSupplier;
	private final BooleanSupplier passThroughEnabledSupplier;

	private final Timer pollTimer;
	private final Timer hoverTimer;

	private boolean pointerWasInside;

	public HoverCopyButtonSupport(JComponent target, Supplier<String> textSupplier,
			BooleanSupplier passThroughEnabledSupplier) {
		this.target = target;
		this.textSupplier = textSupplier;
		this.passThroughEnabledSupplier = passThroughEnabledSupplier;
		this.pollTimer = new Timer(POLL_INTERVAL_MS, e -> pollMousePosition());
		this.hoverTimer = new Timer(HOVER_DELAY_MS, e -> copyIfStillHovering());
		this.hoverTimer.setRepeats(false);
	}

	public void start() {
		pollTimer.start();
	}

	public void stop() {
		pollTimer.stop();
		hoverTimer.stop();
		pointerWasInside = false;
	}

	private void pollMousePosition() {
		if (!target.isShowing() || !target.isEnabled()) {
			stopHoverTracking();
			return;
		}
		if (passThroughEnabledSupplier != null && !passThroughEnabledSupplier.getAsBoolean()) {
			stopHoverTracking();
			return;
		}

		java.awt.PointerInfo info = MouseInfo.getPointerInfo();
		if (info == null) {
			stopHoverTracking();
			return;
		}

		Point screen = info.getLocation();
		Point local = new Point(screen);
		SwingUtilities.convertPointFromScreen(local, target);

		boolean inside = local.x >= 0 && local.y >= 0
				&& local.x < target.getWidth()
				&& local.y < target.getHeight();

		if (inside) {
			if (!pointerWasInside) {
				hoverTimer.restart();
			}
			pointerWasInside = true;
		} else {
			stopHoverTracking();
		}
	}

	private void stopHoverTracking() {
		hoverTimer.stop();
		pointerWasInside = false;
	}

	private void copyIfStillHovering() {
		if (!target.isShowing() || !target.isEnabled()) {
			return;
		}
		if (passThroughEnabledSupplier != null && !passThroughEnabledSupplier.getAsBoolean()) {
			return;
		}
		java.awt.PointerInfo info = MouseInfo.getPointerInfo();
		if (info == null) {
			return;
		}
		Point screen = info.getLocation();
		Point local = new Point(screen);
		SwingUtilities.convertPointFromScreen(local, target);
		if (local.x < 0 || local.y < 0 || local.x >= target.getWidth() || local.y >= target.getHeight()) {
			return;
		}
		String text = textSupplier.get();
		if (text == null || text.isBlank()) {
			return;
		}
		String trimmed = text.trim();
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(trimmed), null);
		SystemTableHoverCopyManager.showCopiedToast(target, trimmed);
	}
}
