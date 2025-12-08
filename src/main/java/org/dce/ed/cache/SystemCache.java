package org.dce.ed.cache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dce.ed.edsm.BodiesResponse;
import org.dce.ed.edsm.EdsmClient;
import org.dce.ed.state.BodyInfo;
import org.dce.ed.state.SystemState;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Simple on-disk cache of system bodies, similar in spirit to what
 * tools like Exploration Buddy do. The cache is keyed by system
 * address (when available) and falls back to system name.
 *
 * Data is persisted as JSON in the user's home directory so that
 * previously scanned systems can be shown immediately on future runs.
 */
public final class SystemCache {
    private CachedSystem lastLoadedSystem;

    private static final String CACHE_FILE_NAME = ".edOverlaySystems.json";

    private static final SystemCache INSTANCE = new SystemCache();

    private final Gson gson;
    private final Path cachePath;

    private final Map<Long, CachedSystem> byAddress = new HashMap<>();
    private final Map<String, CachedSystem> byName = new HashMap<>();

    private boolean loaded = false;
    
    private SystemCache() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create();
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            home = ".";
        }
        this.cachePath = Paths.get(home, CACHE_FILE_NAME);
    }

    public static SystemCache getInstance() {
        return INSTANCE;
    }

    public static CachedSystem load() throws IOException {
        SystemCache cache = getInstance();
        cache.ensureLoaded();
        return cache.lastLoadedSystem;
    }


    private synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        if (!Files.isRegularFile(cachePath)) {
            return;
        }

        System.out.println("SystemCache: loading from " + cachePath.toAbsolutePath());

        try (BufferedReader reader = Files.newBufferedReader(cachePath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<CachedSystem>>() {}.getType();
            List<CachedSystem> systems = gson.fromJson(reader, type);
            if (systems == null) {
                return;
            }
            for (CachedSystem cs : systems) {
                if (cs == null) {
                    continue;
                }
                if (cs.systemAddress != 0L) {
                    byAddress.put(cs.systemAddress, cs);
                }
                if (cs.systemName != null && !cs.systemName.isEmpty()) {
                    byName.put(cs.systemName, cs);
                }
                lastLoadedSystem = cs;
            }
        } catch (IOException ex) {
            // ignore; cache will just start empty
        }
    }

    public synchronized CachedSystem get(long systemAddress, String systemName) {
        ensureLoaded();

        CachedSystem cs = null;
        if (systemAddress != 0L) {
            cs = byAddress.get(systemAddress);
        }
        if (cs == null && systemName != null && !systemName.isEmpty()) {
            cs = byName.get(systemName);
        }
        return cs;
    }

    /**
     * Stores/updates a cached system and persists to disk.
     */
    public synchronized void put(long systemAddress,
            String systemName,
            Integer totalBodies,
            Integer nonBodyCount,
            Double fssProgress,
            Boolean allBodiesFound,
            List<CachedBody> bodies) {
        ensureLoaded();

        if ((systemAddress == 0L) && (systemName == null || systemName.isEmpty())) {
            return;
        }

        CachedSystem cs = get(systemAddress, systemName);
        if (cs == null) {
            cs = new CachedSystem();
        }

        cs.systemAddress = systemAddress;
        cs.systemName = systemName;
        cs.totalBodies = totalBodies;
        cs.nonBodyCount = nonBodyCount;
        cs.fssProgress = fssProgress;
        cs.allBodiesFound = allBodiesFound;
        cs.bodies = (bodies != null) ? new ArrayList<CachedBody>(bodies) : new ArrayList<>();

        if (systemAddress != 0L) {
            byAddress.put(systemAddress, cs);
        }
        if (systemName != null && !systemName.isEmpty()) {
            byName.put(systemName, cs);
        }

        save();
    }
    public void loadInto(SystemState state, CachedSystem cs) {
        if (state == null || cs == null) {
            return;
        }

        state.setSystemName(cs.systemName);
        state.setSystemAddress(cs.systemAddress);
        state.setVisitedByMe(true);
        
        state.resetBodies();

        state.setTotalBodies(cs.totalBodies);
        state.setNonBodyCount(cs.nonBodyCount);
        state.setFssProgress(cs.fssProgress);
        state.setAllBodiesFound(cs.allBodiesFound);
        
        for (CachedBody cb : cs.bodies) {
            BodyInfo info = new BodyInfo();
            info.setName(cb.name);
            info.setShortName(state.computeShortName(cb.name));
            info.setBodyId(cb.bodyId);
            info.setDistanceLs(cb.distanceLs);
            info.setGravityMS(cb.gravityMS);
            info.setLandable(cb.landable);
            info.setHasBio(cb.hasBio);
            info.setHasGeo(cb.hasGeo);
            info.setHighValue(cb.highValue);
            info.setAtmoOrType(cb.atmoOrType);
            info.setPlanetClass(cb.planetClass);
            info.setAtmosphere(cb.atmosphere);
            info.setSurfaceTempK(cb.surfaceTempK);
            info.setVolcanism(cb.volcanism);
            info.setDiscoveryCommander(cb.discoveryCommander);
            
            if (cb.observedGenusPrefixes != null && !cb.observedGenusPrefixes.isEmpty()) {
                info.setObservedGenusPrefixes(new java.util.HashSet<>(cb.observedGenusPrefixes));
            }
            if (cb.observedBioDisplayNames != null && !cb.observedBioDisplayNames.isEmpty()) {
                info.setObservedBioDisplayNames(new java.util.HashSet<>(cb.observedBioDisplayNames));
            }
            state.getBodies().put(info.getBodyId(), info);
        }
    }
    /**
     * Merge EDSM bodies information into the current SystemState.
     *
     * We treat BodiesResponse as the single source of truth for what EDSM
     * knows about this system and enrich our local SystemState where we
     * have gaps (bodyCount, discovery commander, some physical attributes).
     *
     * This does NOT create new BodyInfo entries yet; it only decorates
     * bodies that already exist in the SystemState and matches by name.
     */
    public void mergeBodiesFromEdsm(SystemState state, BodiesResponse edsm) {
        if (state == null || edsm == null) {
            return;
        }

        // If we don't already know how many bodies there are, use EDSM's list size.
        if (state.getTotalBodies() == null && edsm.bodies != null) {
            state.setTotalBodies(edsm.bodies.size());
        }

        if (edsm.bodies == null || edsm.bodies.isEmpty()) {
            return;
        }

        Map<Integer, BodyInfo> localBodies = state.getBodies();
        if (localBodies == null || localBodies.isEmpty()) {
            // For now we only enrich existing bodies (from logs/cache).
            return;
        }

        for (BodyInfo local : localBodies.values()) {
            String localName = local.getName();
            if (localName == null || localName.isEmpty()) {
                continue;
            }

            BodiesResponse.Body remote = null;
            for (BodiesResponse.Body b : edsm.bodies) {
                if (localName.equals(b.name)) {
                    remote = b;
                    break;
                }
            }
            if (remote == null) {
                continue;
            }

            // ----- Discovery commander -----
            String existingCmdr = local.getDiscoveryCommander();
            if ((existingCmdr == null || existingCmdr.isEmpty())
                    && remote.discovery.commander != null
                    && !remote.discovery.commander.isEmpty()) {
                local.setDiscoveryCommander(remote.discovery.commander);
            }

            // ----- Distance to arrival (Ls) -----
//            if (local.getDistanceLs() == null && remote.distanceToArrival != null) {
                local.setDistanceLs(remote.distanceToArrival);
//            }

            // ----- Surface temperature (K) -----
            if (local.getSurfaceTempK() == null && remote.surfaceTemperature != null) {
                local.setSurfaceTempK(remote.surfaceTemperature);
            }

            // ----- Volcanism string -----
            if (local.getVolcanism() == null
                    && remote.volcanismType != null
                    && !remote.volcanismType.isEmpty()) {
                local.setVolcanism(remote.volcanismType);
            }

            // ----- Atmosphere description -----
            if (local.getAtmosphere() == null
                    && remote.atmosphereType != null
                    && !remote.atmosphereType.isEmpty()) {
                local.setAtmosphere(remote.atmosphereType);
            }

            // NOTE: we intentionally do NOT touch landable / gravity / etc yet,
            // because BodyInfo may already be populated from logs and we don't
            // have a clear "unknown" sentinel. Easy to extend later if you want.
        }
    }

    public void storeSystem(SystemState state) {
        if (state == null || state.getSystemName() == null || state.getSystemAddress() == 0L) {
            return;
        }

        List<CachedBody> list = new ArrayList<>();

        for (BodyInfo b : state.getBodies().values()) {
            CachedBody cb = new CachedBody();
            cb.name = b.getName();
            cb.bodyId = b.getBodyId();
            cb.distanceLs = b.getDistanceLs();
            cb.gravityMS = b.getGravityMS();
            cb.landable = b.isLandable();
            cb.hasBio = b.hasBio();
            cb.hasGeo = b.hasGeo();
            cb.highValue = b.isHighValue();
            cb.atmoOrType = b.getAtmoOrType();
            cb.planetClass = b.getPlanetClass();
            cb.atmosphere = b.getAtmosphere();
            cb.surfaceTempK = b.getSurfaceTempK();
            cb.volcanism = b.getVolcanism();
            cb.discoveryCommander = b.getDiscoveryCommander();
            
            if (b.getObservedGenusPrefixes() != null && !b.getObservedGenusPrefixes().isEmpty()) {
                cb.observedGenusPrefixes = new java.util.HashSet<>(b.getObservedGenusPrefixes());
            }

            if (b.getObservedGenusPrefixes() != null && !b.getObservedGenusPrefixes().isEmpty()) {
                cb.observedGenusPrefixes = new java.util.HashSet<>(b.getObservedGenusPrefixes());
            } else {
                cb.observedGenusPrefixes = null;
            }

            if (b.getObservedBioDisplayNames() != null && !b.getObservedBioDisplayNames().isEmpty()) {
                cb.observedBioDisplayNames = new java.util.HashSet<>(b.getObservedBioDisplayNames());
            } else {
                cb.observedBioDisplayNames = null;
            }
            
            list.add(cb);
        }
        put(state.getSystemAddress(),
                state.getSystemName(),
                state.getTotalBodies(),
                state.getNonBodyCount(),
                state.getFssProgress(),
                state.getAllBodiesFound(),
                list);
    }

    
    private synchronized void save() {
        try {
            List<CachedSystem> systems = new ArrayList<>();

            Map<String, CachedSystem> unique = new HashMap<>();
            for (CachedSystem cs : byAddress.values()) {
                if (cs.systemName != null) {
                    unique.put(cs.systemName, cs);
                } else {
                    unique.put("addr:" + cs.systemAddress, cs);
                }
            }
            for (CachedSystem cs : byName.values()) {
                if (cs.systemName != null) {
                    unique.put(cs.systemName, cs);
                }
            }
            systems.addAll(unique.values());

            try (BufferedWriter writer = Files.newBufferedWriter(cachePath, StandardCharsets.UTF_8)) {
                gson.toJson(systems, writer);
            }
        } catch (IOException ex) {
            // ignore; cache is best-effort
        }
    }
    
    /**
     * Merge discovery information (from EDSM or other sources) into the
     * current SystemState, keyed by full body name.
     *
     * If a body already has a discoveryCommander set, we leave it alone.
     * If EDSM says "this body has some discovery commander", we set a
     * placeholder so that downstream logic can treat it as "discovered".
     */
    public void mergeDiscoveryFlags(SystemState state, Map<String, Boolean> discoveryFlagsByBodyName) {
        if (state == null || discoveryFlagsByBodyName == null || discoveryFlagsByBodyName.isEmpty()) {
            return;
        }

        for (BodyInfo b : state.getBodies().values()) {
            Boolean has = discoveryFlagsByBodyName.get(b.getName());
            if (has == null || !has.booleanValue()) {
                continue;
            }

            String existing = b.getDiscoveryCommander();
            if (existing == null || existing.isEmpty()) {
                // We don't know the actual name from EDSM and don't care;
                // we just need "some discovery commander exists".
                b.setDiscoveryCommander("EDSM");
            }
        }
    }

    
}
