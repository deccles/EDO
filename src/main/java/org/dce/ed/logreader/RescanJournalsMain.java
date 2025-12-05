package org.dce.ed.logreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.dce.ed.SystemCache;
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
     * This is intentionally kept in sync with the parsing logic used by the live
     * overlay (SystemEventProcessor/SystemState).
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

        // Fields used for prediction/exobiology
        String planetClass;
        String atmosphere;
        Double surfaceTempK;
        String volcanism;

        // Truth data from DSS / ScanOrganic
        Set<String> observedGenusPrefixes;    // e.g. "bacterium", "stratum"
        Set<String> observedBioDisplayNames;  // e.g. "Bacterium Nebulus"
    }

    /**
     * Accumulates events for a single system and converts them to CachedBody entries.
     */
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

            // Count in this event is the total number of bodies in the system
            if (e.getBodyCount() > 0) {
                totalBodies = e.getBodyCount();
            }

            // Mark this system as fully scanned
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
            info.bodyId = id;
            info.name = bodyName;
            info.distanceLs = e.getDistanceFromArrivalLs();
            info.landable = e.isLandable();
            info.gravityMS = e.getSurfaceGravity();
            info.atmoOrType = chooseAtmoOrType(e);
            info.highValue = isHighValue(e);

            // Prediction-related attributes
            info.planetClass = e.getPlanetClass();
            info.atmosphere = e.getAtmosphere();

            Double temp = e.getSurfaceTemperature();
            if (temp != null) {
                info.surfaceTempK = temp;
            }

            String volc = e.getVolcanism();
            if (volc != null && !volc.isEmpty()) {
                info.volcanism = volc;
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
                    info.hasBio = true;
                } else if (type.contains("geological") || loc.contains("geological")) {
                    info.hasGeo = true;
                }
            }
        }

        void applyGenuses(int bodyId, List<Genus> genuses) {
            if (bodyId < 0 || genuses == null || genuses.isEmpty()) {
                return;
            }
            BodyInfo info = bodies.computeIfAbsent(bodyId, ignored -> new BodyInfo());
            if (info.observedGenusPrefixes == null) {
                info.observedGenusPrefixes = new HashSet<>();
            }
            for (Genus g : genuses) {
                String genusName = toLower(g.getGenusLocalised());
                if (genusName.isEmpty()) {
                    genusName = toLower(g.getGenus());
                }
                if (!genusName.isEmpty()) {
                    info.observedGenusPrefixes.add(genusName);
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

            info.hasBio = true;

            // If the ScanOrganic has a body name and the scan body didn't, fill it in.
            if (bodyName != null && !bodyName.isEmpty() && (info.name == null || info.name.isEmpty())) {
                info.name = bodyName;
            }

            // Genus prefix (for narrowing predictions)
            String genusPrefix = toLower(e.getGenusLocalised());
            if (genusPrefix.isEmpty()) {
                genusPrefix = toLower(e.getGenus());
            }
            if (!genusPrefix.isEmpty()) {
                if (info.observedGenusPrefixes == null) {
                    info.observedGenusPrefixes = new HashSet<>();
                }
                info.observedGenusPrefixes.add(genusPrefix);
            }

            // Full display name (truth row): "Bacterium Nebulus", etc.
         // Full display name (truth row): "Tussock Serrati", "Bacterium Nebulus", etc.
            String genusDisp = firstNonEmpty(e.getGenusLocalised(), e.getGenus());
            String speciesDisp = firstNonEmpty(e.getSpeciesLocalised(), e.getSpecies());
            String displayName = buildDisplayName(genusDisp, speciesDisp);

            if (!isEmpty(displayName)) {
                if (info.observedBioDisplayNames == null) {
                    info.observedBioDisplayNames = new HashSet<>();
                }
                info.observedBioDisplayNames.add(displayName);
            }
        }
        private static String buildDisplayName(String genusDisp, String speciesDisp) {
            // If we have nothing, return null
            if (isEmpty(genusDisp) && isEmpty(speciesDisp)) {
                return null;
            }
            // If only one side is present, just use that
            if (isEmpty(genusDisp)) {
                return speciesDisp;
            }
            if (isEmpty(speciesDisp)) {
                return genusDisp;
            }

            String genusTrim = genusDisp.trim();
            String speciesTrim = speciesDisp.trim();

            // Species often already includes the genus, e.g. "Tussock Serrati" with genus "Tussock".
            // In that case, drop the leading genus so we don't get "Tussock Tussock Serrati".
            String[] parts = speciesTrim.split("\\s+");
            if (parts.length > 0 && parts[0].equalsIgnoreCase(genusTrim)) {
                if (parts.length == 1) {
                    // Species was just "Tussock" – effectively only genus
                    return genusTrim;
                }
                String epithets = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                return genusTrim + " " + epithets;
            }

            // Normal case: species is just the epithet, e.g. "Serrati"
            return genusTrim + " " + speciesTrim;
        }

        private BodyInfo findOrCreateBodyByIdOrName(int bodyId, String bodyName) {
            if (bodyId >= 0) {
                BodyInfo info = bodies.get(bodyId);
                if (info == null) {
                    info = new BodyInfo();
                    info.bodyId = bodyId;
                    info.name = bodyName;
                    bodies.put(bodyId, info);
                } else if (bodyName != null && !bodyName.isEmpty() && (info.name == null || info.name.isEmpty())) {
                    info.name = bodyName;
                }
                return info;
            }

            if (bodyName != null && !bodyName.isEmpty()) {
                for (BodyInfo b : bodies.values()) {
                    if (bodyName.equals(b.name)) {
                        return b;
                    }
                }
                BodyInfo info = new BodyInfo();
                info.bodyId = -1;
                info.name = bodyName;
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
            if (pc.contains("water world")) {
                return true;
            }
            if (pc.contains("ammonia world")) {
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

                cb.planetClass = b.planetClass;
                cb.atmosphere = b.atmosphere;
                cb.atmoOrType = b.atmoOrType;
                cb.surfaceTempK = b.surfaceTempK;
                cb.volcanism = b.volcanism;

                if (b.observedGenusPrefixes != null && !b.observedGenusPrefixes.isEmpty()) {
                    cb.observedGenusPrefixes = new HashSet<>(b.observedGenusPrefixes);
                }

                if (b.observedBioDisplayNames != null && !b.observedBioDisplayNames.isEmpty()) {
                    cb.observedBioDisplayNames = new HashSet<>(b.observedBioDisplayNames);
                }

                list.add(cb);
            }
            return list;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Rescanning Elite Dangerous journals and rebuilding local system cache...");

        EliteJournalReader reader = new EliteJournalReader();

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
            } else if (event instanceof FssAllBodiesFoundEvent) {          // NEW
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
                acc.applySignals(sf.getBodyId(), sf.getSignals());
                acc.applyGenuses(sf.getBodyId(), sf.getGenuses());

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
            List<SystemCache.CachedBody> bodies = acc.toCachedBodies();

            cache.put(
                acc.systemAddress,
                acc.systemName,
                acc.totalBodies,
                acc.nonBodyCount,
                acc.fssProgress,
                acc.allBodiesFound,          // allBodiesFound (Rescan doesn’t compute this yet)
                bodies
            );
            systemCount++;
            bodyCount += bodies.size();
        }

        System.out.println("Rescan complete. Cached " + bodyCount + " bodies in " + systemCount + " systems.");
    }
}
