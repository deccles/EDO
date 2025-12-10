package org.dce.ed.logreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dce.ed.OverlayPreferences;

/**
 * High-level convenience API for reading Elite Dangerous journal events.
 */
public class EliteJournalReader {

    private final EliteLogParser parser = new EliteLogParser();
    private final Path journalDirectory;

    /**
     * Use auto-detected journal directory (via OverlayPreferences / EliteLogFileLocator).
     * @throws IllegalStateException if the directory cannot be located.
     */
    public EliteJournalReader() {
        this(OverlayPreferences.resolveJournalDirectory());
    }

    /**
     * Use a specific journal directory.
     */
    public EliteJournalReader(Path journalDirectory) {
        if (journalDirectory == null || !Files.isDirectory(journalDirectory)) {
            throw new IllegalStateException("Journal directory not found: " + journalDirectory);
        }
        this.journalDirectory = journalDirectory;
    }

    public Path getJournalDirectory() {
        return journalDirectory;
    }

    /**
     * Read all journal files in the directory (sorted by file name)
     * and return all parsed events.
     */
    public List<EliteLogEvent> readAllEvents() throws IOException {
        List<Path> journalFiles = listJournalFiles();
        List<EliteLogEvent> events = new ArrayList<>();

        for (Path file : journalFiles) {
            readEventsFromFile(file, events);
        }

        // Optionally also pull in Status.json as a single StatusEvent "snapshot"
        Path status = EliteLogFileLocator.findStatusFile(journalDirectory);
        if (status != null) {
            String json = Files.readString(status, StandardCharsets.UTF_8);
            EliteLogEvent statusEvent = parser.parseRecord(json);
            events.add(statusEvent);
        }

        events.sort(Comparator.comparing(EliteLogEvent::getTimestamp));
        return events;
    }
    /**
     * Read all events whose timestamp is strictly after the given Instant.
     * <p>
     * This is implemented on top of readEventsFromLastNJournalFiles(int),
     * so we avoid re-reading the entire journal history when the cursor
     * (since) is recent. We grow the number of files considered until
     * we reach a file whose earliest event is at or before the cursor,
     * or we hit all available files.
     */
    public List<EliteLogEvent> readEventsSince(Instant since) throws IOException {
        if (since == null) {
            // Fallback: same behavior as before; caller wants full history.
            return readEventsFromLastNJournalFiles(Integer.MAX_VALUE);
        }

        // Start with a small window of recent files and expand as needed.
        int filesToRead = 4;
        int maxFiles = Integer.MAX_VALUE;

        List<EliteLogEvent> windowEvents = readEventsFromLastNJournalFiles(filesToRead);
        if (windowEvents.isEmpty()) {
            return windowEvents;
        }

        // Grow the window until the earliest event in it is <= since,
        // or until we've pulled in all available files that method can see.
        while (true) {
            Instant earliest = windowEvents.stream()
                    .map(EliteLogEvent::getTimestamp)
                    .filter(Objects::nonNull)
                    .min(Instant::compareTo)
                    .orElse(null);

            // If we have no timestamps at all, or the earliest is at/before the cursor,
            // we've read far enough back in history.
            if (earliest == null || !earliest.isAfter(since)) {
                break;
            }

            // If we've already hit our max (or the read call is not returning more),
            // don't loop forever; just use what we have.
            if (filesToRead >= maxFiles) {
                break;
            }

            int newFilesToRead = filesToRead * 2;
            if (newFilesToRead < filesToRead) { // overflow guard
                break;
            }

            // Expand the window.
            filesToRead = newFilesToRead;
            List<EliteLogEvent> expanded = readEventsFromLastNJournalFiles(filesToRead);
            if (expanded.size() <= windowEvents.size()) {
                // No additional events; we've hit the limit of what this method can provide.
                break;
            }
            windowEvents = expanded;
        }

        // Now filter the window down to strictly-after-since.
        List<EliteLogEvent> result = new ArrayList<>();
        for (EliteLogEvent e : windowEvents) {
            Instant ts = e.getTimestamp();
            if (ts != null && ts.isAfter(since)) {
                result.add(e);
            }
        }

        // Keep them in chronological order just in case the reader didn't already.
        result.sort(Comparator.comparing(EliteLogEvent::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }


    /**
     * Read only the latest journal file (by name).
     */
    public List<EliteLogEvent> readEventsFromLatestJournal() throws IOException {
        List<Path> journalFiles = listJournalFiles();
        if (journalFiles.isEmpty()) {
            return List.of();
        }
        Path latest = journalFiles.get(journalFiles.size() - 1);
        List<EliteLogEvent> events = new ArrayList<>();
        readEventsFromFile(latest, events);
        return events;
    }

    /**
     * Read only events whose timestamp's local date is "today"
     * (according to the system default time zone).
     */
    public List<EliteLogEvent> readEventsForToday() throws IOException {
        LocalDate today = LocalDate.now();
        return readEventsForDate(today);
    }

    /**
     * Read only events whose timestamp's local date matches the given date
     * (system default time zone). To avoid loading your entire history, this
     * method first filters journal files by date portion in their filename,
     * then additionally filters events by timestamp just to be safe.
     *
     * Journal file names look like:
     *   Journal.2025-11-27T154101.01.log
     * so we look for the date's "yyyy-MM-dd" string inside the name.
     */
    public List<EliteLogEvent> readEventsForDate(LocalDate date) throws IOException {
        if (date == null) {
            return List.of();
        }

        List<Path> journalFiles = listJournalFiles();

        String datePart = date.toString(); // yyyy-MM-dd
        List<Path> matchingFiles = new ArrayList<>();
        for (Path p : journalFiles) {
            String name = p.getFileName().toString();
            if (name.contains(datePart)) {
                matchingFiles.add(p);
            }
        }

        if (matchingFiles.isEmpty()) {
            return List.of();
        }

        List<EliteLogEvent> events = new ArrayList<>();
        for (Path file : matchingFiles) {
            readEventsFromFile(file, events);
        }

        ZoneId zone = ZoneId.systemDefault();
        List<EliteLogEvent> filteredByDate =
                events.stream()
                        .filter(e -> {
                            Instant ts = e.getTimestamp();
                            LocalDate eventDate = ts.atZone(zone).toLocalDate();
                            return eventDate.equals(date);
                        })
                        .sorted(Comparator.comparing(EliteLogEvent::getTimestamp))
                        .collect(Collectors.toList());

        // Also consider Status.json if its timestamp matches the given date.
        Path status = EliteLogFileLocator.findStatusFile(journalDirectory);
        if (status != null) {
            try {
                String json = Files.readString(status, StandardCharsets.UTF_8);
                EliteLogEvent statusEvent = parser.parseRecord(json);
                Instant ts = statusEvent.getTimestamp();
                LocalDate statusDate = ts.atZone(zone).toLocalDate();
                if (statusDate.equals(date)) {
                    filteredByDate.add(statusEvent);
                    filteredByDate.sort(Comparator.comparing(EliteLogEvent::getTimestamp));
                }
            } catch (Exception ex) {
                // ignore status parsing errors for this filtered view
            }
        }

        return filteredByDate;
    }

    /**
     * Read events from the last N journal files (by file name order).
     * N <= 0 means "no events".
     */
    public List<EliteLogEvent> readEventsFromLastNJournalFiles(int n) throws IOException {
        if (n <= 0) {
            return List.of();
        }

        List<Path> journalFiles = listJournalFiles();
        if (journalFiles.isEmpty()) {
            return List.of();
        }

        int start = Math.max(0, journalFiles.size() - n);
        List<EliteLogEvent> events = new ArrayList<>();

        for (int i = start; i < journalFiles.size(); i++) {
            readEventsFromFile(journalFiles.get(i), events);
        }

        events.sort(Comparator.comparing(EliteLogEvent::getTimestamp));
        return events;
    }

    /**
     * Return a sorted list of all local dates for which there is at least one
     * Journal.*.log
     *
     * The date is derived from the filename segment between "Journal." and "T",
     * e.g. Journal.2025-11-27T154101.01.log -> 2025-11-27.
     */
    public List<LocalDate> listAvailableDates() throws IOException {
        List<Path> journalFiles = listJournalFiles();
        Set<LocalDate> dates = new HashSet<>();

        for (Path p : journalFiles) {
            String name = p.getFileName().toString();
            // Expect something like: Journal.2025-11-27T154101.01.log
            int firstDot = name.indexOf('.');
            int tIndex = name.indexOf('T', firstDot + 1);
            if (firstDot < 0 || tIndex < 0) {
                continue;
            }
            String datePart = name.substring(firstDot + 1, tIndex);
            try {
                LocalDate d = LocalDate.parse(datePart);
                dates.add(d);
            } catch (Exception ex) {
                // ignore malformed names
            }
        }

        List<LocalDate> list = new ArrayList<>(dates);
        list.sort(Comparator.naturalOrder());
        return list;
    }

    /** package-private so tests can use it if desired */
    void readEventsFromFile(Path file, List<EliteLogEvent> sink) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    EliteLogEvent event = parser.parseRecord(line);
                    sink.add(event);
                } catch (Exception ex) {
                    System.err.println("Failed to parse journal line in " + file + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    private List<Path> listJournalFiles() throws IOException {
        try (Stream<Path> stream = Files.list(journalDirectory)) {
            return stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("Journal.") && name.endsWith(".log");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }
}
