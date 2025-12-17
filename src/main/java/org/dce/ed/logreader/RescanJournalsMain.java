package org.dce.ed.logreader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.dce.ed.cache.SystemCache;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.state.SystemEventProcessor;
import org.dce.ed.state.SystemState;

/**
 * Standalone utility with a main() that scans all Elite Dangerous journal files
 * and populates the local SystemCache with every system/body it can reconstruct.
 *
 * Run this once (with the same JVM/Classpath as the overlay)
 * before starting the overlay, or periodically to refresh the local body cache.
 */
public class RescanJournalsMain {

    private static final String LAST_IMPORT_FILENAME = "edo-cache.lastRescanTimestamp";

    private static Instant readLastImportInstant(Path journalDirectory) {
        if (journalDirectory == null) {
            return null;
        }
        Path cursor = journalDirectory.resolve(LAST_IMPORT_FILENAME);
        if (!Files.isRegularFile(cursor)) {
            return null;
        }
        try {
            String text = Files.readString(cursor, StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                return null;
            }
            return Instant.parse(text);
        } catch (Exception ex) {
            System.err.println("Failed to read last import timestamp from " + cursor + ": " + ex.getMessage());
            return null;
        }
    }

    private static void writeLastImportInstant(Path journalDirectory, Instant instant) {
        if (journalDirectory == null || instant == null) {
            return;
        }
        Path cursor = journalDirectory.resolve(LAST_IMPORT_FILENAME);
        try {
            Files.writeString(cursor, instant.toString(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("Failed to write last import timestamp to " + cursor + ": " + ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Rescanning Elite Dangerous journals and rebuilding local system cache...");

        boolean forceFull = false;
        if (args != null) {
            for (String arg : args) {
                if ("--full".equalsIgnoreCase(arg)) {
                    forceFull = true;
                    break;
                }
            }
        }

        rescanJournals(forceFull);
    }

    public static void rescanJournals(boolean forceFull) throws IOException {
        EliteJournalReader reader = new EliteJournalReader();

        Path journalDirectory = reader.getJournalDirectory();
        Instant lastImport = null;
        if (!forceFull) {
            lastImport = readLastImportInstant(journalDirectory);
            if (lastImport == null) {
                System.out.println("No previous journal import timestamp found; doing full rescan.");
            } else {
                System.out.println("Last journal import time (UTC): " + lastImport);
            }
        } else {
            System.out.println("Forcing full rescan (--full). Ignoring any existing import timestamp.");
        }

        List<EliteLogEvent> events;
        if (lastImport == null) {
            events = reader.readEventsFromLastNJournalFiles(Integer.MAX_VALUE);
        } else {
            events = reader.readEventsSince(lastImport);
        }

        System.out.println("Loaded " + events.size() + " events from journal files.");

        SystemCache cache = SystemCache.getInstance();

        SystemState state = new SystemState();
        SystemEventProcessor processor = new SystemEventProcessor(state);

        Instant newestEventTimestamp = lastImport;

        for (EliteLogEvent event : events) {
            Instant ts = event.getTimestamp();
            if (ts != null && (newestEventTimestamp == null || ts.isAfter(newestEventTimestamp))) {
                newestEventTimestamp = ts;
            }

            // IMPORTANT:
            // SystemEventProcessor.enterSystem(...) clears bodies when we jump/relocate to a new system.
            // To avoid losing the previous system's accumulated state, persist BEFORE processing the
            // Location/FSDJump that causes the reset.
            if (event instanceof LocationEvent) {
                LocationEvent le = (LocationEvent) event;
                persistIfSystemIsChanging(cache, state, le.getStarSystem(), le.getSystemAddress());
            } else if (event instanceof FsdJumpEvent) {
                FsdJumpEvent je = (FsdJumpEvent) event;
                persistIfSystemIsChanging(cache, state, je.getStarSystem(), je.getSystemAddress());
            }

            processor.handleEvent(event);
        }

        // Persist the final system (if valid)
        cache.storeSystem(state);

        if (journalDirectory != null && newestEventTimestamp != null) {
            writeLastImportInstant(journalDirectory, newestEventTimestamp);
            System.out.println("Updated last journal import time to: " + newestEventTimestamp);
        }

        System.out.println("Rescan complete.");
    }

    private static void persistIfSystemIsChanging(SystemCache cache, SystemState state, String nextName, long nextAddr) {
        String curName = state.getSystemName();
        long curAddr = state.getSystemAddress();

        boolean sameName = nextName != null && nextName.equals(curName);
        boolean sameAddr = nextAddr != 0L && nextAddr == curAddr;

        if (sameName || sameAddr) {
            return;
        }

        cache.storeSystem(state);
    }
}
