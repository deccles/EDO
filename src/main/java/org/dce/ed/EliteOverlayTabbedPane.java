package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.dce.ed.OverlayPreferences.MiningLimpetReminderMode;
import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogFileLocator;
import org.dce.ed.logreader.LiveJournalMonitor;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FssDiscoveryScanEvent;
import org.dce.ed.logreader.event.LoadoutEvent;
import org.dce.ed.logreader.event.ProspectedAsteroidEvent;
import org.dce.ed.logreader.event.StartJumpEvent;
import org.dce.ed.logreader.event.StatusEvent;
import org.dce.ed.market.GalacticAveragePrices;
import org.dce.ed.tts.PollyTtsCached;
import org.dce.ed.tts.TtsSprintf;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * Custom transparent "tabbed pane" for the overlay.
 * Does not extend JTabbedPane to avoid opaque background painting.
 *
 * Tabs: Route, System, Biology.
 */
public class EliteOverlayTabbedPane extends JPanel {

	private static final long VALUABLE_MATERIAL_THRESHOLD_CREDITS = 2_000_000L;

	private static final String CARD_ROUTE = "ROUTE";
	private static final String CARD_SYSTEM = "SYSTEM";
	private static final String CARD_BIOLOGY = "BIOLOGY";
	private static final String CARD_MINING = "MINING";
	private static final String CARD_LOG = "LOG";

	private static final int TAB_HOVER_DELAY_MS = 500;

	private static final Color TAB_ORANGE = new Color(255, 140, 0, 220);
	private static final Color TAB_WHITE = new Color(255, 255, 255, 230);

	// Restores the original "bigger" tab look (padding inside the outline)
	private static final Insets TAB_PADDING = new Insets(4, 10, 4, 10);

	private final CardLayout cardLayout;
	private final JPanel cardPanel;
	private final JPanel tabBar;

	private final RouteTabPanel routeTab;
	private final SystemTabPanel systemTab;
	private final BiologyTabPanel biologyTab;
	private final MiningTabPanel miningTab;

	private final TtsSprintf tts = new TtsSprintf(new PollyTtsCached());

	private final GalacticAveragePrices galacticAvgPrices = GalacticAveragePrices.loadDefault();

	private long lastLimpetReminderMs;

	private JButton routeButton;
	private JButton systemButton;
	private JButton biologyButton;
	private JButton miningButton;

	
	public EliteOverlayTabbedPane() {
		super(new BorderLayout());

		boolean opaque = !OverlayPreferences.isOverlayTransparent();

		setOpaque(opaque);

		// ----- Tab bar (row of buttons) -----
		tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		tabBar.setOpaque(opaque);
		tabBar.setBackground(Color.black);
		ButtonGroup group = new ButtonGroup();

		routeButton = createTabButton("Route");
		systemButton = createTabButton("System");
		biologyButton = createTabButton("Biology");
		miningButton = createTabButton("Mining");

		group.add(routeButton);
		group.add(systemButton);
		group.add(biologyButton);
		group.add(miningButton);

		tabBar.add(routeButton);
		tabBar.add(systemButton);
		tabBar.add(biologyButton);
		tabBar.add(miningButton);

		// ----- Card area with the actual tab contents -----
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);
		cardPanel.setOpaque(opaque);
		cardPanel.setBackground(Color.black);
		cardPanel.setPreferredSize(new Dimension(400, 1000));

		// Create tab content panels
		this.routeTab = new RouteTabPanel();
		this.systemTab = new SystemTabPanel();
		this.biologyTab = new BiologyTabPanel();
		this.biologyTab.setSystemTabPanel(systemTab);
		this.miningTab = new MiningTabPanel(galacticAvgPrices);

		cardPanel.add(routeTab, CARD_ROUTE);
		cardPanel.add(systemTab, CARD_SYSTEM);
		cardPanel.add(biologyTab, CARD_BIOLOGY);
		cardPanel.add(miningTab, CARD_MINING);

		systemButton.setSelected(true);
		applyTabButtonStyle(routeButton);
		applyTabButtonStyle(systemButton);
		applyTabButtonStyle(biologyButton);
		applyTabButtonStyle(miningButton);
		systemTab.refreshFromCache();

		// Wire up buttons to show cards
		routeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectTab(CARD_ROUTE, routeButton);
			}
		});

		systemButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectTab(CARD_SYSTEM, systemButton);
			}
		});

		biologyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectTab(CARD_BIOLOGY, biologyButton);
			}
		});

		miningButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectTab(CARD_MINING, miningButton);
			}
		});

		// Hover-to-switch: resting over a tab for a short time activates it
		installHoverSwitch(routeButton, TAB_HOVER_DELAY_MS, () -> routeButton.doClick());
		installHoverSwitch(systemButton, TAB_HOVER_DELAY_MS, () -> systemButton.doClick());
		installHoverSwitch(biologyButton, TAB_HOVER_DELAY_MS, () -> biologyButton.doClick());
		installHoverSwitch(miningButton, TAB_HOVER_DELAY_MS, () -> miningButton.doClick());

		// Select Route tab by default
		systemButton.doClick();

		add(tabBar, BorderLayout.NORTH);

		// Hook live journal monitoring into tabs (existing behavior)
		try {
			LiveJournalMonitor monitor = LiveJournalMonitor.getInstance(EliteDangerousOverlay.clientKey);

			monitor.addListener(event -> {

				this.handleLogEvent(event);

				if (event instanceof ProspectedAsteroidEvent) {
					handleProspectedAsteroid((ProspectedAsteroidEvent) event);
				}

				if (event instanceof StatusEvent) {
					StatusEvent flagEvent = (StatusEvent) event;

					if (flagEvent.isFsdCharging()) {
						showRouteTabFromStatusWatcher();
					}
				}

				systemTab.handleLogEvent(event);
				routeTab.handleLogEvent(event);
				biologyTab.handleLogEvent(event);
			});

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// Start watcher that syncs tabs with in-game Galaxy/System map
		GuiFocusWatcher watcher = new GuiFocusWatcher(this);
		Thread watcherThread = new Thread(watcher, "ED-GuiFocusWatcher");
		watcherThread.setDaemon(true);
		watcherThread.start();

		add(cardPanel, BorderLayout.CENTER);
	}

	public SystemTabPanel getSystemTabPanel() {
		return systemTab;
	}
	static LoadoutEvent loadoutEventx = null;

	public static LoadoutEvent getLatestLoadout() {
		if (loadoutEventx == null) {
			EliteJournalReader r = new EliteJournalReader(EliteDangerousOverlay.clientKey);

			try {
				loadoutEventx = (LoadoutEvent) r.findMostRecentEvent(EliteEventType.LOADOUT, 8);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return loadoutEventx;
	}
	public void handleLogEvent(EliteLogEvent event) {
        if (event instanceof LoadoutEvent e) {
        	loadoutEventx = e;
        }

		if (event instanceof FsdJumpEvent e) {
			if (e.getDocked() == null || e.getDocked()) {
				showSystemTabFromStatusWatcher();
			}
		} else if (event instanceof FssDiscoveryScanEvent) {
			showSystemTabFromStatusWatcher();
		}

		if (event instanceof StartJumpEvent) {
			showRouteTabFromStatusWatcher();
		}

		if (event.getType() == EliteEventType.UNDOCKED) {
			maybeRemindAboutLimpets();
		}
	}

	private void handleProspectedAsteroid(ProspectedAsteroidEvent event) {
		// Update Mining tab UI (always), regardless of whether announcements are enabled.
		try {
			System.out.println("Updating from prospector");
			miningTab.updateFromProspector(event);
		} catch (Exception e) {
			// UI update errors shouldn't break log processing
		}

		if (event == null) {
			return;
		}
		if (!OverlayPreferences.isSpeechEnabled()) {
			return;
		}

		String msg = buildProspectorSummary(event);
		System.out.println("Prospector summary : " + ((msg != null) ? msg : "Nothing valuable"));
		if (msg != null && !msg.isBlank()) {
			tts.speakf(msg);
		}
	}

	private String buildProspectorSummary(ProspectedAsteroidEvent event) {
		if (event == null) {
			return null;
		}

		String motherlodeRaw = event.getMotherlodeMaterial();
		String motherlodeNorm = normalizeMaterialName(motherlodeRaw);
		boolean hasCore = motherlodeRaw != null && !motherlodeRaw.isBlank();

		String content = event.getContent();
		double totalTons = estimateTotalTons(content);

		int valuableCount = 0;
		Set<String> counted = new HashSet<>();
		for (ProspectedAsteroidEvent.MaterialProportion m : event.getMaterials()) {
			if (m == null || m.getName() == null) {
				continue;
			}

			String rawName = m.getName();
			String norm = normalizeMaterialName(rawName);
			if (norm.isBlank()) {
				continue;
			}

			// Don't count the core material in the "+ N valuable materials" part.
			if (!motherlodeNorm.isBlank() && norm.equals(motherlodeNorm)) {
				continue;
			}

			OptionalInt avgOpt = galacticAvgPrices.getAvgSellCrPerTon(rawName);
			if (avgOpt.isEmpty()) {
				continue;
			}

			double tons = (m.getProportion() / 100.0) * totalTons;
			if (tons <= 0.0) {
				continue;
			}

			long estCredits = Math.round(tons * (double) avgOpt.getAsInt());
			if (estCredits < VALUABLE_MATERIAL_THRESHOLD_CREDITS) {
				continue;
			}

			if (counted.add(norm)) {
				valuableCount++;
			}
		}

		if (hasCore) {
			String coreSpoken = toSpokenMaterialName(motherlodeRaw);
			if (valuableCount > 0) {
				return String.format(Locale.US,
						"Prospector detected %s core, plus %d valuable %s.",
						coreSpoken,
						valuableCount,
						valuableCount == 1 ? "material" : "materials");
			}
			return String.format(Locale.US, "Prospector detected %s core.", coreSpoken);
		}

		if (valuableCount > 0) {
			return String.format(Locale.US,
					"Prospector detected %d valuable %s.",
					valuableCount,
					valuableCount == 1 ? "material" : "materials");
		}

		return null;
	}

	private static double estimateTotalTons(String content) {
		if (content == null) {
			return OverlayPreferences.getMiningEstimateTonsMedium();
		}
		String c = content.trim().toLowerCase(Locale.US);
		if (c.equals("high")) {
			return OverlayPreferences.getMiningEstimateTonsHigh();
		}
		if (c.equals("low")) {
			return OverlayPreferences.getMiningEstimateTonsLow();
		}
		return OverlayPreferences.getMiningEstimateTonsMedium();
	}

//	private void announceValuableProspectByAvgValue(ProspectedAsteroidEvent event, int minAvgValueCrPerTon) {
//		Set<String> wanted = parseMaterialList(OverlayPreferences.getProspectorMaterialsCsv());
//
//		LinkedHashMap<String, String> qualifying = new LinkedHashMap<>();
//		for (ProspectedAsteroidEvent.MaterialProportion m : event.getMaterials()) {
//			if (m == null) {
//				continue;
//			}
//			String rawName = m.getName();
//			String norm = normalizeMaterialName(rawName);
//			if (!wanted.isEmpty() && !wanted.contains(norm)) {
//				continue;
//			}
//
//			OptionalInt avg = galacticAvgPrices.getAvgSellCrPerTon(rawName);
//			if (avg.isEmpty()) {
//				continue;
//			}
//			if (avg.getAsInt() < minAvgValueCrPerTon) {
//				continue;
//			}
//
//			qualifying.putIfAbsent(norm, rawName);
//		}
//
//		if (qualifying.isEmpty()) {
//			return;
//		}
//
//		String motherlodeRaw = event.getMotherlodeMaterial();
//		String motherlodeNorm = normalizeMaterialName(motherlodeRaw);
//		boolean motherlodeQualifies = !motherlodeNorm.isBlank() && qualifying.containsKey(motherlodeNorm);
//
//		int count = qualifying.size();
//		boolean isHigh = event.getContent() != null && event.getContent().equalsIgnoreCase("High");
//		String coreWord = isHigh ? "motherlode" : "core";
//
//		String msg;
//		if (count == 1) {
//			String only = toSpokenMaterialName(qualifying.values().iterator().next());
//			if (motherlodeQualifies) {
//				msg = "Detected " + only + " " + coreWord;
//			} else {
//				msg = "Detected " + only;
//			}
//		} else if (count == 2) {
//			if (motherlodeQualifies) {
//				String mother = toSpokenMaterialName(qualifying.get(motherlodeNorm));
//				String other = null;
//				for (Entry<String, String> e : qualifying.entrySet()) {
//					if (!e.getKey().equals(motherlodeNorm)) {
//						other = toSpokenMaterialName(e.getValue());
//						break;
//					}
//				}
//				if (other == null) {
//					other = "material";
//				}
//				msg = "Detected " + mother + " " + coreWord + " and " + other;
//			} else {
//				List<String> names = new ArrayList<>();
//				for (String raw : qualifying.values()) {
//					names.add(toSpokenMaterialName(raw));
//				}
//				msg = "Detected " + names.get(0) + " and " + names.get(1);
//			}
//		} else {
//			if (motherlodeQualifies) {
//				String mother = toSpokenMaterialName(qualifying.get(motherlodeNorm));
//				int otherCount = count - 1;
//				msg = "Detected " + mother + " " + coreWord + " and " + otherCount + " valuable " + (otherCount == 1 ? "material" : "materials");
//			} else {
//				msg = "Detected " + count + " valuable materials";
//			}
//		}
//
//		tts.speakf(msg);
//	}

	private static Set<String> parseMaterialList(String csv) {
		if (csv == null || csv.isBlank()) {
			return Set.of();
		}
		Set<String> out = new HashSet<>();
		Arrays.stream(csv.split(","))
		.map(String::trim)
		.filter(s -> !s.isBlank())
		.map(EliteOverlayTabbedPane::normalizeMaterialName)
		.forEach(out::add);
		return out;
	}

	/**
	 * Normalize material names so user input like "Low Temperature Diamonds" can
	 * match journal material keys like "$LowTemperatureDiamonds_Name;".
	 */
	private static String normalizeMaterialName(String s) {
		if (s == null) {
			return "";
		}

		String t = s.trim();
		if (t.startsWith("$")) {
			t = t.substring(1);
		}
		t = t.replace("_name", "");
		t = t.replace("_Name", "");
		t = t.replace(";", "");

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				out.append(Character.toLowerCase(c));
			}
		}
		return out.toString();
	}

	private static String toSpokenMaterialName(String raw) {
		if (raw == null || raw.isBlank()) {
			return "material";
		}

		String t = raw.trim();
		if (t.startsWith("$")) {
			t = t.substring(1);
		}
		t = t.replace("_Name", "").replace("_name", "").replace(";", "");

		// LowTemperatureDiamonds -> Low Temperature Diamonds
		t = t.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
		t = t.replace('_', ' ');
		t = t.replaceAll("\\s+", " ").trim();
		return t;
	}


	/**
	 * Attach a generic hover handler to a button; when the mouse rests over
	 * the button for the given delay, the action is invoked on the EDT.
	 */
	private static void installHoverSwitch(JButton button, int delayMs, Runnable action) {
		TabHoverPoller.register(button, delayMs, action);
	}

	/**
	 * Global tab hover poller: periodically polls the global mouse position and,
	 * if it is resting on any registered tab button longer than the configured
	 * delay, invokes that tab's action (typically button.doClick()).
	 *
	 * This works even when the overlay is in OS pass-through mode because it
	 * does not depend on Swing mouse events.
	 */
	private static class TabHoverPoller implements ActionListener {

		private static final int POLL_INTERVAL_MS = 40;

		private static final List<Entry> entries = new ArrayList<>();
		private static final Timer pollTimer;

		static {
			TabHoverPoller listener = new TabHoverPoller();
			pollTimer = new Timer(POLL_INTERVAL_MS, listener);
			pollTimer.start();
		}

		private static class Entry {
			final JButton button;
			final int delayMs;
			final Runnable action;

			long hoverStartMs = -1L;
			boolean firedForCurrentHover = false;

			Entry(JButton button, int delayMs, Runnable action) {
				this.button = button;
				this.delayMs = delayMs;
				this.action = action;
			}
		}

		static void register(JButton button, int delayMs, Runnable action) {
			entries.add(new Entry(button, delayMs, action));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (entries.isEmpty()) {
				return;
			}

			PointerInfo pointerInfo = MouseInfo.getPointerInfo();
			if (pointerInfo == null) {
				resetAll();
				return;
			}

			Point mouseOnScreen = pointerInfo.getLocation();
			long now = System.currentTimeMillis();

			for (Entry entry : entries) {
				JButton button = entry.button;
				if (button == null || !button.isShowing()) {
					entry.hoverStartMs = -1L;
					entry.firedForCurrentHover = false;
					continue;
				}

				Point buttonLoc;
				try {
					buttonLoc = button.getLocationOnScreen();
				} catch (IllegalStateException ex) {
					entry.hoverStartMs = -1L;
					entry.firedForCurrentHover = false;
					continue;
				}

				Rectangle bounds = new Rectangle(
						buttonLoc.x,
						buttonLoc.y,
						button.getWidth(),
						button.getHeight()
						);

				if (bounds.contains(mouseOnScreen)) {
					if (entry.hoverStartMs < 0L) {
						entry.hoverStartMs = now;
						entry.firedForCurrentHover = false;
					} else if (!entry.firedForCurrentHover && now - entry.hoverStartMs >= entry.delayMs) {
						if (entry.action != null) {
							SwingUtilities.invokeLater(entry.action);
						}
						entry.firedForCurrentHover = true;
					}
				} else {
					entry.hoverStartMs = -1L;
					entry.firedForCurrentHover = false;
				}
			}
		}

		private static void resetAll() {
			for (Entry entry : entries) {
				entry.hoverStartMs = -1L;
				entry.firedForCurrentHover = false;
			}
		}
	}

	private JButton createTabButton(String text) {
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.setFocusPainted(false);
		button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));

		// Slightly translucent dark background so tabs are legible but not huge blocks
		button.setOpaque(!OverlayPreferences.isOverlayTransparent());
		button.setBackground(new Color(50, 50, 50, 220));

		applyTabButtonStyle(button);
		return button;
	}

	private javax.swing.border.Border createTabBorder(Color c) {
		return javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createLineBorder(c, 1, true),
				javax.swing.BorderFactory.createEmptyBorder(
						TAB_PADDING.top,
						TAB_PADDING.left,
						TAB_PADDING.bottom,
						TAB_PADDING.right
						)
				);
	}

	private void selectTab(String cardName, JButton selectedButton) {
		if (routeButton != null) {
			routeButton.setSelected(selectedButton == routeButton);
		}
		if (systemButton != null) {
			systemButton.setSelected(selectedButton == systemButton);
		}
		if (biologyButton != null) {
			biologyButton.setSelected(selectedButton == biologyButton);
		}
		if (miningButton != null) {
			miningButton.setSelected(selectedButton == miningButton);
		}

		applyTabButtonStyle(routeButton);
		applyTabButtonStyle(systemButton);
		applyTabButtonStyle(biologyButton);
		applyTabButtonStyle(miningButton);
		
		cardLayout.show(cardPanel, cardName);
	}

	private void applyTabButtonStyle(JButton button) {
		if (button == null) {
			return;
		}

		Color c = button.isSelected() ? TAB_WHITE : TAB_ORANGE;

		// This restores size/padding compared to a bare LineBorder.
		button.setMargin(TAB_PADDING);
		button.setForeground(c);
		button.setBorder(createTabBorder(c));
	}

	private void showRouteTabFromStatusWatcher() {
		SwingUtilities.invokeLater(() -> selectTab(CARD_ROUTE, routeButton));
	}

	private void showSystemTabFromStatusWatcher() {
		SwingUtilities.invokeLater(() -> selectTab(CARD_SYSTEM, systemButton));
	}

	/**
	 * Watches Elite Dangerous Status.json and switches tabs when the player
	 * opens the Galaxy Map (Route tab) or System Map (System tab).
	 */
	private static class GuiFocusWatcher implements Runnable {

		private static final long POLL_INTERVAL_MS = 200L;

		private final EliteOverlayTabbedPane parent;
		private final Path statusPath;
		private final Gson gson = new Gson();

		private volatile boolean running = true;
		private int lastGuiFocus = -1;

		GuiFocusWatcher(EliteOverlayTabbedPane parent) {
			this.parent = parent;

			String home = System.getProperty("user.home");
			this.statusPath = Path.of(
					home,
					"Saved Games",
					"Frontier Developments",
					"Elite Dangerous",
					"Status.json");
		}

		@Override
		public void run() {
			while (running) {
				try {
					pollOnce();
					Thread.sleep(POLL_INTERVAL_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (IOException e) {
					try {
						Thread.sleep(500L);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}

		private void pollOnce() throws IOException {
			if (!Files.exists(statusPath)) {
				return;
			}

			try (Reader reader = Files.newBufferedReader(statusPath, StandardCharsets.UTF_8)) {
				JsonObject root = gson.fromJson(reader, JsonObject.class);
				if (root == null || !root.has("GuiFocus")) {
					return;
				}

				int guiFocus = root.get("GuiFocus").getAsInt();
				if (guiFocus != lastGuiFocus) {
					handleGuiFocusChange(guiFocus);
					lastGuiFocus = guiFocus;
				}
			}
		}

		private void handleGuiFocusChange(int guiFocus) {
			// 6 = Galaxy Map -> Route tab
			if (guiFocus == 6) {
				parent.showRouteTabFromStatusWatcher();
			}
			// 7 = System Map -> System tab
			else if (guiFocus == 7) {
				parent.showSystemTabFromStatusWatcher();
			}
		}
	}

	private static class HoverSwitchHandler extends MouseAdapter {

		private final Timer hoverTimer;
		private final Runnable action;

		HoverSwitchHandler(int delayMs, Runnable action) {
			this.action = action;
			this.hoverTimer = new Timer(delayMs, e -> {
				if (this.action != null) {
					this.action.run();
				}
			});
			this.hoverTimer.setRepeats(false);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			hoverTimer.restart();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			hoverTimer.restart();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			hoverTimer.stop();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			hoverTimer.stop();
		}
	}


	public void applyOverlayTransparency(boolean transparent) {
		applyOverlayBackground(new Color(0, 0, 0, transparent ? 0 : 255), transparent);
	}

	public void applyOverlayBackground(Color bgWithAlpha, boolean treatAsTransparent) {
		boolean opaque = !treatAsTransparent;

		setOpaque(opaque);
		setBackground(bgWithAlpha);

		tabBar.setOpaque(opaque);
		tabBar.setBackground(bgWithAlpha);

		cardPanel.setOpaque(opaque);
		cardPanel.setBackground(bgWithAlpha);

		revalidate();
		repaint();
	}
	public static void maybeRemindAboutLimpets() {
		Path journalDir = OverlayPreferences.resolveJournalDirectory(EliteDangerousOverlay.clientKey);
		Path cargoFile = EliteLogFileLocator.findCargoFile(journalDir);
		Path modulesFile = EliteLogFileLocator.findModulesInfoFile(journalDir);
		
		// Avoid spamming if multiple events fire close together.
		long now = System.currentTimeMillis();
//		if (now - lastLimpetReminderMs < 60_000L) {
//			return;
//		}
		if (!OverlayPreferences.isSpeechEnabled()) {
			return;
		}
		if (!OverlayPreferences.isMiningLowLimpetReminderEnabled()) {
			return;
		}
		JsonObject cargo = readJsonObject(cargoFile);
		JsonObject modules = readJsonObject(modulesFile);
		int numLimpets = getLimpetCount(cargo);
		
		boolean lowLimpets = false;
		if (OverlayPreferences.getMiningLowLimpetReminderMode() == MiningLimpetReminderMode.COUNT) {
			lowLimpets = numLimpets < OverlayPreferences.getMiningLowLimpetReminderThreshold();
		} else {
			Integer cargoCapacity = (getLatestLoadout() == null) ? 0 : getLatestLoadout().getCargoCapacity();

			if (cargoCapacity == null || cargoCapacity <= 0) {
				// Without CargoCapacity, the percent threshold is meaningless.
				return;
			}
			double percentage = (numLimpets*100) / cargoCapacity;
			
			lowLimpets = percentage < OverlayPreferences.getMiningLowLimpetReminderThresholdPercent();
		}



		if (cargoFile == null || modulesFile == null) {
			return;
		}


		if (!hasMiningEquipment(modules)) {
			return;
		}


		if (!lowLimpets) {
			return;
		}
		TtsSprintf tts = new TtsSprintf(new PollyTtsCached());
		tts.speakf("Did you forget your limpets again commander?");
	}

	private static JsonObject readJsonObject(Path file) {
		if (file == null) {
			return null;
		}
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement el = JsonParser.parseReader(r);
			if (el != null && el.isJsonObject()) {
				return el.getAsJsonObject();
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	private static int getLimpetCount(JsonObject cargo) {
		if (cargo == null) {
			return 0;
		}

		JsonArray inv = null;
		if (cargo.has("Inventory") && cargo.get("Inventory").isJsonArray()) {
			inv = cargo.getAsJsonArray("Inventory");
		} else if (cargo.has("inventory") && cargo.get("inventory").isJsonArray()) {
			inv = cargo.getAsJsonArray("inventory");
		}

		if (inv == null) {
			return 0;
		}

		for (JsonElement e : inv) {
			if (e == null || !e.isJsonObject()) {
				continue;
			}

			JsonObject o = e.getAsJsonObject();

			String name = null;
			if (o.has("Name") && !o.get("Name").isJsonNull()) {
				try {
					name = o.get("Name").getAsString();
				} catch (Exception ignored) {
				}
			} else if (o.has("name") && !o.get("name").isJsonNull()) {
				try {
					name = o.get("name").getAsString();
				} catch (Exception ignored) {
				}
			}

			if (name == null || !name.equalsIgnoreCase("drones")) {
				continue;
			}

			if (o.has("Count") && !o.get("Count").isJsonNull()) {
				try {
					return (int) o.get("Count").getAsLong();
				} catch (Exception ignored) {
				}
			} else if (o.has("count") && !o.get("count").isJsonNull()) {
				try {
					return (int) o.get("count").getAsLong();
				} catch (Exception ignored) {
				}
			}

			return 0;
		}

		return 0;
	}


	private static boolean isCargoEmpty(JsonObject cargo) {
		if (cargo == null) {
			return true;
		}

		// Typical format: { "Inventory": [ { "Name": "drones", "Count": 32, ... }, ... ] }
		JsonArray inv = null;
		if (cargo.has("Inventory") && cargo.get("Inventory").isJsonArray()) {
			inv = cargo.getAsJsonArray("Inventory");
		} else if (cargo.has("inventory") && cargo.get("inventory").isJsonArray()) {
			inv = cargo.getAsJsonArray("inventory");
		}
		if (inv == null) {
			return true;
		}

		long total = 0;
		for (JsonElement e : inv) {
			if (e == null || !e.isJsonObject()) {
				continue;
			}
			JsonObject o = e.getAsJsonObject();
			if (o.has("Count") && !o.get("Count").isJsonNull()) {
				try {
					total += o.get("Count").getAsLong();
				} catch (Exception ignored) {
				}
			} else if (o.has("count") && !o.get("count").isJsonNull()) {
				try {
					total += o.get("count").getAsLong();
				} catch (Exception ignored) {
				}
			}
		}
		return total <= 0;
	}
	private static boolean hasMiningEquipment(JsonObject modulesInfo) {
		if (modulesInfo == null) {
			return false;
		}

		JsonArray mods = null;
		if (modulesInfo.has("Modules") && modulesInfo.get("Modules").isJsonArray()) {
			mods = modulesInfo.getAsJsonArray("Modules");
		} else if (modulesInfo.has("modules") && modulesInfo.get("modules").isJsonArray()) {
			mods = modulesInfo.getAsJsonArray("modules");
		}
		if (mods == null) {
			return false;
		}

		// Conservative keyword match on module item names.
		String[] miningKeywords = new String[] {
				"mining",
				"abrasion",
				"seismic",
				"subsurf",
				"displacement",
		};

		for (JsonElement e : mods) {
			if (e == null || !e.isJsonObject()) {
				continue;
			}
			JsonObject m = e.getAsJsonObject();

			String item = null;
			if (m.has("Item") && !m.get("Item").isJsonNull()) {
				try {
					item = m.get("Item").getAsString();
				} catch (Exception ignored) {
				}
			} else if (m.has("item") && !m.get("item").isJsonNull()) {
				try {
					item = m.get("item").getAsString();
				} catch (Exception ignored) {
				}
			}

			if (item == null || item.isBlank()) {
				continue;
			}

			String norm = item.toLowerCase(Locale.US);
			for (String kw : miningKeywords) {
				if (norm.contains(kw)) {
					return true;
				}
			}
		}
		return false;
	}


	public void applyUiFontPreferences() {
		systemTab.applyUiFontPreferences();
		routeTab.applyUiFontPreferences();
		biologyTab.applyUiFontPreferences();
		miningTab.applyUiFontPreferences();
		revalidate();
		repaint();
	}

	public void applyUiFont(Font font) {
		systemTab.applyUiFont(font);
		routeTab.applyUiFont(font);
		biologyTab.applyUiFont(font);
		miningTab.applyUiFont(font);
		revalidate();
		repaint();
	}


}
