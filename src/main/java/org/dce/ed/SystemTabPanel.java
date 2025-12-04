package org.dce.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.dce.ed.exobiology.ExobiologyData;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.FsdJumpEvent;
import org.dce.ed.logreader.EliteLogEvent.LocationEvent;
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

    private final JTable table;
    private final JLabel headerLabel;
    private final SystemBodiesTableModel tableModel;

    private final SystemState state = new SystemState();
    private final SystemEventProcessor processor = new SystemEventProcessor(state);

    public SystemTabPanel() {
        super(new BorderLayout());
        setOpaque(false);

        // Header label
        headerLabel = new JLabel("Waiting for system data…");
        headerLabel.setForeground(ED_ORANGE);
        headerLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));

        // Table setup
        tableModel = new SystemBodiesTableModel();
        table = new JTable(tableModel);
        table.setOpaque(false);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(500, 300));

        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setForeground(ED_ORANGE);
        header.setBackground(Color.BLACK);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));

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
                label.setFont(label.getFont().deriveFont(Font.BOLD));
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

                c.setForeground(isSelected ? Color.BLACK : ED_ORANGE);
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
                c.setForeground(isSelected ? Color.BLACK : ED_ORANGE);

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
        add(headerLabel, BorderLayout.NORTH);
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

            List<EliteLogEvent> events = reader.readEventsFromLatestJournal();

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

            SystemCache cache = SystemCache.getInstance();
            SystemCache.CachedSystem cs = cache.get(systemAddress, systemName);

            if (cs != null) {
                cache.loadInto(state, cs);
                rebuildTable();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

            // Start from whatever predictions the state has already computed.
            List<ExobiologyData.BioCandidate> preds = b.getPredictions();

            // If there are no predictions yet, try a one-shot calculation here
            // so the GUI can still show something useful.
            if (preds == null || preds.isEmpty()) {
                ExobiologyData.BodyAttributes attrs = b.buildBodyAttributes();
                if (attrs != null) {
                    List<ExobiologyData.BioCandidate> base = ExobiologyData.predict(attrs);
                    if (base != null && !base.isEmpty()) {
                        // If we know which genera are present, narrow to those.
                        java.util.Set<String> genusPrefixes = b.getObservedGenusPrefixes();
                        if (genusPrefixes != null && !genusPrefixes.isEmpty()) {
                            java.util.Set<String> lower = new java.util.HashSet<>();
                            for (String g : genusPrefixes) {
                                if (g != null && !g.isEmpty()) {
                                    lower.add(g.toLowerCase(java.util.Locale.ROOT));
                                }
                            }

                            List<ExobiologyData.BioCandidate> filtered = new ArrayList<>();
                            for (ExobiologyData.BioCandidate cand : base) {
                                String nameLower = cand.getDisplayName()
                                        .toLowerCase(java.util.Locale.ROOT);
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

                            if (!filtered.isEmpty()) {
                                preds = filtered;
                            } else {
                                preds = base;
                            }
                        } else {
                            preds = base;
                        }
                    }
                }
            }

            // Build the final list of display names:
            //  1) Observed names (truth from ScanOrganic / DSS)
            //  2) Any predicted names we don't already have
            List<String> displayNames = new ArrayList<>();

            java.util.Set<String> existing = new java.util.LinkedHashSet<>(displayNames);

            Set<String> observed = b.getObservedBioDisplayNames();
            if (observed != null && !observed.isEmpty()) {
                displayNames.addAll(observed);
            } else if (preds != null && !preds.isEmpty()) {
                for (ExobiologyData.BioCandidate cand : preds) {
                    String name = cand.getDisplayName();
                    if (!existing.contains(name)) {
                        displayNames.add(name);
                        existing.add(name);
                    }
                }
            }

            if (!displayNames.isEmpty()) {
                // Collect bio rows with their credit values so we can sort
                class BioRowData {
                    final String name;
                    final Long cr;

                    BioRowData(String name, Long cr) {
                        this.name = name;
                        this.cr = cr;
                    }
                }

                List<BioRowData> bioRows = new ArrayList<>();

                for (String name : displayNames) {
                    Long cr = null;
                    if (preds != null && !preds.isEmpty()) {
                        for (ExobiologyData.BioCandidate cand : preds) {
                            if (name.equals(cand.getDisplayName())) {
                                cr = cand.getEstimatedPayout(true);
                                break;
                            }
                        }
                    }

                    bioRows.add(new BioRowData(name, cr));
                }

             // Sort:
           //   1) Genus (first word) ascending
           //   2) Value numerically (credits) descending
           //   3) Full name ascending as tie-breaker
           bioRows.sort((a, bRow) -> {
               String aName = (a.name != null) ? a.name : "";
               String bName = (bRow.name != null) ? bRow.name : "";

               // Genus = first word before space
               String aGenus = aName;
               String bGenus = bName;

               int aSpace = aName.indexOf(' ');
               if (aSpace > 0) {
                   aGenus = aName.substring(0, aSpace);
               }
               int bSpace = bName.indexOf(' ');
               if (bSpace > 0) {
                   bGenus = bName.substring(0, bSpace);
               }

               // 1) Genus ascending
               int cmp = aGenus.compareToIgnoreCase(bGenus);
               if (cmp != 0) {
                   return cmp;
               }

               // 2) Value descending (higher first)
               long aVal = (a.cr != null) ? a.cr.longValue() : Long.MIN_VALUE;
               long bVal = (bRow.cr != null) ? bRow.cr.longValue() : Long.MIN_VALUE;
               cmp = Long.compare(bVal, aVal);
               if (cmp != 0) {
                   return cmp;
               }

               // 3) Full name ascending
               return aName.compareToIgnoreCase(bName);
           });



                for (BioRowData data : bioRows) {
                    String valueText = "";
                    if (data.cr != null) {
                        long millions = Math.round(data.cr.longValue() / 1_000_000.0);
                        valueText = String.format(Locale.US, "%dM Cr", millions);
                    }

                    rows.add(Row.bio(b.getBodyId(), data.name, valueText));
                }
            } else {
                rows.add(Row.bio(
                        b.getBodyId(),
                        "Biological signals detected",
                        ""));
            }
        }

        tableModel.setRows(rows);
    }

    private void updateHeaderLabel() {
        String name = state.getSystemName();
        StringBuilder sb = new StringBuilder();

        if (name != null && !name.isEmpty()) {
            sb.append(name);
        } else {
            sb.append("Current system");
        }

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

        headerLabel.setText(sb.toString());
    }

    private void persistIfPossible() {
        if (state.getSystemName() != null
                && state.getSystemAddress() != 0L
                && !state.getBodies().isEmpty()) {

            SystemCache.getInstance().storeSystem(state);
        }
    }

    // ---------------------------------------------------------------------
    // Table model
    // ---------------------------------------------------------------------

    private static class Row {
        final BodyInfo body;
        final boolean detail;
        final int parentId;
        final String bioText;
        final String bioValue;

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
                    if (b.hasBio() && b.hasGeo()) return "Bio + Geo";
                    if (b.hasBio()) return "Bio";
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
