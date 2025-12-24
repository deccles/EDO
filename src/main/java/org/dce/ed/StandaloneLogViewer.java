package org.dce.ed;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.LiveJournalMonitor;

/**
 * Standalone runner for {@link LogTabPanel}.
 *
 * Drop this class into the same project as LogTabPanel + logreader classes and run the main().
 */
public class StandaloneLogViewer {

    public static String clientKey = "LOG";

	public static void main(String[] args) {
		OverlayPreferences.setAutoLogDir(clientKey, false);
		OverlayPreferences.setCustomLogDir(clientKey, "C:/Users/17036/git-EliteDangerousOverlay/EDO/Elite Dangerous");
		
        EventQueue.invokeLater(() -> {
            LogTabPanel panel = new LogTabPanel();

            JFrame frame = new JFrame("Elite Dangerous - Journal Log Viewer");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setPreferredSize(new Dimension(1100, 800));
            frame.pack();
            frame.setLocationRelativeTo(null);

            // Live tail of the current journal file; feed into the same handler LogTabPanel uses.
            LiveJournalMonitor monitor = LiveJournalMonitor.getInstance(clientKey);
            monitor.addListener((EliteLogEvent e) -> {
                // Ensure all UI updates occur on the EDT
                SwingUtilities.invokeLater(() -> panel.handleLogEvent(e));
            });

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    monitor.shutdown();
                }
            });

            frame.setVisible(true);
        });
    }
}
