package org.dce.ed.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.dce.ed.util.AppIconUtil;

/**
 * Brief full-window splash: large centered app icon over a light dim, fading out (glass pane).
 */
public final class StartupSplashOverlay {

    /** Fade duration (ms). */
    private static final int FADE_MS = 2200;
    private static final int TICK_MS = 40;

    private StartupSplashOverlay() {
    }

    /**
     * Installs a fading splash on {@code frame} if the app icon resource loads. Safe to call from any thread;
     * installation runs on the EDT once the frame is showing.
     */
    public static void install(JFrame frame) {
        if (frame == null) {
            return;
        }
        BufferedImage img = AppIconUtil.loadAppIconForSplash();
        if (img == null) {
            return;
        }
        // Defer one frame so the root pane has real bounds before painting the glass pane.
        SwingUtilities.invokeLater(() -> {
            if (!frame.isShowing()) {
                return;
            }
            JRootPane root = frame.getRootPane();
            SplashPanel panel = new SplashPanel(root, img);
            root.setGlassPane(panel);
            panel.setVisible(true);
            panel.startFade();
        });
    }

    private static final class SplashPanel extends JPanel {

        private final JRootPane root;
        private final BufferedImage image;
        private final Timer timer;
        private final int totalTicks;
        private int tick;
        private volatile boolean dismissed;

        SplashPanel(JRootPane root, BufferedImage image) {
            this.root = root;
            this.image = image;
            setOpaque(false);
            setLayout(null);
            // Block clicks to underlying UI until the splash is gone.
            MouseAdapter block = new MouseAdapter() {
            };
            addMouseListener(block);
            addMouseMotionListener(block);

            totalTicks = Math.max(1, FADE_MS / TICK_MS);
            timer = new Timer(TICK_MS, e -> onTick());
        }

        void startFade() {
            timer.setRepeats(true);
            timer.start();
        }

        private void onTick() {
            tick++;
            if (tick >= totalTicks) {
                timer.stop();
                dismiss();
                return;
            }
            repaint();
        }

        private void dismiss() {
            if (dismissed) {
                return;
            }
            dismissed = true;
            timer.stop();
            Runnable clear = () -> {
                JPanel empty = new JPanel();
                empty.setOpaque(false);
                root.setGlassPane(empty);
                empty.setVisible(false);
            };
            if (SwingUtilities.isEventDispatchThread()) {
                clear.run();
            } else {
                SwingUtilities.invokeLater(clear);
            }
        }

        /** Opacity 1 → 0 while the splash is visible ({@code tick} advances each timer tick). */
        private float splashOpacity() {
            if (tick >= totalTicks) {
                return 0f;
            }
            float t = tick / (float) totalTicks;
            // Ease-out: icon stays bold briefly, then fades more quickly at the end.
            float easedT = 1f - (1f - t) * (1f - t);
            return Math.max(0f, 1f - easedT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            float a = splashOpacity();
            if (a <= 0.001f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.4f));
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                int iw = image.getWidth();
                int ih = image.getHeight();
                int maxSide = (int) Math.round(Math.min(w, h) * 0.58);
                double scale = Math.min((double) maxSide / iw, (double) maxSide / ih);
                int tw = Math.max(1, (int) Math.round(iw * scale));
                int th = Math.max(1, (int) Math.round(ih * scale));
                int x = (w - tw) / 2;
                int y = (h - th) / 2;
                g2.drawImage(image, x, y, tw, th, null);
            } finally {
                g2.dispose();
            }
        }
    }
}
