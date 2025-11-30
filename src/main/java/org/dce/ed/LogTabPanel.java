package org.dce.ed;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Tab that displays Elite Dangerous journal events in a table:
 *
 *   Column 1: Date/Time (local time)
 *   Column 2: Event type (journal "event" name)
 *   Column 3: Details (remaining attributes in human-readable form)
 *
 * Features:
 * - Sortable columns (click the table header)
 * - Date and Event columns sized “just enough”; Details fills the rest
 * - Right-click on a row -> "Exclude \"EventName\" events"
 * - Filter dialog with two lists (Excluded/Included) and buttons to move selected
 * - Excluded event names persisted via Preferences
 *
 * Navigation:
 * - Previous/Next day buttons using the set of dates that actually have journal files.
 * - Current date shown between the arrows.
 */
public class LogTabPanel extends JPanel {

    private static final String PREF_KEY_EXCLUDED_EVENT_NAMES = "log.excludedEventNames";

    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withZone(LOCAL_ZONE);

    private final Preferences prefs;

    private EliteJournalReader journalReader;
    private boolean journalReaderAvailable = false;
    private String journalReaderErrorMessage = null;

    // Available dates that actually have journal files
    private List<LocalDate> availableDates = new ArrayList<>();
    private LocalDate currentDate;

    private final JLabel dateLabel;

    /** A single row in the table: either an event or a message row (event == null). */
    private static class LogRow {
        final EliteLogEvent event; // may be null for info/error row
        final String detailsText;  // for event rows: human-readable details; for message rows: full message

        LogRow(EliteLogEvent event, String detailsText) {
            this.event = event;
            this.detailsText = detailsText;
        }
    }

    /** Table model wrapping a list of LogRow items. */
    private static class LogTableModel extends AbstractTableModel {

        private final List<LogRow> rows = new ArrayList<>();

        void setRows(List<LogRow> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        LogRow getRow(int modelIndex) {
            if (modelIndex < 0 || modelIndex >= rows.size()) {
                return null;
            }
            return rows.get(modelIndex);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Date/Time";
                case 1:
                    return "Event";
                case 2:
                    return "Details";
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LogRow row = rows.get(rowIndex);
            if (row.event == null) {
                // Info/error/no-events row: only show text in Details
                if (columnIndex == 2) {
                    return row.detailsText;
                }
                return "";
            }

            EliteLogEvent e = row.event;
            switch (columnIndex) {
                case 0:
                    return formatLocalTime(e.getTimestamp());
                case 1:
                    return extractEventName(e);
                case 2:
                    return row.detailsText;
                default:
                    return "";
            }
        }
    }

    private final LogTableModel tableModel;
    private final JTable logTable;

    /** Event names (journal "event" field) that are currently excluded. */
    private Set<String> excludedEventNames;

    /** All event names seen in the last reload (for the filter dialog). */
    private Set<String> knownEventNames = new HashSet<>();

    public LogTabPanel() {
        super(new BorderLayout());
        this.prefs = Preferences.userNodeForPackage(LogTabPanel.class);
        this.excludedEventNames = loadExcludedEventNamesFromPreferences();

        // Try to initialize the journal reader, but don't die if it fails.
        try {
            this.journalReader = new EliteJournalReader();
            this.journalReaderAvailable = true;
        } catch (Exception ex) {
            this.journalReaderAvailable = false;
            this.journalReaderErrorMessage = "Log reader not available: " + ex.getMessage();
            System.err.println("[LogTabPanel] Failed to initialize EliteJournalReader: " + ex);
        }

        // Toolbar (Prev/Next day + current date + Reload + Filter)
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(new EmptyBorder(4, 4, 4, 4));

        JButton prevDayButton = new JButton("<<");
        JButton nextDayButton = new JButton(">>");
        dateLabel = new JLabel("-");
        dateLabel.setBorder(new EmptyBorder(0, 8, 0, 8));

        JButton reloadButton = new JButton("Reload");
        JButton filterButton = new JButton("Filter...");

        toolBar.add(prevDayButton);
        toolBar.add(dateLabel);
        toolBar.add(nextDayButton);
        toolBar.add(Box.createHorizontalStrut(16));
        toolBar.add(reloadButton);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(filterButton);

        add(toolBar, BorderLayout.NORTH);

        // JTable-based log view
        tableModel = new LogTableModel();
        logTable = new JTable(tableModel);
//        logTable.setFont(createRoundedLogFont());
        logTable.setRowHeight(logTable.getRowHeight() + 4); // a bit more vertical space
        logTable.setFillsViewportHeight(true);

        // Sortable columns
        TableRowSorter<LogTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setComparator(0, Comparator.naturalOrder());
        sorter.setComparator(1, String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(2, String.CASE_INSENSITIVE_ORDER);
        logTable.setRowSorter(sorter);

        // Column widths: Date and Event narrow, Details fills the rest
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        if (logTable.getColumnModel().getColumnCount() >= 3) {
            TableColumn dateCol = logTable.getColumnModel().getColumn(0);
            dateCol.setPreferredWidth(150);
            dateCol.setMinWidth(140);

            TableColumn eventCol = logTable.getColumnModel().getColumn(1);
            eventCol.setPreferredWidth(140);
            eventCol.setMinWidth(120);

            TableColumn detailsCol = logTable.getColumnModel().getColumn(2);
            detailsCol.setPreferredWidth(600);
        }

        // Right-click context menu for excluding this event name
        logTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopup(e);
            }

            private void handlePopup(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                Point p = e.getPoint();
                int viewRow = logTable.rowAtPoint(p);
                if (viewRow < 0) {
                    return;
                }
                logTable.setRowSelectionInterval(viewRow, viewRow);
                int modelRow = logTable.convertRowIndexToModel(viewRow);
                LogRow row = tableModel.getRow(modelRow);
                if (row == null || row.event == null) {
                    return; // it's an info/error row, no event to filter on
                }

                String eventName = extractEventName(row.event);
                if (eventName == null || eventName.isEmpty()) {
                    return;
                }

                JPopupMenu menu = new JPopupMenu();
                JMenuItem excludeItem = new JMenuItem("Exclude \"" + eventName + "\" events");
                excludeItem.addActionListener(ev -> {
                    excludedEventNames.add(eventName);
                    saveExcludedEventNamesToPreferences();
                    reloadLogs();
                });
                menu.add(excludeItem);
                menu.show(logTable, e.getX(), e.getY());
            }
        });

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setPreferredSize(new Dimension(400, 600));
        add(scrollPane, BorderLayout.CENTER);

        // Wire actions
        reloadButton.addActionListener(e -> reloadLogs());
        filterButton.addActionListener(e -> showFilterDialog());

        prevDayButton.addActionListener(e -> moveToRelativeDate(-1));
        nextDayButton.addActionListener(e -> moveToRelativeDate(+1));

        // Initial date setup & load
        initAvailableDates();
        reloadLogs();
    }

    /* ---------- Dates & timestamps helpers ---------- */

    private static String formatLocalTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return TS_FORMAT.format(instant);
    }

    /**
     * Try to get the raw journal "event" field; fall back to enum type.
     */
    private static String extractEventName(EliteLogEvent event) {
        if (event == null) {
            return "";
        }
        JsonObject raw = event.getRawJson();
        if (raw != null && raw.has("event") && !raw.get("event").isJsonNull()) {
            try {
                return raw.get("event").getAsString();
            } catch (Exception ignored) {
                // fall through
            }
        }
        return event.getType() != null ? event.getType().name() : "";
    }

    private void initAvailableDates() {
        if (!journalReaderAvailable) {
            availableDates = new ArrayList<>();
            currentDate = null;
            dateLabel.setText("-");
            return;
        }
        try {
            availableDates = journalReader.listAvailableDates();
            if (availableDates.isEmpty()) {
                currentDate = null;
                dateLabel.setText("-");
            } else {
                // Default: most recent date with logs
                currentDate = availableDates.get(availableDates.size() - 1);
                dateLabel.setText(currentDate.toString());
            }
        } catch (Exception ex) {
            System.err.println("[LogTabPanel] Failed to list available dates: " + ex.getMessage());
            availableDates = new ArrayList<>();
            currentDate = null;
            dateLabel.setText("-");
        }
    }

    private void moveToRelativeDate(int offset) {
        if (availableDates == null || availableDates.isEmpty() || currentDate == null) {
            return;
        }
        int idx = availableDates.indexOf(currentDate);
        if (idx < 0) {
            return;
        }
        int newIdx = idx + offset;
        if (newIdx < 0 || newIdx >= availableDates.size()) {
            return; // no earlier/later date
        }
        currentDate = availableDates.get(newIdx);
        dateLabel.setText(currentDate.toString());
        reloadLogs();
    }

    /* ---------- Preferences for excluded events ---------- */

    private Set<String> loadExcludedEventNamesFromPreferences() {
        String raw = prefs.get(PREF_KEY_EXCLUDED_EVENT_NAMES, "");
        Set<String> set = new HashSet<>();
        if (raw == null || raw.isEmpty()) {
            return set;
        }

        String[] tokens = raw.split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private void saveExcludedEventNamesToPreferences() {
        String value = excludedEventNames.stream()
                .sorted()
                .collect(Collectors.joining(","));
        prefs.put(PREF_KEY_EXCLUDED_EVENT_NAMES, value);
    }

    /* ---------- Reload & formatting of rows ---------- */

    private void reloadLogs() {
        knownEventNames.clear();

        // Recreate the journal reader so Logging preferences (auto vs custom folder)
        // take effect each time we reload.
        try {
            this.journalReader = new EliteJournalReader();
            this.journalReaderAvailable = true;
            this.journalReaderErrorMessage = null;
        } catch (Exception ex) {
            this.journalReaderAvailable = false;
            this.journalReaderErrorMessage = "Log reader not available: " + ex.getMessage();
            System.err.println("[LogTabPanel] Failed to initialize EliteJournalReader: " + ex);
        }

        if (!journalReaderAvailable) {
            String msg = (journalReaderErrorMessage != null)
                    ? journalReaderErrorMessage
                    : "Elite Dangerous logs not found.";
            List<LogRow> rows = new ArrayList<>();
            rows.add(new LogRow(null, msg));
            tableModel.setRows(rows);
            dateLabel.setText("-");
            return;
        }

        if (currentDate == null) {
            // Try to re-init dates (e.g., first run or after an error)
            initAvailableDates();
            if (currentDate == null) {
                List<LogRow> rows = new ArrayList<>();
                rows.add(new LogRow(null, "No Elite Dangerous journal files found."));
                tableModel.setRows(rows);
                return;
            }
        }

        dateLabel.setText(currentDate.toString());

        List<EliteLogEvent> events;
        try {
            events = journalReader.readEventsForDate(currentDate);
        } catch (Exception ex) {
            String msg = "Error reading Elite Dangerous logs: " + ex.getMessage();
            System.err.println("[LogTabPanel] " + msg);
            List<LogRow> rows = new ArrayList<>();
            rows.add(new LogRow(null, msg));
            tableModel.setRows(rows);
            return;
        }

        List<LogRow> visibleRows = new ArrayList<>();

        for (EliteLogEvent event : events) {
            String eventName = extractEventName(event);
            if (eventName != null && !eventName.isEmpty()) {
                knownEventNames.add(eventName);
            }

            if (eventName != null && excludedEventNames.contains(eventName)) {
                // filtered out
                continue;
            }

            String details = formatDetails(event);
            visibleRows.add(new LogRow(event, details));
        }

        if (visibleRows.isEmpty()) {
            visibleRows.add(new LogRow(null, "No events found for " + currentDate.toString() + "."));
        }

        tableModel.setRows(visibleRows);
    }

    /**
     * Format the "details" column for a given event.
     * Known event types get structured text; others get key=value pairs
     * of remaining attributes (minus timestamp/event).
     */
    private static String formatDetails(EliteLogEvent e) {
        if (e == null) {
            return "";
        }

        EliteEventType type = e.getType();
        switch (type) {
            case FILEHEADER: {
                EliteLogEvent.FileheaderEvent fe = (EliteLogEvent.FileheaderEvent) e;
                return "part=" + fe.getPart()
                        + ", odyssey=" + fe.isOdyssey()
                        + ", gameVersion=" + safe(fe.getGameVersion())
                        + ", build=" + safe(fe.getBuild());
            }
            case COMMANDER: {
                EliteLogEvent.CommanderEvent ce = (EliteLogEvent.CommanderEvent) e;
                return "name=" + safe(ce.getName())
                        + ", fid=" + safe(ce.getFid());
            }
            case LOAD_GAME: {
                EliteLogEvent.LoadGameEvent lg = (EliteLogEvent.LoadGameEvent) e;
                return "commander=" + safe(lg.getCommander())
                        + ", ship=" + safe(lg.getShip())
                        + ", shipName=" + safe(lg.getShipName())
                        + ", fuel=" + lg.getFuelLevel() + "/" + lg.getFuelCapacity()
                        + ", mode=" + safe(lg.getGameMode())
                        + ", credits=" + lg.getCredits();
            }
            case LOCATION: {
                EliteLogEvent.LocationEvent le = (EliteLogEvent.LocationEvent) e;
                return "system=" + safe(le.getStarSystem())
                        + ", body=" + safe(le.getBody())
                        + ", bodyType=" + safe(le.getBodyType())
                        + ", docked=" + le.isDocked()
                        + ", taxi=" + le.isTaxi()
                        + ", multicrew=" + le.isMulticrew();
            }
            case START_JUMP: {
                EliteLogEvent.StartJumpEvent sj = (EliteLogEvent.StartJumpEvent) e;
                return "type=" + safe(sj.getJumpType())
                        + ", system=" + safe(sj.getStarSystem())
                        + ", starClass=" + safe(sj.getStarClass())
                        + ", taxi=" + sj.isTaxi();
            }
            case FSD_JUMP: {
                EliteLogEvent.FsdJumpEvent fj = (EliteLogEvent.FsdJumpEvent) e;
                return "system=" + safe(fj.getStarSystem())
                        + ", body=" + safe(fj.getBody())
                        + ", bodyType=" + safe(fj.getBodyType())
                        + ", jumpDist=" + fj.getJumpDist()
                        + ", fuelUsed=" + fj.getFuelUsed()
                        + ", fuelLevel=" + fj.getFuelLevel();
            }
            case FSD_TARGET: {
                EliteLogEvent.FsdTargetEvent ft = (EliteLogEvent.FsdTargetEvent) e;
                return "name=" + safe(ft.getName())
                        + ", starClass=" + safe(ft.getStarClass())
                        + ", remainingJumps=" + ft.getRemainingJumpsInRoute();
            }
            case SAASIGNALS_FOUND: {
                EliteLogEvent.SaasignalsFoundEvent sa = (EliteLogEvent.SaasignalsFoundEvent) e;
                StringBuilder sb = new StringBuilder();
                sb.append("body=").append(safe(sa.getBodyName()));

                if (sa.getSignals() != null && !sa.getSignals().isEmpty()) {
                    sb.append(", signals=");
                    boolean first = true;
                    for (EliteLogEvent.SaasignalsFoundEvent.Signal s : sa.getSignals()) {
                        if (!first) {
                            sb.append("; ");
                        }
                        first = false;
                        sb.append(s.getType()).append("(").append(s.getCount()).append(")");
                    }
                }

                if (sa.getGenuses() != null && !sa.getGenuses().isEmpty()) {
                    sb.append(", genuses=");
                    boolean first = true;
                    for (EliteLogEvent.SaasignalsFoundEvent.Genus g : sa.getGenuses()) {
                        if (!first) {
                            sb.append("; ");
                        }
                        first = false;
                        String name = g.getGenusLocalised() != null
                                ? g.getGenusLocalised()
                                : g.getGenus();
                        sb.append(name);
                    }
                }

                return sb.toString();
            }
            case STATUS: {
                EliteLogEvent.StatusEvent st = (EliteLogEvent.StatusEvent) e;
                StringBuilder sb = new StringBuilder();
                sb.append("flags=").append(st.getFlags())
                        .append(", flags2=").append(st.getFlags2())
                        .append(", guiFocus=").append(st.getGuiFocus())
                        .append(", fuelMain=").append(st.getFuelMain())
                        .append(", fuelRes=").append(st.getFuelReservoir())
                        .append(", cargo=").append(st.getCargo())
                        .append(", legal=").append(safe(st.getLegalState()))
                        .append(", balance=").append(st.getBalance());
                int[] pips = st.getPips();
                if (pips != null && pips.length == 3) {
                    sb.append(", pips=[")
                            .append(pips[0]).append(',')
                            .append(pips[1]).append(',')
                            .append(pips[2]).append(']');
                }
                return sb.toString();
            }
            case RECEIVE_TEXT: {
                EliteLogEvent.ReceiveTextEvent rt = (EliteLogEvent.ReceiveTextEvent) e;
                String msg = rt.getMessageLocalised() != null
                        ? rt.getMessageLocalised()
                        : rt.getMessage();
                return "from=" + safe(rt.getFrom())
                        + ", channel=" + safe(rt.getChannel())
                        + ", msg=" + safe(msg);
            }
            case NAV_ROUTE:
                return "Nav route updated";
            case NAV_ROUTE_CLEAR:
                return "Nav route cleared";
            default:
                // Generic/unknown: pretty-print remaining attributes as key=value pairs
                return formatGenericAttributes(e);
        }
    }

    /**
     * For generic/unknown events: return a "key=value, key2=value2" string
     * built from raw JSON minus "timestamp" and "event".
     */
    private static String formatGenericAttributes(EliteLogEvent event) {
        JsonObject raw = event.getRawJson();
        if (raw == null || raw.entrySet().isEmpty()) {
            return "";
        }

        JsonObject copy = raw.deepCopy();
        copy.remove("timestamp");
        copy.remove("event");
        if (copy.entrySet().isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (String key : copy.keySet()) {
            JsonElement el = copy.get(key);
            if (el == null || el.isJsonNull()) {
                continue;
            }
            parts.add(key + "=" + el.toString());
        }
        return String.join(", ", parts);
    }

    private static String safe(String s) {
        return s == null ? "<null>" : s;
    }

    /* ---------- Filter dialog ---------- */

    private void showFilterDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);

        List<String> sortedNames = new ArrayList<>(knownEventNames);
        Collections.sort(sortedNames, String.CASE_INSENSITIVE_ORDER);

        LogFilterDialog dialog = new LogFilterDialog(owner, sortedNames, excludedEventNames);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (dialog.isOkPressed()) {
            this.excludedEventNames = dialog.getExcludedEventNames();
            saveExcludedEventNamesToPreferences();
            reloadLogs();
        }
    }

    /**
     * Modal dialog to choose which journal "event" names are excluded vs included.
     *
     * Layout:
     *
     *   Excluded:
     *   [excluded list]
     *   [Include Selected]
     *
     *   Included:
     *   [included list]
     *   [Exclude Selected]
     */
    private static class LogFilterDialog extends JDialog {

        private boolean okPressed = false;
        private final List<String> eventNames;

        private final DefaultListModel<String> excludedModel = new DefaultListModel<>();
        private final DefaultListModel<String> includedModel = new DefaultListModel<>();

        private final JList<String> excludedList = new JList<>(excludedModel);
        private final JList<String> includedList = new JList<>(includedModel);

        private Set<String> excludedEventNames;

        LogFilterDialog(Window owner, List<String> eventNames, Set<String> initiallyExcluded) {
            super(owner, "Log Filter", ModalityType.APPLICATION_MODAL);
            this.eventNames = eventNames;

            setLayout(new BorderLayout());
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            initModels(initiallyExcluded);
            add(buildContentPanel(), BorderLayout.CENTER);
            add(buildButtonPanel(), BorderLayout.SOUTH);

            pack();
        }

        private void initModels(Set<String> initiallyExcluded) {
            // Fill excluded / included models from eventNames + initiallyExcluded
            for (String name : eventNames) {
                if (initiallyExcluded.contains(name)) {
                    excludedModel.addElement(name);
                } else {
                    includedModel.addElement(name);
                }
            }
        }

        private JPanel buildContentPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(new EmptyBorder(8, 8, 8, 8));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new java.awt.Insets(4, 4, 4, 4);

            // Row 0: Excluded label
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel excludedLabel = new JLabel("Excluded:");
            panel.add(excludedLabel, gbc);

            // Row 1: Excluded list
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.CENTER;
            JScrollPane excludedScroll = new JScrollPane(excludedList);
            excludedScroll.setPreferredSize(new Dimension(260, 120));
            panel.add(excludedScroll, gbc);

            // Row 2: "Include Selected" button
            gbc.gridy = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            JPanel includeSelectedPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            JButton includeSelectedButton = new JButton("Include Selected");
            includeSelectedButton.addActionListener(e ->
                    moveSelected(excludedList, excludedModel, includedModel));
            includeSelectedPanel.add(includeSelectedButton);
            panel.add(includeSelectedPanel, gbc);

            // Row 3: Included label
            gbc.gridy = 3;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel includedLabel = new JLabel("Included:");
            panel.add(includedLabel, gbc);

            // Row 4: Included list
            gbc.gridy = 4;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.CENTER;
            JScrollPane includedScroll = new JScrollPane(includedList);
            includedScroll.setPreferredSize(new Dimension(260, 140));
            panel.add(includedScroll, gbc);

            // Row 5: "Exclude Selected" button
            gbc.gridy = 5;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            JPanel excludeSelectedPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            JButton excludeSelectedButton = new JButton("Exclude Selected");
            excludeSelectedButton.addActionListener(e ->
                    moveSelected(includedList, includedModel, excludedModel));
            excludeSelectedPanel.add(excludeSelectedButton);
            panel.add(excludeSelectedPanel, gbc);

            return panel;
        }

        private void moveSelected(JList<String> fromList,
                                  DefaultListModel<String> fromModel,
                                  DefaultListModel<String> toModel) {
            List<String> selected = fromList.getSelectedValuesList();
            if (selected.isEmpty()) {
                return;
            }
            for (String s : selected) {
                if (!toModel.contains(s)) {
                    toModel.addElement(s);
                }
                fromModel.removeElement(s);
            }
        }

        private JPanel buildButtonPanel() {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

            JButton includeAllButton = new JButton("Include All");
            JButton excludeAllButton = new JButton("Exclude All");
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");

            includeAllButton.addActionListener(e -> {
                // move everything into Included
                copyModel(excludedModel, includedModel, true);
            });

            excludeAllButton.addActionListener(e -> {
                // move everything into Excluded
                copyModel(includedModel, excludedModel, true);
            });

            okButton.addActionListener(e -> {
                okPressed = true;
                computeExcludedEventNamesFromUI();
                dispose();
            });

            cancelButton.addActionListener(e -> dispose());

            buttonPanel.add(includeAllButton);
            buttonPanel.add(excludeAllButton);
            buttonPanel.add(cancelButton);
            buttonPanel.add(okButton);

            return buttonPanel;
        }

        private void copyModel(DefaultListModel<String> from,
                               DefaultListModel<String> to,
                               boolean clearFrom) {
            List<String> all = new ArrayList<>();
            for (int i = 0; i < from.size(); i++) {
                all.add(from.getElementAt(i));
            }
            for (String s : all) {
                if (!to.contains(s)) {
                    to.addElement(s);
                }
            }
            if (clearFrom) {
                from.clear();
            }
        }

        private void computeExcludedEventNamesFromUI() {
            Set<String> excluded = new HashSet<>();
            for (int i = 0; i < excludedModel.size(); i++) {
                excluded.add(excludedModel.getElementAt(i));
            }
            this.excludedEventNames = excluded;
        }

        boolean isOkPressed() {
            return okPressed;
        }

        Set<String> getExcludedEventNames() {
            return excludedEventNames == null ? new HashSet<>() : excludedEventNames;
        }
    }
//    private static Font createLogFont() {
//        // Prefer Segoe UI (Windows), fall back to other common sans-serifs
//        String[] preferred = {
//            "Segoe UI",
//            "Calibri",
//            "Tahoma",
//            "Arial",
//            "SansSerif"
//        };
//
//        for (String name : preferred) {
//            Font f = new Font(name, Font.PLAIN, 13);
//            if (f.getFamily().equals(name)) {
//                return f;
//            }
//        }
//
//        // Last resort
//        return new Font("SansSerif", Font.PLAIN, 13);
//    }

    private static Font createRoundedLogFont() {
        // Prefer a very rounded font first
        String[] preferred = {
            "Consolas",  // nice, chonky, rounded
            "Calibri",
            "Verdana",
            "Tahoma",
            "SansSerif"
        };

        for (String name : preferred) {
            Font f = new Font(name, Font.PLAIN, 13);
            // getFamily() returns a real family name if it's actually available
            if (f.getFamily().equals(name)) {
                return f;
            }
        }

        return new Font("SansSerif", Font.PLAIN, 13);
    }

}
