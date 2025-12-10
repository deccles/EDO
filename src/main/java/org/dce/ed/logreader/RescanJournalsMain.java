package org.dce.ed.logreader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dce.ed.cache.CachedBody;
import org.dce.ed.cache.SystemCache;
import org.dce.ed.logreader.EliteLogEvent.FsdJumpEvent;
import org.dce.ed.logreader.EliteLogEvent.FssAllBodiesFoundEvent;
import org.dce.ed.logreader.EliteLogEvent.FssBodySignalsEvent;
import org.dce.ed.logreader.EliteLogEvent.FssDiscoveryScanEvent;
import org.dce.ed.logreader.EliteLogEvent.LocationEvent;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent.Genus;
import org.dce.ed.logreader.EliteLogEvent.SaasignalsFoundEvent.Signal;
import org.dce.ed.logreader.EliteLogEvent.ScanEvent;
import org.dce.ed.logreader.EliteLogEvent.ScanOrganicEvent;
import org.dce.ed.state.BodyInfo;

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

    private static final class SystemAccumulator {
        String systemName;
        long systemAddress;
        Integer totalBodies;
        Integer nonBodyCount;
        Double fssProgress;
        Boolean allBodiesFound;

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

        void applyFssAllBodiesFound(FssAllBodiesFoundEvent e) {
            // Prefer system info from the event if present
            if (e.getSystemName() != null && !e.getSystemName().isEmpty()) {
                systemName = e.getSystemName();
            }
            if (e.getSystemAddress() != 0L) {
                systemAddress = e.getSystemAddress();
            }
            if (e.getBodyCount() > 0) {
                totalBodies = e.getBodyCount();
            }
            allBodiesFound = Boolean.TRUE;
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
            info.setBodyId(id);
            info.setName(bodyName);

            // Distance / landability / gravity
            info.setDistanceLs(e.getDistanceFromArrivalLs());
            info.setLandable(e.isLandable());
            info.setGravityMS(e.getSurfaceGravity());

            // Atmosphere / planet type used by prediction code
            info.setAtmoOrType(chooseAtmoOrType(e));

            // High-value flag for ELW / WW / AW / terraformable HMC etc.
            info.setHighValue(isHighValue(e));

            // Prediction-related attributes
            info.setPlanetClass(e.getPlanetClass());
            info.setAtmosphere(e.getAtmosphere());

            Double temp = e.getSurfaceTemperature();
            if (temp != null) {
                info.setSurfaceTempK(temp);
            }

            String volc = e.getVolcanism();
            if (volc != null && !volc.isEmpty()) {
                info.setVolcanism(volc);
            }
        }

        void applySignals(int bodyId, List<Signal> signals) {
            if (bodyId < 0 || signals == null || signals.isEmpty()) {
                return;
            }

            BodyInfo info = bodies.computeIfAbsent(bodyId, ignored -> new BodyInfo());
            for (Signal s : signals) {
                String type = toLower(s.getType());
                String loc = toLower(s.getTypeLocalised());
                if (type.contains("biological") || loc.contains("biological")) {
                    info.setHasBio(true);
                } else if (type.contains("geological") || loc.contains("geological")) {
                    info.setHasGeo(true);
                }
            }
        }

        void applyGenuses(int bodyId, List<Genus> genuses) {
            if (bodyId < 0 || genuses == null || genuses.isEmpty()) {
                return;
            }
            BodyInfo info = bodies.computeIfAbsent(bodyId, ignored -> new BodyInfo());
            if (info.getObservedGenusPrefixes() == null) {
                info.setObservedGenusPrefixes(new HashSet<>());
            }
            for (Genus g : genuses) {
                String genusName = toLower(g.getGenusLocalised());
                if (genusName.isEmpty()) {
                    genusName = toLower(g.getGenus());
                }
                if (!genusName.isEmpty()) {
                    info.getObservedGenusPrefixes().add(genusName);
                }
            }
        }

        void applyScanOrganic(ScanOrganicEvent e) {
            String bodyName = e.getBodyName();
            int bodyId = e.getBodyId();

            BodyInfo info = findOrCreateBodyByIdOrName(bodyId, bodyName);
            if (info == null) {
                return;
            }

            info.setHasBio(true);

            // If the ScanOrganic has a body name and the scan body didn't, fill it in.
            if (bodyName != null && !bodyName.isEmpty()
                    && (info.getName() == null || info.getName().isEmpty())) {
                info.setName(bodyName);
            }

            // Genus prefix (for narrowing predictions)
            String genusPrefix = toLower(e.getGenusLocalised());
            if (genusPrefix.isEmpty()) {
                genusPrefix = toLower(e.getGenus());
            }
            if (!genusPrefix.isEmpty()) {
                if (info.getObservedGenusPrefixes() == null) {
                    info.setObservedGenusPrefixes(new HashSet<>());
                }
                info.getObservedGenusPrefixes().add(genusPrefix);
            }

            // Full display name (truth row): "Tussock Serrati", "Bacterium Nebulus", etc.
            String genusDisp = firstNonEmpty(e.getGenusLocalised(), e.getGenus());
            String speciesDisp = firstNonEmpty(e.getSpeciesLocalised(), e.getSpecies());
            String displayName = buildDisplayName(genusDisp, speciesDisp);

            if (displayName != null && !displayName.isEmpty()) {
                if (info.getObservedBioDisplayNames() == null) {
                    info.setObservedBioDisplayNames(new HashSet<>());
                }
                info.getObservedBioDisplayNames().add(displayName);
            }
        }

        private BodyInfo findOrCreateBodyByIdOrName(int bodyId, String bodyName) {
            if (bodyId > 0) {
                BodyInfo existing = bodies.get(bodyId);
                if (existing != null) {
                    return existing;
                }
                BodyInfo info = new BodyInfo();
                info.setBodyId(bodyId);
                if (bodyName != null && !bodyName.isEmpty()) {
                    info.setName(bodyName);
                }
                bodies.put(bodyId, info);
                return info;
            }

            String name = bodyName != null ? bodyName.trim() : "";
            if (!name.isEmpty()) {
                for (BodyInfo b : bodies.values()) {
                    if (name.equals(b.getName())) {
                        return b;
                    }
                }
                BodyInfo info = new BodyInfo();
                info.setBodyId(-1);
                info.setName(bodyName);
                bodies.put(bodies.size() + 1000000, info);
                return info;
            }

            return null;
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
            if (pc.contains("ammonia world")) {
                return true;
            }
            if (pc.contains("water world")) {
                return true;
            }
            if (tf.contains("terraformable")) {
                return true;
            }
            return false;
        }

        private static boolean isBeltOrRing(String bodyName) {
            if (bodyName == null) {
                return false;
            }
            String lower = bodyName.toLowerCase(Locale.ROOT);
            return lower.contains("belt cluster")
                    || lower.contains("belt ")
                    || lower.contains(" ring");
        }

        private static String toLower(String s) {
            return s == null ? "" : s.toLowerCase(Locale.ROOT);
        }

        private static boolean isEmpty(String s) {
            return s == null || s.isEmpty();
        }

        private static String firstNonEmpty(String a, String b) {
            if (!isEmpty(a)) {
                return a;
            }
            if (!isEmpty(b)) {
                return b;
            }
            return null;
        }

        private static String buildDisplayName(String genusDisp, String speciesDisp) {
            if ((genusDisp == null || genusDisp.isEmpty())
                    && (speciesDisp == null || speciesDisp.isEmpty())) {
                return null;
            }
            if (genusDisp == null || genusDisp.isEmpty()) {
                return speciesDisp;
            }
            if (speciesDisp == null || speciesDisp.isEmpty()) {
                return genusDisp;
            }
            return genusDisp + " " + speciesDisp;
        }

        List<CachedBody> toCachedBodies() {
            List<CachedBody> list = new ArrayList<>();
            for (BodyInfo b : bodies.values()) {
                CachedBody cb = new CachedBody();
                cb.name = b.getName();
                cb.bodyId = b.getBodyId();
                cb.distanceLs = b.getDistanceLs();
                cb.gravityMS = b.getGravityMS();
                cb.landable = b.isLandable();
                cb.hasBio = b.isHasBio();
                cb.hasGeo = b.isHasGeo();
                cb.highValue = b.isHighValue();

                cb.planetClass = b.getPlanetClass();
                cb.atmosphere = b.getAtmosphere();
                cb.atmoOrType = b.getAtmoOrType();
                cb.surfaceTempK = b.getSurfaceTempK();
                cb.volcanism = b.getVolcanism();
                cb.discoveryCommander = b.getDiscoveryCommander();

                if (b.getObservedGenusPrefixes() != null
                        && !b.getObservedGenusPrefixes().isEmpty()) {
                    cb.observedGenusPrefixes = new HashSet<>(b.getObservedGenusPrefixes());
                }

                if (b.getObservedBioDisplayNames() != null
                        && !b.getObservedBioDisplayNames().isEmpty()) {
                    cb.observedBioDisplayNames = new HashSet<>(b.getObservedBioDisplayNames());
                }

                list.add(cb);
            }
            return list;
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

        EliteJournalReader reader = new EliteJournalReader();

        boolean forceFull = false;
        if (args != null) {
            for (String arg : args) {
                if ("--full".equalsIgnoreCase(arg)) {
                    forceFull = true;
                    break;
                }
            }
        }

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

        List<EliteLogEvent> allEvents = reader.readEventsFromLastNJournalFiles(Integer.MAX_VALUE);
        List<EliteLogEvent> events;
        if (lastImport == null) {
            events = allEvents;
        } else {
            events = new ArrayList<>();
            for (EliteLogEvent e : allEvents) {
                Instant ts = e.getTimestamp();
                if (ts != null && ts.isAfter(lastImport)) {
                    events.add(e);
                }
            }
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
                acc.applySignals(fb.getBodyId(), fb.getSignals());

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
                acc.applyGenuses(sf.getBodyId(), sf.getGenuses());

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
            if (acc.bodies.isEmpty()) {
                continue;
            }
            List<CachedBody> bodies = acc.toCachedBodies();

            cache.put(
                acc.systemAddress,
                acc.systemName,
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
