package org.dce.ed.logreader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dce.ed.cache.CachedBody;
import org.dce.ed.cache.SystemCache;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FssAllBodiesFoundEvent;
import org.dce.ed.logreader.event.FssBodySignalsEvent;
import org.dce.ed.logreader.event.FssDiscoveryScanEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.logreader.event.SaasignalsFoundEvent;
import org.dce.ed.logreader.event.ScanEvent;
import org.dce.ed.logreader.event.ScanOrganicEvent;

/**
 * Standalone utility with a main() that scans all Elite Dangerous journal files
 * and populates the local SystemCache with every system/body it can reconstruct.
 *
 * Run this once (with the same JVM/Classpath as the overlay) 
 * before starting the overlay, or periodically to refresh the local body cache.
 */
public class RescanJournalsMain {

    /**
     * Compound key so we can index systems by (address, name).
     */
    private static final class SystemKey {
        final long systemAddress;
        final String systemName;

        SystemKey(long systemAddress, String systemName) {
            this.systemAddress = systemAddress;
            this.systemName = systemName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SystemKey)) {
                return false;
            }
            SystemKey other = (SystemKey) o;
            if (systemAddress != 0L && other.systemAddress != 0L) {
                return systemAddress == other.systemAddress;
            }
            return systemName != null && systemName.equals(other.systemName);
        }

        @Override
        public int hashCode() {
            if (systemAddress != 0L) {
                return Long.hashCode(systemAddress);
            }
            return systemName != null ? systemName.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "SystemKey{" +
                    "systemAddress=" + systemAddress +
                    ", systemName='" + systemName + '\'' +
                    '}';
        }
    }



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

        Map<SystemKey, SystemAccumulator> systems = new HashMap<>();

        String currentSystemName = null;
        long currentSystemAddress = 0L;

        Instant newestEventTimestamp = lastImport;

        for (EliteLogEvent event : events) {
            Instant ts = event.getTimestamp();
            if (ts != null && (newestEventTimestamp == null || ts.isAfter(newestEventTimestamp))) {
                newestEventTimestamp = ts;
            }

            if (event instanceof LocationEvent) {
                LocationEvent le = (LocationEvent) event;
                currentSystemName = le.getStarSystem();
                currentSystemAddress = le.getSystemAddress();
                SystemKey key = new SystemKey(currentSystemAddress, currentSystemName);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyLocation(le);
                if (le.getStarPos() != null)
                	acc.starPos = le.getStarPos();

            } else if (event instanceof FsdJumpEvent) {
                FsdJumpEvent je = (FsdJumpEvent) event;
                currentSystemName = je.getStarSystem();
                currentSystemAddress = je.getSystemAddress();
                SystemKey key = new SystemKey(currentSystemAddress, currentSystemName);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyFsdJump(je);
                if (je.getStarPos() != null) {
                	acc.starPos = je.getStarPos();
                }

            } else if (event instanceof FssDiscoveryScanEvent) {
                FssDiscoveryScanEvent fds = (FssDiscoveryScanEvent) event;
                String name = fds.getSystemName();
                long addr = fds.getSystemAddress();
                if (addr != 0L || name != null) {
                    if (name != null) {
                        currentSystemName = name;
                    }
                    if (addr != 0L) {
                        currentSystemAddress = addr;
                    }
                }
                SystemKey key = new SystemKey(currentSystemAddress, currentSystemName);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyFssDiscovery(fds);

            } else if (event instanceof FssAllBodiesFoundEvent) {
                FssAllBodiesFoundEvent fab = (FssAllBodiesFoundEvent) event;
                long addr = fab.getSystemAddress();
                String name = fab.getSystemName();
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                if (name == null || name.isEmpty()) {
                    name = currentSystemName;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyFssAllBodiesFound(fab);

            } else if (event instanceof ScanEvent) {
                ScanEvent se = (ScanEvent) event;
                long addr = se.getSystemAddress();
                String name = currentSystemName;
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyScan(se);

            } else if (event instanceof FssBodySignalsEvent) {
                FssBodySignalsEvent fb = (FssBodySignalsEvent) event;
                long addr = fb.getSystemAddress();
                String name = currentSystemName;
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applySignals(fb.getBodyId(), fb.getBodyName(), fb.getSignals());

            } else if (event instanceof SaasignalsFoundEvent) {
                SaasignalsFoundEvent sf = (SaasignalsFoundEvent) event;
                long addr = sf.getSystemAddress();
                String name = currentSystemName;
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyGenuses(sf.getBodyId(), sf.getBodyName(), sf.getGenuses());

            } else if (event instanceof ScanOrganicEvent) {
                ScanOrganicEvent so = (ScanOrganicEvent) event;
                long addr = so.getSystemAddress();
                String name = currentSystemName;
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyScanOrganic(so);
            }
        }

        SystemCache cache = SystemCache.getInstance();
        int systemCount = 0;
        int bodyCount = 0;

        for (SystemAccumulator acc : systems.values()) {
            if (acc.bodiesIsEmpty()) {
                continue;
            }
            List<CachedBody> bodies = acc.toCachedBodies();

            cache.put(
                acc.systemAddress,
                acc.systemName,
                acc.starPos,
                acc.totalBodies,
                acc.nonBodyCount,
                acc.fssProgress,
                acc.allBodiesFound,
                bodies
            );
            systemCount++;
            bodyCount += bodies.size();
        }

        if (journalDirectory != null && newestEventTimestamp != null) {
            writeLastImportInstant(journalDirectory, newestEventTimestamp);
            System.out.println("Updated last journal import time to: " + newestEventTimestamp);
        }

        System.out.println("Rescan complete. Cached " + bodyCount + " bodies in " + systemCount + " systems.");
	}
}
