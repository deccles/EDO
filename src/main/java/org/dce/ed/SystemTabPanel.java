package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.dce.ed.cache.CachedSystem;
import org.dce.ed.cache.SystemCache;
import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.edsm.EdsmClient;
import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.state.BodyInfo;
import org.dce.ed.state.SystemEventProcessor;
import org.dce.ed.state.SystemState;

/**
 * System tab – now a *pure UI* renderer.
 *
 * All parsing, prediction, and system-state logic lives in:
 *   SystemState
 *   SystemEventProcessor
 *   SystemCache
 */
public class SystemTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color ED_ORANGE = new Color(255, 140, 0);
    // NEW: semi-transparent orange for separators, similar to RouteTabPanel
    private static final Color ED_ORANGE_TRANS = new Color(255, 140, 0, 64);
    // NEW: shared ED font (similar to Route tab)
    private static final Font ED_FONT = new Font("Segoe UI", Font.PLAIN, 17);

    private final JTable table;
    private final JTextField headerLabel;
    private final SystemBodiesTableModel tableModel;

    private final SystemState state = new SystemState();
    private final SystemEventProcessor processor = new SystemEventProcessor(state);

    private final EdsmClient edsmClient = new EdsmClient();

	private JLabel headerSummaryLabel;
    
    public SystemTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        // Header label
        headerLabel = new JTextField("Waiting for system data…");
        headerLabel.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = headerLabel.getText();
                    if (text == null) {
                        return;
                    }
//PLOEA EURL mn-j d9-22
//PLOEA EURL ZP-T C18-0
                    
                    System.out.println("User hit enter for system: '" + text + "'");

                    // User is specifying by name; let loadSystem resolve address
                    state.setSystemName(text);
                    state.setSystemAddress(0L);

                    loadSystem(text, 0L);
                }
            }
        });
        headerLabel.setForeground(ED_ORANGE);
        headerLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        headerLabel.setOpaque(false);
        headerLabel.setFont(ED_FONT.deriveFont(Font.BOLD));

        headerSummaryLabel = new JLabel();
        headerSummaryLabel.setForeground(ED_ORANGE);
        headerSummaryLabel.setFont(ED_FONT.deriveFont(Font.BOLD));
        headerSummaryLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        headerSummaryLabel.setOpaque(false);
        
        // Table setup
        tableModel = new SystemBodiesTableModel();
        table = new SystemBodiesTable(tableModel);
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(500, 300));
        // NEW: apply ED font to table cells
        table.setFont(ED_FONT);
        table.setRowHeight(24);

        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setForeground(ED_ORANGE);
        header.setBackground(Color.BLACK);
        header.setFont(ED_FONT.deriveFont(Font.BOLD));

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, false, false, row, column);

                label.setOpaque(true);
                label.setBackground(Color.BLACK);
                label.setForeground(ED_ORANGE);
                label.setFont(ED_FONT.deriveFont(Font.BOLD));
                label.setHorizontalAlignment(LEFT);
                label.setBorder(new EmptyBorder(0, 4, 0, 4));

                return label;
            }
        });

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            {
                setOpaque(false);
                setForeground(ED_ORANGE);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Biological detail rows (bioText/bioValue) should be gray when not selected
                Row r = tableModel.getRowAt(row);
                boolean isBioRow = (r != null && r.detail && (r.bioText != null || r.bioValue != null));

                if (isSelected) {
                    c.setForeground(Color.BLACK);
                } else if (isBioRow) {
                	if (r.isObservedGenusHeader()) {
                		c.setForeground(Color.green);
                	}else 
                    c.setForeground(new Color(180, 180, 180)); // gray for biologicals
                } else {
                    c.setForeground(ED_ORANGE);
                }

                return c;
            }
        };

        table.setDefaultRenderer(Object.class, cellRenderer);


        DefaultTableCellRenderer valueRightRenderer = new DefaultTableCellRenderer() {
            {
                setOpaque(false);
                setForeground(ED_ORANGE);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Component c = super.getTableCellRendererComponent(table,
                                                                  value,
                                                                  isSelected,
                                                                  hasFocus,
                                                                  row,
                                                                  column);

                setHorizontalAlignment(SwingConstants.RIGHT);

                // Biological detail rows should be gray in the Value column too
                Row r = tableModel.getRowAt(row);
                boolean isBioRow = (r != null && r.detail && (r.bioText != null || r.bioValue != null));

                if (isSelected) {
                    c.setForeground(Color.BLACK);
                } else if (isBioRow) {
                    c.setForeground(new Color(180, 180, 180));
                } else {
                    c.setForeground(ED_ORANGE);
                }

                return c;
            }
        };

        // Column index 4 is "Value"
        table.getColumnModel().getColumn(4).setCellRenderer(valueRightRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JViewport headerViewport = scrollPane.getColumnHeader();
        if (headerViewport != null) {
            headerViewport.setOpaque(false);
            headerViewport.setBackground(new Color(0, 0, 0, 0));
        }

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        setBorder(new EmptyBorder(4, 4, 4, 4));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(headerLabel, BorderLayout.WEST);
        headerPanel.add(headerSummaryLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Column widths preserved
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(40);
        columns.getColumn(1).setPreferredWidth(60);
        columns.getColumn(2).setPreferredWidth(220);
        columns.getColumn(3).setPreferredWidth(70);
        columns.getColumn(4).setPreferredWidth(20);
        columns.getColumn(4).setMinWidth(60);
        columns.getColumn(4).setMaxWidth(80);
        columns.getColumn(4).setResizable(false);
        
        
        columns.getColumn(5).setPreferredWidth(30);
        columns.getColumn(6).setPreferredWidth(80);

        refreshFromCache();
    }

    // ---------------------------------------------------------------------
    // Event forwarding
    // ---------------------------------------------------------------------

    public void handleLogEvent(EliteLogEvent event) {
        if (event != null) {
            processor.handleEvent(event);
            
            if (event instanceof FsdJumpEvent) {
                FsdJumpEvent e = (FsdJumpEvent)event;
                loadSystem(e.getStarSystem(), e.getSystemAddress());
                rebuildTable();
            }

            rebuildTable();
            persistIfPossible();
        }
    }

    // ---------------------------------------------------------------------
    // Cache loading at startup
    // ---------------------------------------------------------------------

    public void refreshFromCache() {
        try {
            EliteJournalReader reader = new EliteJournalReader();

            String systemName = null;
            long systemAddress = 0L;

            List<EliteLogEvent> events = reader.readEventsFromLastNJournalFiles(3);

            for (EliteLogEvent event : events) {
                if (event instanceof LocationEvent) {
                    LocationEvent e = (LocationEvent) event;
                    systemName = e.getStarSystem();
                    systemAddress = e.getSystemAddress();
                } else if (event instanceof FsdJumpEvent) {
                    FsdJumpEvent e = (FsdJumpEvent) event;
                    systemName = e.getStarSystem();
                    systemAddress = e.getSystemAddress();
                }
            }

            loadSystem(systemName, systemAddress);
            rebuildTable();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadSystem(String systemName, long systemAddress) {
        SystemCache cache = SystemCache.getInstance();
        CachedSystem cs = cache.get(systemAddress, systemName);

        // Start from a clean state for this system.
        state.setSystemName(systemName);
        state.setSystemAddress(systemAddress);
        state.resetBodies();
        state.setTotalBodies(null);
        state.setNonBodyCount(null);
        state.setFssProgress(null);
        state.setAllBodiesFound(null);

        // 1) Load from cache if we have it
        if (cs != null) {
            cache.loadInto(state, cs);
        }

        // 2) Always try to enrich with EDSM via a single bodies call
        try {
            BodiesResponse edsmBodies = edsmClient.showBodies(systemName);
            if (edsmBodies != null) {
                edsmClient.mergeBodiesFromEdsm(state, edsmBodies);
            }
        } catch (Exception ex) {
            // EDSM is best-effort; overlay should still work from cache/logs.
            ex.printStackTrace();
        }

        // 3) Refresh UI and persist merged result
        rebuildTable();
        persistIfPossible();
    }


    // ---------------------------------------------------------------------
    // UI rebuild from SystemState
    // ---------------------------------------------------------------------

    private void rebuildTable() {
        updateHeaderLabel();

        List<BodyInfo> sorted = new ArrayList<>(state.getBodies().values());
        sorted.sort(Comparator.comparingDouble(b -> Double.isNaN(b.getDistanceLs())
                ? Double.MAX_VALUE
                : b.getDistanceLs()));

        List<Row> rows = new ArrayList<>();

        for (BodyInfo b : sorted) {
            rows.add(Row.body(b));

            if (!b.hasBio()) {
                continue;
            }

            // 1) Start from whatever predictions we already have
            List<ExobiologyData.BioCandidate> preds = b.getPredictions();

            // If there are no predictions yet, try a one-shot calculation here
            if (preds == null || preds.isEmpty()) {
                ExobiologyData.BodyAttributes attrs = b.buildBodyAttributes();
                if (attrs != null) {
                    List<ExobiologyData.BioCandidate> base = ExobiologyData.predict(attrs);
                    if (base != null && !base.isEmpty()) {
                        // If we know which genera are present, narrow to those
                        Set<String> genusPrefixes = b.getObservedGenusPrefixes();
                        if (genusPrefixes != null && !genusPrefixes.isEmpty()) {
                            Set<String> lower = new java.util.HashSet<>();
                            for (String g : genusPrefixes) {
                                if (g != null && !g.isEmpty()) {
                                    lower.add(g.toLowerCase(Locale.ROOT));
                                }
                            }

                            List<ExobiologyData.BioCandidate> filtered = new ArrayList<>();
                            for (ExobiologyData.BioCandidate cand : base) {
                                String nameLower = cand.getDisplayName()
                                                       .toLowerCase(Locale.ROOT);
                                boolean matches = false;
                                for (String prefix : lower) {
                                    if (nameLower.startsWith(prefix + " ")
                                            || nameLower.equals(prefix)) {
                                        matches = true;
                                        break;
                                    }
                                }
                                if (matches) {
                                    filtered.add(cand);
                                }
                            }

                            preds = filtered.isEmpty() ? base : filtered;
                        } else {
                            preds = base;
                        }
                    }
                }
            }

            Set<String> genusPrefixes = b.getObservedGenusPrefixes();
            Set<String> observedNamesRaw = b.getObservedBioDisplayNames();

            Set<String> observedGenusLower = new java.util.HashSet<>();
            if (genusPrefixes != null) {
                for (String gp : genusPrefixes) {
                    if (gp != null && !gp.isEmpty()) {
                        observedGenusLower.add(gp.toLowerCase(Locale.ROOT));
                    }
                }
            }

            boolean hasGenusPrefixes = genusPrefixes != null && !genusPrefixes.isEmpty();
            boolean hasObservedNames = observedNamesRaw != null && !observedNamesRaw.isEmpty();
            boolean hasPreds = preds != null && !preds.isEmpty();

            // If literally nothing but "hasBio", show a generic message
            if (!hasGenusPrefixes && !hasObservedNames && !hasPreds) {
                rows.add(Row.bio(b.getBodyId(),
                        "Biological signals detected",
                        ""));
                continue;
            }

            //
            // CASE A: Predictions only, no genus info from scan yet.
            //   -> collapse by genus: "Genus (n)" with max value.
            //
            if (!hasGenusPrefixes && !hasObservedNames) {

                class BioRowData {
                    final String name;
                    final Long cr;

                    BioRowData(String name, Long cr) {
                        this.name = name;
                        this.cr = cr;
                    }
                }

                List<BioRowData> bioRows = new ArrayList<>();

                if (preds != null) {
                    for (ExobiologyData.BioCandidate cand : preds) {
                        String name = canonicalBioName(cand.getDisplayName());
                        Long cr = cand.getEstimatedPayout(true);
                        bioRows.add(new BioRowData(name, cr));
                    }
                }

                if (bioRows.isEmpty()) {
                    rows.add(Row.bio(b.getBodyId(),
                            "Biological signals detected",
                            ""));
                    continue;
                }

                // Sort by value desc, then genus, then full name
                bioRows.sort((a, bRow) -> {
                    String aName = (a.name != null) ? a.name : "";
                    String bName = (bRow.name != null) ? bRow.name : "";

                    String aGenus = firstWord(aName);
                    String bGenus = firstWord(bName);

                    long aVal = (a.cr != null) ? a.cr : Long.MIN_VALUE;
                    long bVal = (bRow.cr != null) ? bRow.cr : Long.MIN_VALUE;

                    int cmp = Long.compare(bVal, aVal);
                    if (cmp != 0) {
                        return cmp;
                    }

                    cmp = aGenus.compareToIgnoreCase(bGenus);
                    if (cmp != 0) {
                        return cmp;
                    }

                    return aName.compareToIgnoreCase(bName);
                });

                // Collapse by genus: "Genus (n)" with max CR
                class GenusSummary {
                    int count = 0;
                    Long maxCr = null;
                }

                Map<String, GenusSummary> byGenus = new LinkedHashMap<>();

                for (BioRowData br : bioRows) {
                    String genus = firstWord(br.name);
                    GenusSummary summary = byGenus.get(genus);
                    if (summary == null) {
                        summary = new GenusSummary();
                        byGenus.put(genus, summary);
                    }
                    summary.count++;
                    if (br.cr != null) {
                        if (summary.maxCr == null || br.cr > summary.maxCr) {
                            summary.maxCr = br.cr;
                        }
                    }
                }

                for (Map.Entry<String, GenusSummary> e : byGenus.entrySet()) {
                    String genus = e.getKey();
                    GenusSummary summary = e.getValue();

                    String label = summary.count > 1
                            ? genus + " (" + summary.count + ")"
                            : genus;

                    String valueText = "";
                    if (summary.maxCr != null) {
                        long millions = Math.round(summary.maxCr / 1_000_000.0);
                        valueText = String.format(Locale.US, "%dM Cr", millions);
                    }

                    rows.add(Row.bio(b.getBodyId(), label, valueText));
                }

                continue;
            }

            //
            // CASE B: We have genus info and/or observed species.
            //   -> Expand by genus, then species.
            //   Rules:
            //     - For a genus with confirmed species: show ONLY confirmed species rows
            //       (truth replaces predictions for that genus).
            //     - For a genus with no confirmed species: show predicted species.
            //

            // Predictions indexed by canonical name and genus
            Map<String, ExobiologyData.BioCandidate> predictedByCanonName = new LinkedHashMap<>();
            Map<String, List<ExobiologyData.BioCandidate>> predictedByGenus = new LinkedHashMap<>();
            if (preds != null) {
                for (ExobiologyData.BioCandidate cand : preds) {
                    String canon = canonicalBioName(cand.getDisplayName());
                    predictedByCanonName.put(canon, cand);
                    String genusKey = firstWord(canon).toLowerCase(Locale.ROOT);
                    predictedByGenus
                            .computeIfAbsent(genusKey, g -> new ArrayList<>())
                            .add(cand);
                }
            }

            // Confirmed species, grouped by genus, using canonical names
            Map<String, List<String>> confirmedByGenus = new LinkedHashMap<>();
            if (observedNamesRaw != null) {
                for (String rawName : observedNamesRaw) {
                    if (rawName == null || rawName.isEmpty()) {
                        continue;
                    }
                    String canon = canonicalBioName(rawName);
                    String genusKey = firstWord(canon).toLowerCase(Locale.ROOT);
                    confirmedByGenus
                            .computeIfAbsent(genusKey, g -> new ArrayList<>())
                            .add(canon);
                }
            }

            // Build genus order:
            //  - first: order from observed genus prefixes
            //  - then any extra genera we only have from predictions / confirmed species
            List<String> genusOrder = new ArrayList<>();
            if (genusPrefixes != null) {
                for (String gp : genusPrefixes) {
                    if (gp == null || gp.isEmpty()) {
                        continue;
                    }
                    String key = gp.toLowerCase(Locale.ROOT);
                    if (!genusOrder.contains(key)) {
                        genusOrder.add(key);
                    }
                }
            }
            for (String key : predictedByGenus.keySet()) {
                if (!genusOrder.contains(key)) {
                    genusOrder.add(key);
                }
            }
            for (String key : confirmedByGenus.keySet()) {
                if (!genusOrder.contains(key)) {
                    genusOrder.add(key);
                }
            }

            if (genusOrder.isEmpty()) {
                // Should be rare, but fall back to generic message
                rows.add(Row.bio(b.getBodyId(),
                        "Biological signals detected",
                        ""));
                continue;
            }

            for (String genusKey : genusOrder) {
                List<ExobiologyData.BioCandidate> predictedForGenus = predictedByGenus.get(genusKey);
                List<String> confirmedForGenus = confirmedByGenus.get(genusKey);

                boolean hasAnySpecies =
                        (confirmedForGenus != null && !confirmedForGenus.isEmpty()) ||
                        (predictedForGenus != null && !predictedForGenus.isEmpty());

                // If we *only* know the genus name and have no species/predictions,
                // keep the old behavior and show a single genus row.
                if (!hasAnySpecies) {
                    String displayGenus;
                    // Fall back to capitalized key (since we have no examples)
                    if (genusKey.isEmpty()) {
                        displayGenus = genusKey;
                    } else {
                        displayGenus = Character.toUpperCase(genusKey.charAt(0))
                                + genusKey.substring(1);
                    }
                    rows.add(Row.bio(b.getBodyId(), displayGenus, ""));
                    continue;
                }

                // From here on, we have at least one species (predicted or confirmed),
                // so we do NOT add a genus-only header line. This removes the
                // "Bacterium" header in stages 2 and 3.

                // If we have confirmed species for this genus, they REPLACE predictions.
                if (confirmedForGenus != null && !confirmedForGenus.isEmpty()) {
                    class SpeciesRow {
                        final String name;
                        final Long cr;

                        SpeciesRow(String name, Long cr) {
                            this.name = name;
                            this.cr = cr;
                        }
                    }

                    List<SpeciesRow> speciesRows = new ArrayList<>();
                    for (String canonName : confirmedForGenus) {
                        ExobiologyData.BioCandidate cand = predictedByCanonName.get(canonName);
                        Long cr = (cand != null) ? cand.getEstimatedPayout(true) : null;
                        speciesRows.add(new SpeciesRow(canonName, cr));
                    }

                    speciesRows.sort((a, bRow) -> {
                        long aVal = (a.cr != null) ? a.cr : Long.MIN_VALUE;
                        long bVal = (bRow.cr != null) ? bRow.cr : Long.MIN_VALUE;
                        int cmp = Long.compare(bVal, aVal);
                        if (cmp != 0) {
                            return cmp;
                        }
                        return a.name.compareToIgnoreCase(bRow.name);
                    });

                    for (SpeciesRow sr : speciesRows) {
                        String valueText = "";
                        if (sr.cr != null) {
                            long millions = Math.round(sr.cr / 1_000_000.0);
                            valueText = String.format(Locale.US, "%dM Cr", millions);
                        }

                        Row bio = Row.bio(b.getBodyId(), sr.name, valueText);
                        // This flag keeps the green styling you like.
                        bio.setObservedGenusHeader(true);
                        rows.add(bio);
                    }
                } else if (predictedForGenus != null && !predictedForGenus.isEmpty()) {
                    // No confirmed species for this genus -> show predicted species
                    predictedForGenus.sort((c1, c2) -> {
                        long v1 = c1.getEstimatedPayout(true);
                        long v2 = c2.getEstimatedPayout(true);
                        int cmp = Long.compare(v2, v1);
                        if (cmp != 0) {
                            return cmp;
                        }
                        String n1 = canonicalBioName(c1.getDisplayName());
                        String n2 = canonicalBioName(c2.getDisplayName());
                        return n1.compareToIgnoreCase(n2);
                    });

                    for (ExobiologyData.BioCandidate cand : predictedForGenus) {
                        String name = canonicalBioName(cand.getDisplayName());
                        long cr = cand.getEstimatedPayout(true);
                        long millions = Math.round(cr / 1_000_000.0);
                        String valueText = String.format(Locale.US, "%dM Cr", millions);
                        rows.add(Row.bio(b.getBodyId(), name, valueText));
                    }
                }
            }
        }

        tableModel.setRows(rows);
    }
    private static String canonicalBioName(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return s;
        }

        String[] parts = s.split("\\s+");
        // Collapse "Genus Genus Species..." -> "Genus Species..."
        if (parts.length >= 3 && parts[0].equalsIgnoreCase(parts[1])) {
            StringBuilder sb = new StringBuilder(parts[0]);
            for (int i = 2; i < parts.length; i++) {
                sb.append(' ').append(parts[i]);
            }
            return sb.toString();
        }

        return s;
    }


    public static String firstWord(String s) {
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    private void updateHeaderLabel() {
        String systemName = state.getSystemName();
        headerLabel.setText(systemName != null ? systemName : "");
        
        StringBuilder sb = new StringBuilder();

        if (state.getTotalBodies() != null) {
            int scanned = state.getBodies().size();
            sb.append("  |  Bodies: ").append(scanned)
              .append(" of ").append(state.getTotalBodies());

            if (state.getFssProgress() != null) {
                sb.append("  (")
                  .append(Math.round(state.getFssProgress() * 100.0))
                  .append("%)");
            }
        }

        if (state.getNonBodyCount() != null) {
            sb.append("  |  Non-bodies: ").append(state.getNonBodyCount());
        }

        headerSummaryLabel.setText(sb.toString());
    }
    public static boolean bodyIssues = false;
    private void persistIfPossible() {
        if (state.getSystemName() != null
                && state.getSystemAddress() != 0L
                && !state.getBodies().isEmpty()) {

        	boolean usedToHaveBodyIssues = bodyIssues;
        	for (BodyInfo x : state.getBodies().values()) {
        		if (x.getBodyId() == -1) {
        			System.out.println("Can't save yet");
        			bodyIssues = false;

        			return;
        		}
    			usedToHaveBodyIssues = true;
        	}
            SystemCache.getInstance().storeSystem(state);
        }
    }

    // ---------------------------------------------------------------------
    // Table model
    // ---------------------------------------------------------------------

    public static class Row {
        final BodyInfo body;
        final boolean detail;
        final int parentId;
        final String bioText;
        final String bioValue;
        private boolean observedGenusHeader;

        boolean isObservedGenusHeader() {
            return observedGenusHeader;
        }

        void setObservedGenusHeader(boolean observedGenusHeader) {
            this.observedGenusHeader = observedGenusHeader;
        }
        private Row(BodyInfo body,
                    boolean detail,
                    int parentId,
                    String bioText,
                    String bioValue) {
            this.body = body;
            this.detail = detail;
            this.parentId = parentId;
            this.bioText = bioText;
            this.bioValue = bioValue;
        }

        static Row body(BodyInfo b) {
            return new Row(b, false, -1, null, null);
        }

        static Row bio(int parentId, String text, String val) {
            return new Row(null, true, parentId, text, val);
        }
    }

    // NEW: custom JTable to draw separators only between systems
    private class SystemBodiesTable extends JTable {

        SystemBodiesTable(SystemBodiesTableModel model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(ED_ORANGE_TRANS);

                int rowCount = tableModel.getRowCount();
                boolean firstBodySeen = false;

                for (int row = 0; row < rowCount; row++) {
                    Row r = tableModel.getRowAt(row);
                    if (!r.detail) { // body row
                        if (firstBodySeen) {
                            Rectangle rect = getCellRect(row, 0, true);
                            int y = rect.y;
                            g2.setColor(ED_ORANGE_TRANS);
                            g2.drawLine(0, y, getWidth(), y);
                        } else {
                            firstBodySeen = true;
                        }
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    class SystemBodiesTableModel extends AbstractTableModel {

        private final String[] columns = {
                "Body",
                "g",
                "Atmosphere / Type",
                "Bio",
                "Value",
                "Land",
                "Dist (Ls)"
        };

        private final List<Row> rows = new ArrayList<>();

        void setRows(List<Row> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        // NEW: allow table to inspect rows (for separators)
        Row getRowAt(int index) {
            return rows.get(index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int col) {
            Row r = rows.get(rowIndex);

            if (r.detail) {
                switch (col) {
                    case 3: return r.bioText != null ? r.bioText : "";
                    case 4: return r.bioValue != null ? r.bioValue : "";
                    default: return "";
                }
            }

            BodyInfo b = r.body;
            switch (col) {
                case 0:
                    return b.getShortName() != null ? b.getShortName() : "";
                case 1:
                    if (b.getGravityMS() == null) return "";
                    double g = b.getGravityMS() / 9.80665;
                    return String.format(Locale.US, "%.2f g", g);
                case 2:
                    return b.getAtmoOrType() != null ? b.getAtmoOrType() : "";
                case 3:
                    // CHANGED: remove bare "Bio" label – only show Geo / Bio+Geo
                    if (b.hasBio() && b.hasGeo()) return "Bio + Geo";
                    if (b.hasGeo()) return "Geo";
                    return "";
                case 4:
                    // Keep "High" marker for the main body row;
                    // detail rows carry the M Cr values.
                    return b.isHighValue() ? "High" : "";
                case 5:
                    return b.isLandable() ? "Yes" : "";
                case 6:
                    if (Double.isNaN(b.getDistanceLs())) return "";
                    return String.format(Locale.US, "%.0f Ls", b.getDistanceLs());
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    }
}
