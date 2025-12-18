package org.dce.ed.logreader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dce.ed.OverlayPreferences;
import org.dce.ed.logreader.event.StatusEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Tails the latest Elite Dangerous journal file and emits EliteLogEvent
 * objects to registered listeners in (roughly) real time.
 *
 * This class does NOT replay history on startup; the UI that wants
 * history (e.g. SystemTabPanel) should use EliteJournalReader once at
 * construction time, then rely on this monitor for new events only.
 */
public final class LiveJournalMonitor {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private Path statusFile;


    private Instant lastStatusTimestamp;
    private int lastStatusFlags  = Integer.MIN_VALUE;
    private int lastStatusFlags2 = Integer.MIN_VALUE;
    
    private static Map<String,LiveJournalMonitor> INSTANCE = new HashMap<String,LiveJournalMonitor>();

    private final CopyOnWriteArrayList<Consumer<EliteLogEvent>> listeners =
            new CopyOnWriteArrayList<>();

    private final EliteLogParser parser = new EliteLogParser();

    private volatile boolean running = false;
    private Thread workerThread;
    
	private String clientKey;

    private LiveJournalMonitor(String clientKey) {
    	this.clientKey = clientKey;
    }

    public static LiveJournalMonitor getInstance(String clientKey) {
        LiveJournalMonitor liveJournalMonitor = INSTANCE.get(clientKey);
        if (liveJournalMonitor == null) {
        	INSTANCE.put(clientKey,  new LiveJournalMonitor(clientKey));
        }
        return INSTANCE.get(clientKey);
    }

    public void addListener(Consumer<EliteLogEvent> listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        startIfNeeded();
    }

    public void removeListener(Consumer<EliteLogEvent> listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
    }

    public void shutdown() {
        running = false;
        Thread t = workerThread;
        if (t != null) {
            t.interrupt();
        }
    }

    private synchronized void startIfNeeded() {
        if (running) {
            return;
        }
        running = true;
        workerThread = new Thread(this::runLoop, "Elite-LiveJournalMonitor");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void runLoop() {
        Path journalDir = null;
        
        if (OverlayPreferences.isAutoLogDir(clientKey)) { 
        	journalDir = EliteLogFileLocator.findDefaultJournalDirectory();
        } else {
        	journalDir = Path.of(OverlayPreferences.getCustomLogDir(clientKey));
        }
        if (journalDir == null || !Files.isDirectory(journalDir)) {
            running = false;
            return;
        }

        // NEW: remember Status.json in the same directory
        statusFile = journalDir.resolve("Status.json");

        Path currentFile = null;
        long filePointer = 0L;

        while (running) {
            try {
                Path latest = findLatestJournalFile(journalDir);
                if (!Objects.equals(latest, currentFile)) {
                    // new or rotated file: start tailing from the end
                    currentFile = latest;
                    filePointer = (currentFile != null && Files.isRegularFile(currentFile))
                            ? Files.size(currentFile)
                            : 0L;
                }

                if (currentFile != null) {
                    filePointer = readFromFile(currentFile, filePointer);
                }

                // NEW: independently poll Status.json every tick
                pollStatusFile();

                try {
                    Thread.sleep(POLL_INTERVAL.toMillis());
                } catch (InterruptedException ie) {
                    if (!running) {
                        break;
                    }
                }
            } catch (Exception ex) {
                try {
                    Thread.sleep(POLL_INTERVAL.toMillis());
                } catch (InterruptedException ignored) {
                    // ignore
                }
            }
        }
    }
    /**
     * Poll Status.json in the journal directory.
     * When Flags / Flags2 change, emit a StatusFlagsEvent into the normal pipeline.
     */
    /**
     * Poll Status.json in the journal directory.
     * When Flags / Flags2 change, emit a StatusEvent into the normal pipeline.
     */
    private void pollStatusFile() {
        if (statusFile == null || !Files.isRegularFile(statusFile)) {
            return;
        }
        try {
            String json = Files.readString(statusFile, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) {
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // timestamp
            Instant ts = null;
            JsonElement tsEl = root.get("timestamp");
            if (tsEl != null && !tsEl.isJsonNull()) {
                try {
                    ts = Instant.parse(tsEl.getAsString());
                } catch (Exception ignored) {
                }
            }

            int flags = getIntOrDefault(root, "Flags", 0);
            int flags2 = getIntOrDefault(root, "Flags2", 0);

            // Only emit when flags actually change
            if (flags == lastStatusFlags && flags2 == lastStatusFlags2) {
                lastStatusTimestamp = ts;
                return;
            }
        	
            // Pips: [sys, eng, wep]
            int[] pips = new int[] { 0, 0, 0 };
            JsonElement pipsEl = root.get("Pips");
            if (pipsEl != null && pipsEl.isJsonArray()) {
                JsonArray arr = pipsEl.getAsJsonArray();
                for (int i = 0; i < Math.min(3, arr.size()); i++) {
                    try {
                        pips[i] = arr.get(i).getAsInt();
                    } catch (Exception ignored) {
                    }
                }
            }

            int fireGroup = getIntOrDefault(root, "FireGroup", 0);
            int guiFocus = getIntOrDefault(root, "GuiFocus", 0);

            double fuelMain = 0.0;
            double fuelReservoir = 0.0;
            JsonElement fuelEl = root.get("Fuel");
            if (fuelEl != null && fuelEl.isJsonObject()) {
                JsonObject fuel = fuelEl.getAsJsonObject();
                fuelMain = getDoubleOrDefault(fuel, "FuelMain", 0.0);
                fuelReservoir = getDoubleOrDefault(fuel, "FuelReservoir", 0.0);
            }

            double cargo = getDoubleOrDefault(root, "Cargo", 0.0);
            String legalState = getStringOrNull(root, "LegalState");
            long balance = getLongOrDefault(root, "Balance", 0L);

            // ---- Extra Status.json fields ----
            Double latitude = null;
            if (root.has("Latitude") && !root.get("Latitude").isJsonNull()) {
                try {
                    latitude = root.get("Latitude").getAsDouble();
                } catch (Exception ignored) {
                }
            }

            Double longitude = null;
            if (root.has("Longitude") && !root.get("Longitude").isJsonNull()) {
                try {
                    longitude = root.get("Longitude").getAsDouble();
                } catch (Exception ignored) {
                }
            }

            Double altitude = null;
            if (root.has("Altitude") && !root.get("Altitude").isJsonNull()) {
                try {
                    altitude = root.get("Altitude").getAsDouble();
                } catch (Exception ignored) {
                }
            }

            Double heading = null;
            if (root.has("Heading") && !root.get("Heading").isJsonNull()) {
                try {
                    heading = root.get("Heading").getAsDouble();
                } catch (Exception ignored) {
                }
            }

            String bodyName = getStringOrNull(root, "BodyName");

            Double planetRadius = null;
            if (root.has("PlanetRadius") && !root.get("PlanetRadius").isJsonNull()) {
                try {
                    planetRadius = root.get("PlanetRadius").getAsDouble();
                } catch (Exception ignored) {
                }
            }

            // ---- Destination block ----
            Long destSystem = null;
            Integer destBody = null;
            String destName = null;

            JsonElement destEl = root.get("Destination");
            if (destEl != null && destEl.isJsonObject()) {
                JsonObject dest = destEl.getAsJsonObject();

                JsonElement sysEl = dest.get("System");
                if (sysEl != null && !sysEl.isJsonNull()) {
                    try {
                        destSystem = sysEl.getAsLong();
                    } catch (Exception ignored) {
                    }
                }

                JsonElement bodyEl = dest.get("Body");
                if (bodyEl != null && !bodyEl.isJsonNull()) {
                    try {
                        destBody = bodyEl.getAsInt();
                    } catch (Exception ignored) {
                    }
                }

                JsonElement nameEl = dest.get("Name");
                if (nameEl != null && !nameEl.isJsonNull()) {
                    try {
                        destName = nameEl.getAsString();
                    } catch (Exception ignored) {
                    }
                }
            }

            // Build the StatusEvent using the extended constructor
            StatusEvent event =
                    new StatusEvent(
                            ts,
                            root,
                            flags,
                            flags2,
                            pips,
                            fireGroup,
                            guiFocus,
                            fuelMain,
                            fuelReservoir,
                            cargo,
                            legalState,
                            balance,
                            latitude,
                            longitude,
                            altitude,
                            heading,
                            bodyName,
                            planetRadius,
                            destSystem,
                            destBody,
                            destName
                    );

            // If you only care about “FSD charging for a jump to another system”:
            boolean isHyperjumpCharging =
                    event.isFsdCharging()
                            && event.isFsdHyperdriveCharging()
                            && event.getDestinationSystem() != null
                            && event.getDestinationSystem() != 0L;

//            if (isHyperjumpCharging) {
                dispatch(event);
//            }

            lastStatusTimestamp = ts;
            lastStatusFlags = flags;
            lastStatusFlags2 = flags2;

        } catch (IOException | JsonSyntaxException ex) {
            ex.printStackTrace();
        }
    }

    private static int getIntOrDefault(JsonObject obj, String key, int def) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return def;
        }
        try {
            return el.getAsInt();
        } catch (Exception e) {
            return def;
        }
    }

    private static long getLongOrDefault(JsonObject obj, String key, long def) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return def;
        }
        try {
            return el.getAsLong();
        } catch (Exception e) {
            return def;
        }
    }

    private static double getDoubleOrDefault(JsonObject obj, String key, double def) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return def;
        }
        try {
            return el.getAsDouble();
        } catch (Exception e) {
            return def;
        }
    }

    private static String getStringOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        try {
            return el.getAsString();
        } catch (Exception e) {
            return null;
        }
    }
    
    private long readFromFile(Path file, long startPos) {
        if (!Files.isRegularFile(file)) {
            return startPos;
        }

        long newPos = startPos;

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(startPos);

            String raw;
            while ((raw = raf.readLine()) != null) {
                newPos = raf.getFilePointer();

                // Convert from ISO-8859-1 assumption to UTF-8
                String line = new String(raw.getBytes(StandardCharsets.ISO_8859_1),
                                         StandardCharsets.UTF_8).trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    EliteLogEvent event = parser.parseRecord(line);
                    if (event != null) {
                        dispatch(event);
                    }
                } catch (JsonSyntaxException | IllegalStateException ex) {
                    // malformed line – skip
                }
            }
        } catch (IOException ex) {
            // transient I/O – skip; retry next poll
        }

        return newPos;
    }

    public void dispatch(EliteLogEvent event) {
        for (Consumer<EliteLogEvent> l : listeners) {
            try {
                l.accept(event);
            } catch (RuntimeException ex) {
                // don't let one bad listener break the others
            }
        }
    }

    private Path findLatestJournalFile(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> candidates = s
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("Journal.") && name.endsWith(".log");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                return null;
            }
            return candidates.get(candidates.size() - 1);
        }
    }
}
