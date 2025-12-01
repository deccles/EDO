package org.dce.ed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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

/**
 * Simple on-disk cache of system bodies, similar in spirit to what
 * tools like Exploration Buddy do. The cache is keyed by system
 * address (when available) and falls back to system name.
 *
 * Data is persisted as JSON in the user's home directory so that
 * previously scanned systems can be shown immediately on future runs.
 */
public final class SystemCache {
    // Remember the last system we saw in the cache file so callers
    // can easily restore "whatever was loaded last" at startup.
    private CachedSystem lastLoadedSystem;

    private static final String CACHE_FILE_NAME = ".edOverlaySystems.json";

    private static final SystemCache INSTANCE = new SystemCache();

    private final Gson gson;
    private final Path cachePath;

    // In-memory maps
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
    
    /**
     * Represents one body as stored in the cache.
     */
    public static final class CachedBody {
        public String name;
        public int bodyId;
        public double distanceLs;
        public Double gravityMS;
        public boolean landable;
        public boolean hasBio;
        public boolean hasGeo;
        public boolean highValue;
        // extra fields used by SystemTabPanel for exobiology prediction / display
        public String planetClass;
        public String atmosphere;
        public String atmoOrType;
    }

    /**
     * Represents a cached system and its bodies.
     */
    public static final class CachedSystem {
        public long systemAddress;
        public String systemName;
        public List<CachedBody> bodies = new ArrayList<>();
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
                // NEW: remember the last one we saw
                lastLoadedSystem = cs;
            }
        } catch (IOException ex) {
            // ignore; cache will just start empty
        }
    }

    /**
     * Returns a cached system by address or (fallback) name.
     */
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
    public synchronized void put(long systemAddress, String systemName, List<CachedBody> bodies) {
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
        cs.bodies = (bodies != null) ? new ArrayList<>(bodies) : new ArrayList<>();

        if (systemAddress != 0L) {
            byAddress.put(systemAddress, cs);
        }
        if (systemName != null && !systemName.isEmpty()) {
            byName.put(systemName, cs);
        }

        save();
    }

    private synchronized void save() {
        try {
            List<CachedSystem> systems = new ArrayList<>();
            // Use a map to avoid duplicates if a system appears under both keys
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
}
