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

    
    
    
    
    /**
     * Merge EDSM bodies/discovery info into the current SystemState.
     *
     * Currently EdsmClient.BodiesScanInfo only exposes:
     *   - bodyCount
     *   - per-body name
     *   - per-body hasDiscoveryCommander
     *
     * So we:
     *   - fill state.totalBodies from EDSM when missing
     *   - for any existing BodyInfo with the same name, if it has no
     *     discoveryCommander yet and EDSM says there is one, mark it "EDSM".
     */
    public void mergeEdsmBodies(SystemState state, EdsmClient.BodiesScanInfo edsmInfo) {
        if (state == null || edsmInfo == null) {
            return;
        }

        // Use EDSM bodyCount if we don't already know it
        if (state.getTotalBodies() == null && edsmInfo.bodyCount != null) {
            state.setTotalBodies(edsmInfo.bodyCount);
        }

        if (edsmInfo.bodies == null || edsmInfo.bodies.isEmpty()) {
            return;
        }

        Map<Integer, BodyInfo> bodies = state.getBodies();
        if (bodies == null || bodies.isEmpty()) {
            // Right now we only decorate existing bodies; we don't invent new
            // BodyInfo instances because we don't have IDs or physical data.
            return;
        }

        for (BodyInfo local : bodies.values()) {
            String localName = local.getName();
            if (localName == null || localName.isEmpty()) {
                continue;
            }

            boolean hasDiscovery = false;
            for (EdsmClient.BodyDiscoveryInfo edsmBody : edsmInfo.bodies) {
                if (localName.equals(edsmBody.name)) {
                    hasDiscovery = edsmBody.hasDiscoveryCommander;
                    break;
                }
            }

            if (hasDiscovery) {
                String existingCmdr = local.getDiscoveryCommander();
                if (existingCmdr == null || existingCmdr.isEmpty()) {
                    local.setDiscoveryCommander("EDSM");
                }
            }
        }
    }
    
}
