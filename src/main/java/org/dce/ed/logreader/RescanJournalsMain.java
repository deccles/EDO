package org.dce.ed.logreader;

import org.dce.ed.SystemCache;
import org.dce.ed.logreader.EliteJournalReader;
import org.dce.ed.logreader.EliteLogEvent;
import org.dce.ed.logreader.EliteLogEvent.FsdJumpEvent;
import org.dce.ed.logreader.EliteLogEvent.LocationEvent;
import org.dce.ed.logreader.EliteLogEvent.FssDiscoveryScanEvent;
import org.dce.ed.logreader.EliteLogEvent.ScanEvent;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone utility with a main() that scans all Elite Dangerous journal files
 * and populates the local SystemCache with every system/body it can reconstruct.
 *
 * Run this once (with the same JVM/Classpath as the overlay) to backfill the
 * cache from your entire journal history.
 */
public class RescanJournalsMain {

    /**
     * Key used to group events by system.
     * Prefer systemAddress when available, otherwise fall back to systemName.
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

            // If either side has a non-zero address, compare by address.
            if (this.systemAddress != 0L || other.systemAddress != 0L) {
                return this.systemAddress == other.systemAddress;
            }

            // Otherwise fall back to name.
            if (this.systemName == null && other.systemName == null) {
                return true;
            }
            if (this.systemName == null || other.systemName == null) {
                return false;
            }
            return this.systemName.equals(other.systemName);
        }

        @Override
        public int hashCode() {
            if (systemAddress != 0L) {
                return Long.hashCode(systemAddress);
            }
            return systemName != null ? systemName.hashCode() : 0;
        }
    }

    /**
     * Minimal body info, mirrors the fields we persist in SystemCache.CachedBody.
     */
    private static final class BodyInfo {
        String name;
        int bodyId = -1;
        double distanceLs = Double.NaN;
        Double gravityMS = null;
        boolean landable = false;
        boolean hasBio = false;
        boolean hasGeo = false;
        boolean highValue = false;
        String atmoOrType = "";
    }

    /**
     * Accumulates data for one system across all events.
     */
    private static final class SystemAccumulator {
        String systemName;
        long systemAddress;
        Integer totalBodies;
        Integer nonBodyCount;
        Double fssProgress;

        final Map<Integer, BodyInfo> bodies = new HashMap<>();

        SystemAccumulator(String systemName, long systemAddress) {
            this.systemName = systemName;
            this.systemAddress = systemAddress;
        }

        void applyLocation(LocationEvent e) {
            if (e.getStarSystem() != null) {
                systemName = e.getStarSystem();
            }
            if (e.getSystemAddress() != 0L) {
                systemAddress = e.getSystemAddress();
            }
        }

        void applyFsdJump(FsdJumpEvent e) {
            if (e.getStarSystem() != null) {
                systemName = e.getStarSystem();
            }
            if (e.getSystemAddress() != 0L) {
                systemAddress = e.getSystemAddress();
            }
        }

        void applyFssDiscovery(FssDiscoveryScanEvent e) {
            fssProgress = e.getProgress();
            totalBodies = e.getBodyCount();
            nonBodyCount = e.getNonBodyCount();
            if (e.getSystemName() != null && !e.getSystemName().isEmpty()) {
                systemName = e.getSystemName();
            }
            if (e.getSystemAddress() != 0L) {
                systemAddress = e.getSystemAddress();
            }
        }

        void applyScan(ScanEvent e) {
            String bodyName = e.getBodyName();
            if (isBeltOrRing(bodyName)) {
                return;
            }
            int id = e.getBodyId();
            if (id < 0) {
                return;
            }
            BodyInfo info = bodies.computeIfAbsent(id, ignored -> new BodyInfo());
            info.bodyId = id;
            info.name = bodyName;
            info.distanceLs = e.getDistanceFromArrivalLs();
            info.landable = e.isLandable();
            info.gravityMS = e.getSurfaceGravity();
            info.atmoOrType = chooseAtmoOrType(e);
            info.highValue = isHighValue(e);
        }

        void applySignals(int bodyId, List<SaasignalsFoundEvent.Signal> signals) {
            if (bodyId < 0 || signals == null || signals.isEmpty()) {
                return;
            }
            BodyInfo info = bodies.computeIfAbsent(bodyId, ignored -> new BodyInfo());
            for (SaasignalsFoundEvent.Signal s : signals) {
                String type = toLower(s.getType());
                String loc = toLower(s.getTypeLocalised());
                if (type.contains("biological") || loc.contains("biological")) {
                    info.hasBio = true;
                } else if (type.contains("geological") || loc.contains("geological")) {
                    info.hasGeo = true;
                }
            }
        }

        private static String chooseAtmoOrType(ScanEvent e) {
            String atmo = e.getAtmosphere();
            if (atmo != null && !atmo.isEmpty()) {
                return atmo;
            }
            String pc = e.getPlanetClass();
            if (pc != null && !pc.isEmpty()) {
                return pc;
            }
            String star = e.getStarType();
            return star != null ? star : "";
        }

        private static boolean isHighValue(ScanEvent e) {
            String pc = toLower(e.getPlanetClass());
            String tf = toLower(e.getTerraformState());
            if (pc.contains("earth-like")) {
                return true;
            }
            if (pc.contains("water world")) {
                return true;
            }
            if (pc.contains("ammonia world")) {
                return true;
            }
            return tf.contains("terraformable");
        }

        private static boolean isBeltOrRing(String bodyName) {
            if (bodyName == null) {
                return false;
            }
            String lower = bodyName.toLowerCase(Locale.ROOT);
            return lower.contains("belt") || lower.contains("ring");
        }

        private static String toLower(String s) {
            return s == null ? "" : s.toLowerCase(Locale.ROOT);
        }

        List<SystemCache.CachedBody> toCachedBodies() {
            List<SystemCache.CachedBody> list = new ArrayList<>();
            for (BodyInfo b : bodies.values()) {
                SystemCache.CachedBody cb = new SystemCache.CachedBody();
                cb.name = b.name;
                cb.bodyId = b.bodyId;
                cb.distanceLs = b.distanceLs;
                cb.gravityMS = b.gravityMS;
                cb.landable = b.landable;
                cb.hasBio = b.hasBio;
                cb.hasGeo = b.hasGeo;
                cb.highValue = b.highValue;
                cb.atmoOrType = b.atmoOrType;
                list.add(cb);
            }
            return list;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Rescanning Elite Dangerous journals and rebuilding local system cache...");

        EliteJournalReader reader = new EliteJournalReader();

        // Big number: effectively "all logs" – the reader will cap it by actual file count.
        List<EliteLogEvent> events = reader.readEventsFromLastNJournalFiles(Integer.MAX_VALUE);
        System.out.println("Loaded " + events.size() + " events from journal files.");

        Map<SystemKey, SystemAccumulator> systems = new HashMap<>();

        String currentSystemName = null;
        long currentSystemAddress = 0L;

        for (EliteLogEvent event : events) {
            if (event instanceof LocationEvent) {
                LocationEvent le = (LocationEvent) event;
                currentSystemName = le.getStarSystem();
                currentSystemAddress = le.getSystemAddress();
                SystemKey key = new SystemKey(currentSystemAddress, currentSystemName);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyLocation(le);

            } else if (event instanceof FsdJumpEvent) {
                FsdJumpEvent je = (FsdJumpEvent) event;
                currentSystemName = je.getStarSystem();
                currentSystemAddress = je.getSystemAddress();
                SystemKey key = new SystemKey(currentSystemAddress, currentSystemName);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(k.systemName, k.systemAddress));
                acc.applyFsdJump(je);

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

            } else if (event instanceof ScanEvent) {
                ScanEvent se = (ScanEvent) event;
                long addr = se.getSystemAddress();
                String name = currentSystemName;
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(key.systemName, key.systemAddress));
                acc.applyScan(se);

            } else if (event instanceof SaasignalsFoundEvent) {
                SaasignalsFoundEvent sf = (SaasignalsFoundEvent) event;
                long addr = sf.getSystemAddress();
                String name = currentSystemName;
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(key.systemName, key.systemAddress));
                acc.applySignals(sf.getBodyId(), sf.getSignals());

            } else if (event instanceof EliteLogEvent.FssBodySignalsEvent) {
                EliteLogEvent.FssBodySignalsEvent fb = (EliteLogEvent.FssBodySignalsEvent) event;
                long addr = fb.getSystemAddress();
                String name = currentSystemName;
                if (addr == 0L) {
                    addr = currentSystemAddress;
                }
                SystemKey key = new SystemKey(addr, name);
                SystemAccumulator acc = systems.computeIfAbsent(
                        key, k -> new SystemAccumulator(key.systemName, key.systemAddress));
                acc.applySignals(fb.getBodyId(), fb.getSignals());
            }
        }

        SystemCache cache = SystemCache.getInstance();
        int systemCount = 0;
        int bodyCount = 0;

        for (SystemAccumulator acc : systems.values()) {
            if (acc.bodies.isEmpty()) {
                continue;
            }
            List<SystemCache.CachedBody> bodies = acc.toCachedBodies();
            cache.put(acc.systemAddress, acc.systemName, bodies);
            systemCount++;
            bodyCount += bodies.size();
        }

        System.out.println("Rescan complete. Cached " + bodyCount + " bodies in " + systemCount + " systems.");
    }
}
