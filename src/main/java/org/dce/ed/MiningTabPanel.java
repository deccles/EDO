package org.dce.ed;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.LayerUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.dce.ed.logreader.EliteLogFileLocator;
import org.dce.ed.logreader.event.ProspectedAsteroidEvent;
import org.dce.ed.logreader.event.ProspectedAsteroidEvent.MaterialProportion;
import org.dce.ed.market.GalacticAveragePrices;
import org.dce.ed.market.MaterialNameMatcher;
import org.dce.ed.tts.PollyTtsCached;
import org.dce.ed.tts.TtsSprintf;
import org.dce.ed.ui.EdoUi;
import org.dce.ed.ui.EdoUi.User;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Overlay tab: Mining
 *
 * Shows the most recent ProspectedAsteroid materials sorted by galactic average value.
 */
public class MiningTabPanel extends JPanel {

	private final TtsSprintf tts = new TtsSprintf(new PollyTtsCached());
	private String lastProspectorAnnouncementSig;
	private Set<String> prospectorHighlightNames = new HashSet<>();

	private final GalacticAveragePrices prices;
	private final MaterialNameMatcher matcher;

	private final JLabel headerLabel;
	private final JLabel inventoryLabel;
	private final JTable table;
	private final MiningTableModel model;

	private final JTable cargoTable;
	private final MiningTableModel cargoModel;

	private final JScrollPane materialsScroller;
	private final JScrollPane cargoScroller;


	private final Map<String, Long> lastCargoTonsByName = new HashMap<>();

	/** Inventory tons by commodity (display name) at the time of the previous ProspectedAsteroid event. */
	private Map<String, Double> lastInventoryTonsAtProspector = new HashMap<>();

	private final TableScanState prospectorScan;
private final TableScanState cargoScan;
private final JLayer<JTable> prospectorLayer;
private final JLayer<JTable> cargoLayer;


	private Font uiFont;
	private JLabel prospectorLabel;

	private static final int VISIBLE_ROWS = 10;

	// Row colors for mining tables.
	private static final Color CORE_COLOR = EdoUi.User.VALUABLE;
	private static final Color NON_CORE_GREEN = EdoUi.User.SUCCESS;

	public MiningTabPanel(GalacticAveragePrices prices) {
		super(new BorderLayout());
		this.prices = prices;

		
		this.matcher = new MaterialNameMatcher(prices);
// Always render transparent so passthrough mode looks right.
		setOpaque(false);
		setBackground(EdoUi.Internal.TRANSPARENT);

		headerLabel = new JLabel("Mining (latest prospector)");
		headerLabel.setForeground(EdoUi.User.MAIN_TEXT);
		headerLabel.setHorizontalAlignment(SwingConstants.LEFT);
		headerLabel.setOpaque(false);

		prospectorLabel = new JLabel("Prospector Limpet");
		prospectorLabel.setForeground(EdoUi.User.MAIN_TEXT);
		prospectorLabel.setFont(prospectorLabel.getFont().deriveFont(Font.BOLD));
		prospectorLabel.setHorizontalAlignment(SwingConstants.LEFT);
		prospectorLabel.setOpaque(false);
		prospectorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		Font base = OverlayPreferences.getUiFont();
		prospectorLabel.setFont(base.deriveFont(Font.BOLD, OverlayPreferences.getUiFontSize() + 4));

		// Let it span the width so BoxLayout doesn't center it
		Dimension pref = prospectorLabel.getPreferredSize();
		prospectorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

		
		inventoryLabel = new JLabel("Ship Inventory");
		inventoryLabel.setForeground(EdoUi.User.MAIN_TEXT);
		inventoryLabel.setFont(inventoryLabel.getFont().deriveFont(Font.BOLD));
		inventoryLabel.setHorizontalAlignment(SwingConstants.LEFT);
		inventoryLabel.setOpaque(false);
		inventoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		inventoryLabel.setFont(base.deriveFont(Font.BOLD, OverlayPreferences.getUiFontSize() + 4));

		model = new MiningTableModel("Est. Tons");

		table = new JTable(model) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

			@Override
			public boolean editCellAt(int row, int column, java.util.EventObject e) {
				return false;
			}

			@Override
			protected void configureEnclosingScrollPane() {
				super.configureEnclosingScrollPane();

				Container p = SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
				if (p instanceof JScrollPane) {
					JScrollPane sp = (JScrollPane)p;
					sp.setBorder(BorderFactory.createEmptyBorder());
					sp.setViewportBorder(BorderFactory.createEmptyBorder());

					sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
					sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

					sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
					sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

					// Some Look-and-Feels still paint a faint outline when border is null; force an empty border.
					sp.setBorder(BorderFactory.createEmptyBorder());
					sp.setViewportBorder(BorderFactory.createEmptyBorder());

					JViewport hv = sp.getColumnHeader();
					if (hv != null) {
						hv.setOpaque(false);
						hv.setBackground(EdoUi.Internal.TRANSPARENT);
						hv.setBorder(null);
					}
				}
			}
		};

		// Hard-disable editing and selection (passthrough-friendly visuals).
		table.setDefaultEditor(Object.class, null);
		table.setDefaultEditor(String.class, null);
		table.setFocusable(false);
		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		table.setCellSelectionEnabled(false);
		table.setSurrendersFocusOnKeystroke(false);
		table.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

		table.setOpaque(false);
		table.setBorder(null);
		table.setFillsViewportHeight(true);

		table.setShowGrid(false);
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);
		table.setIntercellSpacing(new java.awt.Dimension(0, 0));
		table.setGridColor(EdoUi.Internal.TRANSPARENT);

		table.setForeground(EdoUi.User.MAIN_TEXT);
		table.setBackground(EdoUi.Internal.TRANSPARENT);
		table.setRowHeight(22);

		table.setTableHeader(new TransparentTableHeader(table.getColumnModel()));

		JTableHeader th = table.getTableHeader();
		if (th != null) {
			th.setOpaque(true);
			th.setForeground(EdoUi.User.MAIN_TEXT);
			th.setBackground(EdoUi.User.BACKGROUND);
			th.setBorder(null);
			th.setReorderingAllowed(false);
			th.setFocusable(false);
			th.putClientProperty("JTableHeader.focusCellBackground", null);
			th.putClientProperty("JTableHeader.cellBorder", null);
			th.setDefaultRenderer(new HeaderRenderer());

			th.setPreferredSize(new Dimension(pref.width, table.getRowHeight()));
			
			// Give the Material column more room (prevents truncation like "Grandidierite...").
			table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			applyMiningColumnWidths(table);
			
		}

		DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			{
				setOpaque(false);
				setForeground(EdoUi.User.MAIN_TEXT);
			}

			@Override
			public Component getTableCellRendererComponent(JTable tbl,
					Object value,
					boolean isSelected,
					boolean hasFocus,
					int row,
					int column) {
				Component c = super.getTableCellRendererComponent(tbl,
						value,
						false,
						false,
						row,
						column);

				if (c instanceof JLabel) {
					JLabel l = (JLabel)c;
					l.setFont(tbl.getFont());
					Color base = resolveRowForeground(tbl, row);
					float reveal = getRevealAlpha(tbl, row);
					float flare = getFlareAlpha(tbl, row);
					l.setForeground(applyRevealAndFlare(base, reveal, flare));
					if (isSummaryRow(tbl, row)) {
						l.setFont(l.getFont().deriveFont(Font.BOLD));
						c.setForeground(Color.green.darker());
					}
					l.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
					l.setBorder(new EmptyBorder(3, 4, 3, 4));
					l.setOpaque(false);
				}

				return c;
			}

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D)g.create();
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);

				super.paintComponent(g2);

				g2.setColor(EdoUi.Internal.tableHeaderTopBorder());
				g2.drawLine(0, 0, getWidth(), 0);

				g2.dispose();
			}
		};
		table.setDefaultRenderer(Object.class, defaultRenderer);

		prospectorScan = new TableScanState(table);
		prospectorLayer = new JLayer<>(table, new ScanLayerUi(prospectorScan));

		materialsScroller = new JScrollPane(prospectorLayer);
		materialsScroller.setOpaque(false);
		materialsScroller.setBackground(EdoUi.Internal.TRANSPARENT);
		materialsScroller.getViewport().setOpaque(false);
		materialsScroller.getViewport().setBackground(EdoUi.Internal.TRANSPARENT);
		materialsScroller.setBorder(null);
		materialsScroller.setViewportBorder(null);

		JViewport headerViewport = materialsScroller.getColumnHeader();
		if (headerViewport != null) {
			headerViewport.setOpaque(false);
			headerViewport.setBackground(EdoUi.Internal.TRANSPARENT);
			headerViewport.setBorder(null);
		}

		configureOverlayScroller(materialsScroller);
		materialsScroller.setAlignmentX(Component.LEFT_ALIGNMENT);

		// ----- Cargo table -----
		cargoModel = new MiningTableModel("Tons");
		cargoTable = new JTable(cargoModel);

		cargoTable.setAutoCreateRowSorter(true);
		cargoTable.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

		cargoTable.setOpaque(false);
		cargoTable.setBorder(null);
		cargoTable.setFillsViewportHeight(true);

		cargoTable.setShowGrid(false);
		cargoTable.setShowHorizontalLines(false);
		cargoTable.setShowVerticalLines(false);
		cargoTable.setIntercellSpacing(new java.awt.Dimension(0, 0));
		cargoTable.setGridColor(EdoUi.Internal.TRANSPARENT);

		cargoTable.setForeground(EdoUi.User.MAIN_TEXT);
		cargoTable.setBackground(EdoUi.Internal.TRANSPARENT);
		cargoTable.setRowHeight(22);

		cargoTable.setTableHeader(new TransparentTableHeader(cargoTable.getColumnModel()));

		JTableHeader cargoHeader = cargoTable.getTableHeader();
		if (cargoHeader != null) {
			cargoHeader.setOpaque(false);
			cargoHeader.setForeground(EdoUi.User.MAIN_TEXT);
			cargoHeader.setBackground(EdoUi.Internal.TRANSPARENT);
			cargoHeader.setBorder(null);
			cargoHeader.setReorderingAllowed(false);
			cargoHeader.setFocusable(false);
			cargoHeader.putClientProperty("JTableHeader.focusCellBackground", null);
			cargoHeader.putClientProperty("JTableHeader.cellBorder", null);
			cargoHeader.setDefaultRenderer(new HeaderRenderer());

			cargoHeader.setPreferredSize(new Dimension(pref.width, cargoTable.getRowHeight()));
			
			cargoTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			applyMiningColumnWidths(cargoTable);
		}

		for (int c = 0; c < cargoTable.getColumnModel().getColumnCount(); c++) {
			cargoTable.getColumnModel().getColumn(c).setCellRenderer(defaultRenderer);
		}

		cargoScan = new TableScanState(cargoTable);
		cargoLayer = new JLayer<>(cargoTable, new ScanLayerUi(cargoScan));

		cargoScroller = new JScrollPane(cargoLayer);
		cargoScroller.setOpaque(false);
		cargoScroller.setBackground(EdoUi.Internal.TRANSPARENT);
		cargoScroller.getViewport().setOpaque(false);
		cargoScroller.getViewport().setBackground(EdoUi.Internal.TRANSPARENT);
		cargoScroller.setBorder(null);
		cargoScroller.setViewportBorder(null);

		JViewport cargoHeaderViewport = cargoScroller.getColumnHeader();
		if (cargoHeaderViewport != null) {
			cargoHeaderViewport.setOpaque(false);
			cargoHeaderViewport.setBackground(EdoUi.Internal.TRANSPARENT);
			cargoHeaderViewport.setBorder(null);
		}

		configureOverlayScroller(cargoScroller);
		cargoScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
		inventoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		Dimension invPref = inventoryLabel.getPreferredSize();
		inventoryLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, invPref.height));

		// Leave about 10 rows for each table.
		updateScrollerHeights();

		JPanel centerPanel = new JPanel();
		centerPanel.setOpaque(false);
		centerPanel.setBackground(EdoUi.Internal.TRANSPARENT);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		
		centerPanel.add(prospectorLabel);
		centerPanel.add(Box.createVerticalStrut(4)); // small gap, optional
		centerPanel.add(materialsScroller);
		centerPanel.add(Box.createVerticalStrut(8));
		centerPanel.add(inventoryLabel);
		centerPanel.add(Box.createVerticalStrut(2));
		centerPanel.add(cargoScroller);

		add(centerPanel, BorderLayout.CENTER);

		CargoMonitor.getInstance().addListener(snap -> SwingUtilities.invokeLater(() -> updateFromCargoSnapshot(snap)));
		updateFromCargoSnapshot(CargoMonitor.getInstance().getSnapshot());


		applyUiFontPreferences();
	}

	
	private boolean isHighlightedProspectorRow(Row r) {
		if (r == null) {
			return false;
		}
		return prospectorHighlightNames.contains(r.getName());
	}

	private static void applyMiningColumnWidths(JTable tbl) {
		if (tbl == null) {
			return;
		}

		TableColumnModel cm = tbl.getColumnModel();
		if (cm == null || cm.getColumnCount() < 5) {
			return;
		}

		// Material | Percent | Avg Cr/t | Tons | Est. Value
		cm.getColumn(0).setMinWidth(170);
		cm.getColumn(0).setPreferredWidth(260);

		cm.getColumn(1).setMinWidth(55);
		cm.getColumn(1).setPreferredWidth(70);

		cm.getColumn(2).setMinWidth(70);
		cm.getColumn(2).setPreferredWidth(85);

		cm.getColumn(3).setMinWidth(55);
		cm.getColumn(3).setPreferredWidth(70);

		cm.getColumn(4).setMinWidth(75);
		cm.getColumn(4).setPreferredWidth(95);
	}



private static final int GREEN_THRESHOLD_AVG_CR_PER_TON = 4_000_000;
	private Color resolveRowForeground(JTable tbl, int viewRow) {
		if (tbl == null || viewRow < 0) {
			return EdoUi.User.MAIN_TEXT;
		}

		if (tbl == table) {
			int modelRow = viewRow;
			if (tbl.getRowSorter() != null) {
				modelRow = tbl.convertRowIndexToModel(viewRow);
			}

			Row r = model.getRow(modelRow);
			if (r != null) {
				if (r.isCore()) {
					return CORE_COLOR;
				}
				if (isHighlightedProspectorRow(r)) {
					return NON_CORE_GREEN;
				}
				if (r.getEstimatedValue() > GREEN_THRESHOLD_AVG_CR_PER_TON) {
					return NON_CORE_GREEN;
				}
			}
return EdoUi.User.MAIN_TEXT;
		}

		return EdoUi.User.MAIN_TEXT;
	}

	private boolean isSummaryRow(JTable tbl, int viewRow) {
		if (tbl == null || viewRow < 0) {
			return false;
		}
		if (tbl == cargoTable) {
			int modelRow = viewRow;
			if (tbl.getRowSorter() != null) {
				modelRow = tbl.convertRowIndexToModel(viewRow);
			}
			Row r = cargoModel.getRow(modelRow);
			return r != null && r.isSummary();
		}
		return false;
	}

	private float getRevealAlpha(JTable tbl, int viewRow) {
		if (tbl == table) {
			int modelRow = viewRow;
			if (tbl.getRowSorter() != null) {
				modelRow = tbl.convertRowIndexToModel(viewRow);
			}
			return prospectorScan.getRevealAlpha(modelRow);
		}
		if (tbl == cargoTable) {
			int modelRow = viewRow;
			if (tbl.getRowSorter() != null) {
				modelRow = tbl.convertRowIndexToModel(viewRow);
			}
			return cargoScan.getRevealAlpha(modelRow);
		}
		return 1.0f;
	}

	private float getFlareAlpha(JTable tbl, int viewRow) {
		if (tbl == table) {
			int modelRow = viewRow;
			if (tbl.getRowSorter() != null) {
				modelRow = tbl.convertRowIndexToModel(viewRow);
			}
			return prospectorScan.getFlareAlpha(modelRow);
		}
		if (tbl == cargoTable) {
			int modelRow = viewRow;
			if (tbl.getRowSorter() != null) {
				modelRow = tbl.convertRowIndexToModel(viewRow);
			}
			return cargoScan.getFlareAlpha(modelRow);
		}
		return 0.0f;
	}

	private static Color applyRevealAndFlare(Color base, float reveal, float flare) {
		reveal = Math.max(0.0f, Math.min(1.0f, reveal));
		flare = Math.max(0.0f, Math.min(1.0f, flare));

		int r = base.getRed();
		int g = base.getGreen();
		int b = base.getBlue();

		int add = (int) (180f * flare);   // HOTTER glow

		// Push red harder, suppress green/blue for orange rows
		r = Math.min(255, r + add);
		g = Math.min(255, g + (int) (add * 0.35f));
		b = Math.min(255, b + (int) (add * 0.20f));

		int a = (int)(255.0f * reveal);
		return EdoUi.rgba(r, g, b, a);
	}

	public void applyUiFontPreferences() {
		applyUiFont(OverlayPreferences.getUiFont());
	}

	public void applyUiFont(Font font) {
		if (font == null) {
			return;
		}

		Font base = OverlayPreferences.getUiFont();
		Font headerFont = base.deriveFont(Font.BOLD, OverlayPreferences.getUiFontSize() + 4);
		
		prospectorLabel.setFont(headerFont);
		inventoryLabel.setFont(headerFont);
		
		uiFont = font;

		headerLabel.setFont(uiFont.deriveFont(Font.BOLD));

		table.setFont(uiFont);
		if (table.getTableHeader() != null) {
			table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));
		}

		cargoTable.setFont(uiFont);
		if (cargoTable.getTableHeader() != null) {
			cargoTable.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));
		}

		int rowH = Math.max(18, uiFont.getSize() + 6);

		table.setRowHeight(rowH);
		cargoTable.setRowHeight(rowH);

		JTableHeader th = table.getTableHeader();
		if (th != null) {
			Dimension pref = th.getPreferredSize();
			th.setPreferredSize(new Dimension(pref.width, rowH));
		}

		JTableHeader cth = cargoTable.getTableHeader();
		if (cth != null) {
			Dimension pref = cth.getPreferredSize();
			cth.setPreferredSize(new Dimension(pref.width, rowH));
		}
		applyMiningColumnWidths(table);
		applyMiningColumnWidths(cargoTable);

		updateScrollerHeights();

		revalidate();
		repaint();
	}



	private void updateScrollerHeights() {
		updateScrollerHeight(materialsScroller, table);
		updateScrollerHeight(cargoScroller, cargoTable);
	}

	private static void updateScrollerHeight(JScrollPane scroller, JTable tbl) {
		if (scroller == null || tbl == null) {
			return;
		}

		int headerH = 0;
		JTableHeader th = tbl.getTableHeader();
		if (th != null) {
			headerH = th.getPreferredSize().height;
		}

		int h = (tbl.getRowHeight() * VISIBLE_ROWS) + headerH;

		scroller.setPreferredSize(new Dimension(Integer.MAX_VALUE, h));
		scroller.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
		scroller.revalidate();
	}

	private List<Row> withTotalRow(List<Row> rows) {
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}

		double totalTons = 0.0;
		double totalValue = 0.0;
		List<Row> out = new ArrayList<>();
		for (Row r : rows) {
			if (r == null || r.isSummary()) {
				continue;
			}
			out.add(r);
			totalTons += r.getExpectedTons();
			totalValue += r.getEstimatedValue();
		}

		out.add(new Row("Total", Double.NaN, 0, totalTons, totalValue, false, true));
		return out;
	}


	private void updateFromCargoSnapshot(CargoMonitor.Snapshot snap) {
		try {
			if (snap == null || snap.getCargoJson() == null) {
				cargoModel.setRows(List.of());
				return;
			}

			JsonObject cargoObj = snap.getCargoJson();
			List<Row> rows = buildRowsFromCargo(cargoObj);
			Set<Integer> changedModelRows = computeChangedInventoryModelRows(rows);
			cargoModel.setRows(withTotalRow(rows));
			cargoScan.startInventoryScan(cargoLayer, changedModelRows);
		} catch (Exception ignored) {
		}
	}


	private List<Row> buildRowsFromCargo(JsonObject cargo) {
		if (cargo == null) {
			return List.of();
		}

		JsonArray inv = null;
		if (cargo.has("Inventory") && cargo.get("Inventory").isJsonArray()) {
			inv = cargo.getAsJsonArray("Inventory");
		} else if (cargo.has("inventory") && cargo.get("inventory").isJsonArray()) {
			inv = cargo.getAsJsonArray("inventory");
		}
		if (inv == null) {
			return List.of();
		}

		List<Row> rows = new ArrayList<>();
		for (JsonElement e : inv) {
			if (e == null || !e.isJsonObject()) {
				continue;
			}
			JsonObject o = e.getAsJsonObject();

			String rawName = null;
			if (o.has("Name") && !o.get("Name").isJsonNull()) {
				rawName = o.get("Name").getAsString();
			} else if (o.has("name") && !o.get("name").isJsonNull()) {
				rawName = o.get("name").getAsString();
			}
			if (rawName == null || rawName.isBlank()) {
				continue;
			}

			String localizedName = null;
			if (o.has("Name_Localised") && !o.get("Name_Localised").isJsonNull()) {
				localizedName = o.get("Name_Localised").getAsString();
			} else if (o.has("Name_Localized") && !o.get("Name_Localized").isJsonNull()) {
				localizedName = o.get("Name_Localized").getAsString();
			} else if (o.has("name_localised") && !o.get("name_localised").isJsonNull()) {
				localizedName = o.get("name_localised").getAsString();
			} else if (o.has("name_localized") && !o.get("name_localized").isJsonNull()) {
				localizedName = o.get("name_localized").getAsString();
			}
			long count = 0;
			if (o.has("Count") && !o.get("Count").isJsonNull()) {
				try {
					count = o.get("Count").getAsLong();
				} catch (Exception ignored) {
				}
			} else if (o.has("count") && !o.get("count").isJsonNull()) {
				try {
					count = o.get("count").getAsLong();
				} catch (Exception ignored) {
				}
			}
			if (count <= 0) {
				continue;
			}

			String shownName = (localizedName != null && !localizedName.isBlank()) ? localizedName : toUiName(rawName);
			int avg = lookupAvgSell(rawName, shownName);

			double tons = count;
			double value = (avg > 0) ? (tons * avg) : 0;
			rows.add(new Row(shownName, avg, tons, value));
		}

		rows.sort(Comparator
				.comparing(Row::isCore).reversed()
				.thenComparingDouble(Row::getEstimatedValue).reversed()
				.thenComparing(Row::getName, String.CASE_INSENSITIVE_ORDER));

		return rows;
	}

	/**
	 * Builds a map of commodity display name -> total tons from Cargo.json Inventory.
	 * Used to compare inventory at each ProspectorEvent and compute ton deltas for CSV logging.
	 */
	private Map<String, Double> buildInventoryTonsFromCargo(JsonObject cargo) {
		return buildInventoryTonsFromCargo(cargo, this::toUiName);
	}

	/**
	 * Static variant for unit tests; nameResolver maps raw item name to display name.
	 */
	static Map<String, Double> buildInventoryTonsFromCargo(JsonObject cargo, Function<String, String> nameResolver) {
		Map<String, Double> out = new HashMap<>();
		if (cargo == null || nameResolver == null) {
			return out;
		}
		JsonArray inv = null;
		if (cargo.has("Inventory") && cargo.get("Inventory").isJsonArray()) {
			inv = cargo.getAsJsonArray("Inventory");
		} else if (cargo.has("inventory") && cargo.get("inventory").isJsonArray()) {
			inv = cargo.get("inventory").getAsJsonArray();
		}
		if (inv == null) {
			return out;
		}
		for (JsonElement e : inv) {
			if (e == null || !e.isJsonObject()) {
				continue;
			}
			JsonObject o = e.getAsJsonObject();
			String rawName = null;
			if (o.has("Name") && !o.get("Name").isJsonNull()) {
				rawName = o.get("Name").getAsString();
			} else if (o.has("name") && !o.get("name").isJsonNull()) {
				rawName = o.get("name").getAsString();
			}
			if (rawName == null || rawName.isBlank()) {
				continue;
			}
			String localizedName = null;
			if (o.has("Name_Localised") && !o.get("Name_Localised").isJsonNull()) {
				localizedName = o.get("Name_Localised").getAsString();
			} else if (o.has("Name_Localized") && !o.get("Name_Localized").isJsonNull()) {
				localizedName = o.get("Name_Localized").getAsString();
			} else if (o.has("name_localised") && !o.get("name_localised").isJsonNull()) {
				localizedName = o.get("name_localised").getAsString();
			} else if (o.has("name_localized") && !o.get("name_localized").isJsonNull()) {
				localizedName = o.get("name_localized").getAsString();
			}
			long count = 0;
			if (o.has("Count") && !o.get("Count").isJsonNull()) {
				try {
					count = o.get("Count").getAsLong();
				} catch (Exception ignored) {
				}
			} else if (o.has("count") && !o.get("count").isJsonNull()) {
				try {
					count = o.get("count").getAsLong();
				} catch (Exception ignored) {
				}
			}
			if (count <= 0) {
				continue;
			}
			String shownName = (localizedName != null && !localizedName.isBlank()) ? localizedName : nameResolver.apply(rawName);
			out.merge(shownName, (double) count, Double::sum);
		}
		return out;
	}

	static String csvEscape(String s) {
		if (s == null) {
			return "";
		}
		if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}

	private static final DateTimeFormatter PROSPECTOR_CSV_TIMESTAMP = DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.US);

	private void appendProspectorCsv(ProspectedAsteroidEvent event, Map<String, Double> currentInventory) {
		Path edoDir = Paths.get(System.getProperty("user.home", ""), "EDO");
		try {
			Files.createDirectories(edoDir);
		} catch (Exception e) {
			return;
		}
		Path csvPath = edoDir.resolve("prospector_log.csv");
		try {
			boolean newFile = !Files.exists(csvPath);
			Instant ts = event.getTimestamp();
			String timestampStr = (ts == null) ? "" : ts.atZone(ZoneId.systemDefault()).format(PROSPECTOR_CSV_TIMESTAMP);
			String email = OverlayPreferences.getProspectorEmail();
			if (newFile) {
				String header = "timestamp,material,percent,before amount,after amount,difference,email address";
				Files.writeString(csvPath, header + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			for (MaterialProportion mp : event.getMaterials()) {
				if (mp == null || mp.getName() == null) {
					continue;
				}
				String material = toUiName(mp.getName());
				double pct = mp.getProportion();
				double beforeTons = lastInventoryTonsAtProspector.getOrDefault(material, 0.0);
				double afterTons = currentInventory.getOrDefault(material, 0.0);
				double difference = afterTons - beforeTons;
				String line = csvEscape(timestampStr) + ","
					+ csvEscape(material) + ","
					+ String.format(Locale.US, "%.2f", pct) + ","
					+ String.format(Locale.US, "%.2f", beforeTons) + ","
					+ String.format(Locale.US, "%.2f", afterTons) + ","
					+ String.format(Locale.US, "%.2f", difference) + ","
					+ csvEscape(email);
				Files.writeString(csvPath, line + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
		} catch (Exception e) {
			// don't break UI on log failure
		}
	}

	private Set<Integer> computeChangedInventoryModelRows(List<Row> newRows) {
		if (newRows == null) {
			return Set.of();
		}

		Set<String> changedNames = new HashSet<>();
		Map<String, Long> now = new HashMap<>();

		for (Row r : newRows) {
			if (r == null) {
				continue;
			}
			String name = r.getName();
			if (name == null) {
				continue;
			}
			long tons = Math.round(r.getExpectedTons());
			now.put(name, tons);

			Long old = lastCargoTonsByName.get(name);
			if (old == null || old.longValue() != tons) {
				changedNames.add(name);
			}
		}

		boolean removedSomething = false;
		for (String oldName : lastCargoTonsByName.keySet()) {
			if (!now.containsKey(oldName)) {
				removedSomething = true;
				break;
			}
		}

		lastCargoTonsByName.clear();
		lastCargoTonsByName.putAll(now);

		Set<Integer> changedModelRows = new HashSet<>();
		for (int i = 0; i < newRows.size(); i++) {
			Row r = newRows.get(i);
			if (r == null) {
				continue;
			}
			if (changedNames.contains(r.getName())) {
				changedModelRows.add(i);
			}
		}

		// The Total row changes whenever any cargo line changes.
		if (!changedModelRows.isEmpty() || removedSomething) {
			changedModelRows.add(newRows.size());
		}

		return changedModelRows;
	}

    public void applyOverlayBackground(Color bg) {
        if (bg == null) {
            bg = EdoUi.Internal.TRANSPARENT;
        }

        boolean opaque = bg.getAlpha() >= 255;
        setOpaque(opaque);
        setBackground(bg);

        // Keep tables non-opaque so the panel background shows through.
        table.setOpaque(false);
        cargoTable.setOpaque(false);

        repaint();
    }

    public void applyOverlayTransparency(boolean transparent) {
        // Legacy wrapper
        Color bg = OverlayPreferences.buildOverlayBackgroundColor(
                OverlayPreferences.getOverlayBackgroundColor(),
                transparent ? 100 : OverlayPreferences.getOverlayTransparencyPercent()
        );
        applyOverlayBackground(bg);
    }

    private static final class ProspectorAnnouncement {
    	private final boolean single;
    	private final String material;
    	private final int pct;

    	private final String listText;
    	private final int minPct;
    	private final int maxPct;

    	private final String sig;

    	private ProspectorAnnouncement(String material, int pct, String sig) {
    		this.single = true;
    		this.material = material;
    		this.pct = pct;

    		this.listText = null;
    		this.minPct = 0;
    		this.maxPct = 0;

    		this.sig = sig;
    	}

    	private ProspectorAnnouncement(String listText, int minPct, int maxPct, String sig) {
    		this.single = false;
    		this.material = null;
    		this.pct = 0;

    		this.listText = listText;
    		this.minPct = minPct;
    		this.maxPct = maxPct;

    		this.sig = sig;
    	}
    }



    private ProspectorAnnouncement buildProspectorAnnouncement(ProspectedAsteroidEvent event, List<Row> rows) {
    	if (event == null || rows == null || rows.isEmpty()) {
    		prospectorHighlightNames.clear();
    		return null;
    	}

    	double minProp = OverlayPreferences.getProspectorMinProportionPercent();
    	if (minProp <= 0.0) {
    		prospectorHighlightNames.clear();
    		return null;
    	}

    	String materialsCsv = OverlayPreferences.getProspectorMaterialsCsv();
    	Set<String> allowed = new HashSet<>();
    	if (materialsCsv != null && !materialsCsv.isBlank()) {
    		for (String s : materialsCsv.split(",")) {
    			if (s == null) {
    				continue;
    			}
    			String norm = GalacticAveragePrices.normalizeMaterialKey(s.trim());
    			if (norm != null && !norm.isBlank()) {
    				allowed.add(norm);
    			}
    		}
    	}

    	double minEstValueForAnnounce = OverlayPreferences.getProspectorMinAvgValueCrPerTon();

    	
    	

		prospectorHighlightNames.clear();
    	Row best = null;
    	List<Row> matches = new ArrayList<>();
    	double minPct = Double.POSITIVE_INFINITY;
    	double maxPct = Double.NEGATIVE_INFINITY;

    	for (Row r : rows) {
    		if (r == null || r.isSummary() || r.isCore()) {
    			continue;
    		}

    		double pct = r.getProportionPercent();
			if (Double.isNaN(pct)) {
				continue;
			}

			boolean pctOk = !Double.isNaN(pct) && pct >= minProp;
boolean csvOk = false;
    		if (!allowed.isEmpty()) {
    			String norm = GalacticAveragePrices.normalizeMaterialKey(r.getName());
    			if (norm != null && !norm.isBlank()) {
    				for (String a : allowed) {
    					String needle = GalacticAveragePrices.normalizeMaterialKey(a);
    					if (needle != null && !needle.isBlank() && norm.contains(needle)) {
    						csvOk = true;
    						break;
    					}
    				}
    			}
    		}

    		boolean valueOk = false;
    		if (minEstValueForAnnounce > 0) {
    			valueOk = r.getEstimatedValue() >= minEstValueForAnnounce;
    		}

    		// RULE: include if (pctOk && csvOk) OR valueOk
			if (!((pctOk && csvOk) || valueOk)) {
				continue;
			}
matches.add(r);
			prospectorHighlightNames.add(r.getName());

    		if (pct < minPct) {
    			minPct = pct;
    		}
    		if (pct > maxPct) {
    			maxPct = pct;
    		}

    		if (best == null || r.getEstimatedValue() > best.getEstimatedValue()) {
    			best = r;
    		}
    	}

    	if (matches.isEmpty()) {
			prospectorHighlightNames.clear();
			return null;
		}
matches.sort(Comparator.comparingDouble(Row::getProportionPercent).reversed());
    	List<String> names = new ArrayList<>();
    	for (Row r : matches) {
    		names.add(r.getName());
    	}

    	int minRounded = (int) Math.round(minPct);
    	int maxRounded = (int) Math.round(maxPct);

    	String ts = event.getTimestamp().toString();
    	if (ts.length() > 19) {
    		ts = ts.substring(0, 19);
    	}

    	// SINGLE vs LIST
    	if (names.size() == 1) {
    		Row only = matches.get(0);
    		int pctRounded = (int) Math.round(only.getProportionPercent());

    		String sig = ts + "|" + only.getName() + "|" + pctRounded;
    		if (sig.equals(lastProspectorAnnouncementSig)) {
    			return null;
    		}

    		return new ProspectorAnnouncement(only.getName(), pctRounded, sig);
    	}

    	String sig = ts + "|" + String.join(",", names) + "|" + minRounded + "|" + maxRounded;
    	if (sig.equals(lastProspectorAnnouncementSig)) {
    		return null;
    	}

    	String listText = joinWithAnd(names);
    	return new ProspectorAnnouncement(listText, minRounded, maxRounded, sig);
    }

    
	public void updateFromProspector(ProspectedAsteroidEvent event) {
		if (event == null) {
			model.setRows(List.of());
			headerLabel.setText("Mining (latest prospector)");
			return;
		}

		// Snapshot current cargo so we can log inventory deltas since last ProspectorEvent (CargoMonitor already polls)
		CargoMonitor.Snapshot cargoSnap = CargoMonitor.getInstance().getSnapshot();
		Map<String, Double> currentInventory = buildInventoryTonsFromCargo(cargoSnap != null ? cargoSnap.getCargoJson() : null);

		// If we have a previous snapshot, append one CSV row per prospected material: commodity, percent, increase_tons
		if (!lastInventoryTonsAtProspector.isEmpty()) {
			appendProspectorCsv(event, currentInventory);
		}
		lastInventoryTonsAtProspector = new HashMap<>(currentInventory);

		String motherlode = event.getMotherlodeMaterial();
		String content = event.getContent();

		List<Row> rows = new ArrayList<>();

		double totalTons = estimateTotalTons(content);
		for (MaterialProportion mp : event.getMaterials()) {
			if (mp == null || mp.getName() == null) {
				continue;
			}

			String rawName = mp.getName();
			String shownName = toUiName(rawName);

			int avg = lookupAvgSell(rawName, shownName);
			double tons = (mp.getProportion() / 100.0) * totalTons;
			double value = tons * avg;

			rows.add(new Row(shownName, mp.getProportion(), avg, tons, value));
		}

		if (motherlode != null && !motherlode.isBlank()) {
			String shownName = toUiName(motherlode);

			int avg = lookupAvgSell(motherlode, shownName);
			double tons = OverlayPreferences.getMiningEstimateTonsCore();
			double value = tons * avg;

			rows.add(new Row(shownName + " (Core)", Double.NaN, avg, tons, value, true));
		}

		ProspectorAnnouncement ann = buildProspectorAnnouncement(event, rows);

		rows.sort(Comparator
				.comparing(Row::isCore).reversed()
				.thenComparing(Comparator.comparing((Row r) -> isHighlightedProspectorRow(r)).reversed())
				.thenComparing(Comparator.comparingDouble(Row::getEstimatedValue).reversed())
				.thenComparing(Row::getName, String.CASE_INSENSITIVE_ORDER));

		model.setRows(rows);
		prospectorScan.startProspectorScan(prospectorLayer);

			if (!OverlayPreferences.isSpeechEnabled()) {
				return;
			}
			
			if (ann != null) {
				lastProspectorAnnouncementSig = ann.sig;

				if (ann.single) {
					tts.speakf("Prospector found {material} at {n} percent.", ann.material, ann.pct);
				} else {
					tts.speakf("Prospector found {list} from {min} to {max} percent.", ann.listText, ann.minPct, ann.maxPct);
				}
			}



				String hdr = "Mining (" + (content == null ? "" : content) + ")";
		if (motherlode != null && !motherlode.isBlank()) {
			hdr += " - Motherlode: " + motherlode;
		}
		headerLabel.setText(hdr);

	}



	/**
	 * Use the INARA CSV display name if present; fall back to a friendly formatting of the journal token.
	 * This is what fixes "Crystals" (and lots of other tokens) showing inconsistently.
	 */    private String toUiName(String s) {
		 if (s == null || s.isBlank()) {
			 return "";
		 }

		 String norm = GalacticAveragePrices.normalizeMaterialKey(s);
		 if ("lowtemperaturediamonds".equals(norm)) {
			 return "Low Temperature Diamonds";
		 }
		 if ("opal".equals(norm)) {
			 return "Void Opal";
		 }

		 String fromCsv = prices.getDisplayName(s);
		 if (fromCsv != null && !fromCsv.isBlank()) {
			 return fromCsv;
		 }

		 // Handle snake_case / kebab-case first
		 String out = s.replace('_', ' ').replace('-', ' ').trim();

		 // Friendly fallback (handles $..._Name; too)
		 if (out.startsWith("$")) {
			 out = out.substring(1);
		 }
		 out = out.replace("_name", "");
		 out = out.replace("_Name", "");
		 out = out.replace(";", "");
		 out = out.trim();

		 // Insert spaces for tokens like "LowTemperatureDiamonds"
		 out = out.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
		 out = out.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ");
		 out = out.trim();

		 if (out.isEmpty()) {
			 return "";
		 }

		 // Title-case
		 String[] parts = out.split("\\s+");
		 StringBuilder sb = new StringBuilder();
		 for (String p : parts) {
			 if (p.isBlank()) {
				 continue;
			 }
			 if (sb.length() > 0) {
				 sb.append(' ');
			 }
			 if (p.length() == 1) {
				 sb.append(p.toUpperCase());
			 } else {
				 sb.append(Character.toUpperCase(p.charAt(0)));
				 sb.append(p.substring(1).toLowerCase());
			 }
		 }
		 return sb.toString();
	 }
	 private static String joinWithAnd(List<String> items) {
		    if (items == null || items.isEmpty()) {
		        return "";
		    }
		    if (items.size() == 1) {
		        return items.get(0);
		    }
		    if (items.size() == 2) {
		        return items.get(0) + " and " + items.get(1);
		    }

		    StringBuilder sb = new StringBuilder();
		    for (int i = 0; i < items.size(); i++) {
		        if (i > 0) {
		            if (i == items.size() - 1) {
		                sb.append(", and ");
		            } else {
		                sb.append(", ");
		            }
		        }
		        sb.append(items.get(i));
		    }
		    return sb.toString();
		}


	 /**
	  * Price lookup should use the journal name (because GalacticAveragePrices normalizes keys already),
	  * with one alias to cover the "opal" token.
	  */

	 private static String splitCamelCase(String s) {
		 if (s == null || s.isBlank()) {
			 return "";
		 }
		 // Insert spaces for tokens like "LowTemperatureDiamonds".
		 String out = s.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
		 out = out.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ");
		 return out.trim();
	 }    

	 private int lookupAvgSell(String journalName, String uiName) {
		return matcher.lookupAvgSell(journalName, uiName);
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

	 private static final class Row {
		 private final String name;
		 private final double proportionPercent;
		 private final int avgSell;
		 private final double expectedTons;
		 private final double estimatedValue;
		 private final boolean isCore;
		 private final boolean isSummary;

		 Row(String name, int avgSell, double expectedTons, double estimatedValue) {
			 this(name, Double.NaN, avgSell, expectedTons, estimatedValue, false, false);
		 }

		 Row(String name, int avgSell, double expectedTons, double estimatedValue, boolean isCore) {
			 this(name, Double.NaN, avgSell, expectedTons, estimatedValue, isCore, false);
		 }

		 Row(String name, double proportionPercent, int avgSell, double expectedTons, double estimatedValue) {
			 this(name, proportionPercent, avgSell, expectedTons, estimatedValue, false, false);
		 }

		 Row(String name, double proportionPercent, int avgSell, double expectedTons, double estimatedValue, boolean isCore) {
			 this(name, proportionPercent, avgSell, expectedTons, estimatedValue, isCore, false);
		 }

		 Row(String name, double proportionPercent, int avgSell, double expectedTons, double estimatedValue, boolean isCore, boolean isSummary) {
			 this.name = name;
			 this.proportionPercent = proportionPercent;
			 this.avgSell = avgSell;
			 this.expectedTons = expectedTons;
			 this.estimatedValue = estimatedValue;
			 this.isCore = isCore;
			 this.isSummary = isSummary;
		 }

String getName() {
			 return name;
		 }

		 double getProportionPercent() {
			 return proportionPercent;
		 }

		 int getAvgSell() {
			 return avgSell;
		 }

		 double getExpectedTons() {
			 return expectedTons;
		 }

		 double getEstimatedValue() {
			 return estimatedValue;
		 }

		 boolean isCore() {
			 return isCore;
		 }

		 boolean isSummary() {
			 return isSummary;
		 }
	 }

	 private static final class MiningTableModel extends AbstractTableModel {

		 private final String[] cols;

		 private final NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
		 private final NumberFormat tonsFmt = NumberFormat.getNumberInstance(Locale.US);
		 private final NumberFormat pctFmt = NumberFormat.getNumberInstance(Locale.US);

		 private List<Row> rows = List.of();

		 MiningTableModel(String tonsLabel) {
			 String tl = (tonsLabel == null || tonsLabel.isBlank()) ? "Est. Tons" : tonsLabel;

			 cols = new String[] {
					 "Material",
					 "Percent",
					 "Avg Cr/t",
					 tl,
					 "Est. Value"
			 };

			 boolean estimated = tl.toLowerCase(Locale.US).contains("est");
			 tonsFmt.setMaximumFractionDigits(estimated ? 1 : 0);
			 tonsFmt.setMinimumFractionDigits(0);
		 
			 pctFmt.setMaximumFractionDigits(1);
			 pctFmt.setMinimumFractionDigits(0);
		 }

		 void setRows(List<Row> newRows) {
			 rows = (newRows == null) ? List.of() : List.copyOf(newRows);
			 fireTableDataChanged();
		 }

		 Row getRow(int modelRow) {
			 if (modelRow < 0 || modelRow >= rows.size()) {
				 return null;
			 }
			 return rows.get(modelRow);
		 }

		 @Override
		 public int getRowCount() {
			 return rows.size();
		 }

		 @Override
		 public int getColumnCount() {
			 return cols.length;
		 }

		 @Override
		 public String getColumnName(int column) {
			 return cols[column];
		 }

		 @Override
		 public Object getValueAt(int rowIndex, int columnIndex) {
			 Row r = rows.get(rowIndex);
			 if (r.isSummary()) {
				 switch (columnIndex) {
				 case 0:
					 return r.getName();
				 case 1:
					 return "";
				 case 2:
					 return "";
				 case 3:
					 return tonsFmt.format(r.getExpectedTons());
				 case 4:
					 return intFmt.format(Math.round(r.getEstimatedValue()));
				 default:
					 return "";
				 }
			}

			 switch (columnIndex) {
			 case 0:
				 return r.getName();
			 case 1:
				 if (Double.isNaN(r.getProportionPercent()) || r.getProportionPercent() <= 0.0) {
					 return "";
				 }
				 return pctFmt.format(r.getProportionPercent());
			 case 2:
				 return r.getAvgSell() <= 0 ? "" : intFmt.format(r.getAvgSell());
			 case 3:
				 return tonsFmt.format(r.getExpectedTons());
			 case 4:
				 return r.getAvgSell() <= 0 ? "" : intFmt.format(Math.round(r.getEstimatedValue()));
			 default:
				 return "";
			 }
		 }

		 @Override
		 public Class<?> getColumnClass(int columnIndex) {
			 return String.class;
		 }
	 }



	 private static void configureOverlayScroller(JScrollPane sp) {
		 if (sp == null) {
			 return;
		 }

		 sp.setOpaque(false);
		 sp.setBackground(EdoUi.Internal.TRANSPARENT);
		 sp.setBorder(BorderFactory.createEmptyBorder());
		 sp.setViewportBorder(BorderFactory.createEmptyBorder());

		 if (sp.getViewport() != null) {
			 sp.getViewport().setOpaque(false);
			 sp.getViewport().setBackground(EdoUi.Internal.TRANSPARENT);
		 }

		 if (sp.getColumnHeader() != null) {
			 sp.getColumnHeader().setOpaque(false);
			 sp.getColumnHeader().setBackground(EdoUi.Internal.TRANSPARENT);
			 sp.getColumnHeader().setBorder(BorderFactory.createEmptyBorder());
		 }

		 if (sp.getHorizontalScrollBar() != null) {
			 sp.getHorizontalScrollBar().setOpaque(false);
			 sp.getHorizontalScrollBar().setBackground(EdoUi.Internal.TRANSPARENT);
		 }

		 if (sp.getVerticalScrollBar() != null) {
			 sp.getVerticalScrollBar().setOpaque(false);
			 sp.getVerticalScrollBar().setBackground(EdoUi.Internal.TRANSPARENT);
		 }

		 JPanel corner = new JPanel();
		 corner.setOpaque(false);
		 corner.setBackground(EdoUi.Internal.TRANSPARENT);

		 sp.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
		 sp.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner);
		 sp.setCorner(JScrollPane.UPPER_LEFT_CORNER, corner);
		 sp.setCorner(JScrollPane.LOWER_LEFT_CORNER, corner);
	 }

	 	private static final class TableScanState {
		private final JTable table;
		private final Map<Integer, Float> revealAlphaByModelRow = new HashMap<>();
		private final Map<Integer, Float> flareAlphaByModelRow = new HashMap<>();

		private int scanY = Integer.MIN_VALUE;
		private Timer scanTimer;

		private int lastRowCount = 0;
		private int scanEndY = 0;

		private TableScanState(JTable table) {
			this.table = table;
		}

		private float getRevealAlpha(int modelRow) {
			return revealAlphaByModelRow.getOrDefault(modelRow, 1.0f);
		}

		private float getFlareAlpha(int modelRow) {
			return flareAlphaByModelRow.getOrDefault(modelRow, 0.0f);
		}

		private boolean isScanning() {
			return scanTimer != null && scanTimer.isRunning();
		}

		private int getScanY() {
			return scanY;
		}

		private void startProspectorScan(JLayer<JTable> layer) {
			initRevealForAllRows(0.0f);
			flareAlphaByModelRow.clear();
			startScan(layer, true, null);
		}

		private void startInventoryScan(JLayer<JTable> layer, Set<Integer> flareModelRows) {
			initRevealForAllRows(1.0f);
			flareAlphaByModelRow.clear();
			startScan(layer, false, flareModelRows);
		}

		private void initRevealForAllRows(float alpha) {
			revealAlphaByModelRow.clear();
			int rc = table.getRowCount();
			for (int viewRow = 0; viewRow < rc; viewRow++) {
				int modelRow = viewRow;
				if (table.getRowSorter() != null) {
					modelRow = table.convertRowIndexToModel(viewRow);
				}
				revealAlphaByModelRow.put(modelRow, alpha);
			}
		}

		private void startScan(JLayer<JTable> layer, boolean revealOnCross, Set<Integer> flareOnlyModelRows) {
			if (scanTimer != null && scanTimer.isRunning()) {
				scanTimer.stop();
			}

			scanY = 0;

			int currentRowCount = table.getRowCount();
			int maxRows = Math.max(lastRowCount, currentRowCount);
			int rowHeight = table.getRowHeight();
			scanEndY = maxRows * rowHeight;
			lastRowCount = currentRowCount;

			final Set<Integer> flaredAlready = new HashSet<>();

			scanTimer = new Timer(16, e -> {
				scanY += 10;

				int y = 0;
				for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
					int h = table.getRowHeight(viewRow);
					int mid = y + (h / 2);

					if (scanY >= mid) {
						int modelRow = viewRow;
						if (table.getRowSorter() != null) {
							modelRow = table.convertRowIndexToModel(viewRow);
						}

						if (revealOnCross) {
							Float a = revealAlphaByModelRow.get(modelRow);
							if (a != null && a.floatValue() < 1.0f) {
								revealAlphaByModelRow.put(modelRow, 1.0f);
								triggerFlare(modelRow);
							}
						} else {
							if (flareOnlyModelRows != null && flareOnlyModelRows.contains(modelRow) && !flaredAlready.contains(modelRow)) {
								flaredAlready.add(modelRow);
								triggerFlare(modelRow);
							}
						}
					}

					y += h;
				}

				if (layer != null) {
					layer.repaint();
				}
				table.repaint();

				if (scanY > scanEndY + 10) {
					((Timer)e.getSource()).stop();
					return;
				}
			});

			scanTimer.start();
		}

		private void triggerFlare(int modelRow) {
			flareAlphaByModelRow.put(modelRow, 1.0f);

			Timer decay = new Timer(16, null);
			decay.addActionListener(ev -> {
				float v = flareAlphaByModelRow.getOrDefault(modelRow, 0.0f);
				v -= 0.08f;
				if (v <= 0.0f) {
					flareAlphaByModelRow.remove(modelRow);
					((Timer)ev.getSource()).stop();
				} else {
					flareAlphaByModelRow.put(modelRow, v);
				}
				table.repaint();
			});

			decay.start();
		}
	}

	private static final class ScanLayerUi extends LayerUI<JTable> {
		private static final long serialVersionUID = 1L;
		private final TableScanState scan;

		private ScanLayerUi(TableScanState scan) {
			this.scan = scan;
		}

		@Override
		public void paint(Graphics g, JComponent c) {
			super.paint(g, c);

			if (!scan.isScanning()) {
				return;
			}

			Graphics2D g2 = (Graphics2D)g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				int w = c.getWidth();
				int y = scan.getScanY();

				// Soft glow band
				g2.setColor(EdoUi.Internal.MAIN_TEXT_ALPHA_40);
				g2.fillRect(0, y - 6, w, 12);

				// Bright core scan line
				g2.setColor(EdoUi.Internal.MAIN_TEXT_ALPHA_180);
				g2.fillRect(0, y - 1, w, 3);

			} finally {
				g2.dispose();
			}
		}
	}

private static final class TransparentTableHeader extends JTableHeader {
		 private static final long serialVersionUID = 1L;

		 TransparentTableHeader(TableColumnModel cm) {
			 super(cm);
			 setOpaque(true);
			 setBackground(EdoUi.User.BACKGROUND);
		 }

		 @Override
		 protected void paintComponent(Graphics g) {
			 // Fill with theme background (header does not automatically inherit panel background).
			 Graphics2D g2 = (Graphics2D) g.create();
			 g2.setComposite(AlphaComposite.SrcOver);
			 g2.setColor(EdoUi.User.BACKGROUND);
			 g2.fillRect(0, 0, getWidth(), getHeight());
			 g2.dispose();

			 setOpaque(true);
			 setBackground(EdoUi.User.BACKGROUND);

			 super.paintComponent(g);
		 }
	 }

	 private static final class HeaderRenderer extends DefaultTableCellRenderer {
		 private static final long serialVersionUID = 1L;

		 @Override
		 public Component getTableCellRendererComponent(JTable table,
				 Object value,
				 boolean isSelected,
				 boolean hasFocus,
				 int row,
				 int column) {
			 JLabel label = (JLabel)super.getTableCellRendererComponent(table,
					 value,
					 false,
					 false,
					 row,
					 column);

			 label.setOpaque(true);
			 label.setBackground(EdoUi.User.BACKGROUND);
			 label.setForeground(EdoUi.Internal.tableHeaderForeground());
			 label.setFont(label.getFont().deriveFont(Font.BOLD));
			 label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
			 label.setBorder(new CompoundBorder(
					 new MatteBorder(2, 0, 0, 0, EdoUi.Internal.tableHeaderTopBorder()),
					 new EmptyBorder(0, 4, 0, 4)
			 ));
			 return label;
		 }

		 @Override
		 protected void paintComponent(Graphics g) {
			 Graphics2D g2 = (Graphics2D) g.create();
			 g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					 RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

			 super.paintComponent(g2);

			 g2.setColor(EdoUi.ED_ORANGE_TRANS);
			 int y = getHeight() - 1;
			 g2.drawLine(0, y, getWidth(), y);

			 g2.dispose();
		 }
	 }
}