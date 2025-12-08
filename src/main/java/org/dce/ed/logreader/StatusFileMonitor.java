package org.dce.ed.logreader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Helper for decoding Elite Dangerous Status.json Flags / Flags2 and
 * watching the Status.json file for changes.
 *
 * Current responsibilities:
 *   - Decode Flags and Flags2 into booleans (all bits from the Status.json spec)
 *   - Poll Status.json for changes
 *   - Detect transition into "FSD hyperdrive charging for a jump" and notify a listener
 *
 * This is designed to be self-contained. You can:
 *   - Use StatusFileMonitor.DecodedFlags.decode(flags, flags2) anywhere you like
 *   - Use StatusFileMonitor to watch Status.json and fire a callback
 */
public final class StatusFileMonitor implements Closeable {

    private static final Gson GSON = new Gson();

    private final Path statusFile;
    private final Listener listener;
    private final ScheduledExecutorService executor;
    private final long pollMillis;

    private volatile boolean running;
    private volatile boolean lastHyperjumpCharging;
    private volatile Instant lastSeenTimestamp;

    /**
     * Listener interface for Status.json events discovered by this monitor.
     */
    public interface Listener {
        /**
         * Called when we detect a transition into:
         *   - FSD Charging (Flags bit 17)
         *   - FSD hyperdrive charging (Flags2 bit 19)
         *   - With a non-null Destination.System (i.e. a jump is actually targeted)
         */
        void onFsdHyperjumpCharging(StatusSnapshot snapshot);
    }

    /**
     * Snapshot of a Status.json frame with both raw and decoded fields.
     */
    public static final class StatusSnapshot {

        private final Instant timestamp;
        private final int flags;
        private final int flags2;
        private final DecodedFlags decoded;
        private final Long destinationSystem;
        private final Integer destinationBody;
        private final String destinationName;

        public StatusSnapshot(Instant timestamp,
                              int flags,
                              int flags2,
                              DecodedFlags decoded,
                              Long destinationSystem,
                              Integer destinationBody,
                              String destinationName) {
            this.timestamp = timestamp;
            this.flags = flags;
            this.flags2 = flags2;
            this.decoded = decoded;
            this.destinationSystem = destinationSystem;
            this.destinationBody = destinationBody;
            this.destinationName = destinationName;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getFlags() {
            return flags;
        }

        public int getFlags2() {
            return flags2;
        }

        public DecodedFlags getDecoded() {
            return decoded;
        }

        public Long getDestinationSystem() {
            return destinationSystem;
        }

        public Integer getDestinationBody() {
            return destinationBody;
        }

        public String getDestinationName() {
            return destinationName;
        }

        @Override
        public String toString() {
            return "StatusSnapshot{" +
                    "timestamp=" + timestamp +
                    ", flags=" + flags +
                    ", flags2=" + flags2 +
                    ", destinationSystem=" + destinationSystem +
                    ", destinationBody=" + destinationBody +
                    ", destinationName='" + destinationName + '\'' +
                    ", decoded=" + decoded +
                    '}';
        }
    }

    /**
     * Fully decoded bitfields from Flags and Flags2, according to the Status.json
     * documentation.
     *
     * See:
     *   https://doc.elitedangereuse.fr/Status%20File/ (Flags and Flags2 tables)
     */
    public static final class DecodedFlags {

        // ----- Flags (bitfield #1) -----
        public final boolean docked;
        public final boolean landed;
        public final boolean landingGearDown;
        public final boolean shieldsUp;
        public final boolean supercruise;
        public final boolean flightAssistOff;
        public final boolean hardpointsDeployed;
        public final boolean inWing;
        public final boolean lightsOn;
        public final boolean cargoScoopDeployed;
        public final boolean silentRunning;
        public final boolean scoopingFuel;
        public final boolean srvHandbrake;
        public final boolean srvUsingTurretView;
        public final boolean srvTurretRetracted;
        public final boolean srvDriveAssist;
        public final boolean fsdMassLocked;
        public final boolean fsdCharging;
        public final boolean fsdCooldown;
        public final boolean lowFuel;
        public final boolean overHeating;
        public final boolean hasLatLong;
        public final boolean isInDanger;
        public final boolean beingInterdicted;
        public final boolean inMainShip;
        public final boolean inFighter;
        public final boolean inSrv;
        public final boolean hudInAnalysisMode;
        public final boolean nightVision;
        public final boolean altitudeFromAverageRadius;
        public final boolean fsdJump;
        public final boolean srvHighBeam;

        // ----- Flags2 (bitfield #2) -----
        public final boolean onFoot;
        public final boolean inTaxi;
        public final boolean inMulticrew;
        public final boolean onFootInStation;
        public final boolean onFootOnPlanet;
        public final boolean aimDownSight;
        public final boolean lowOxygen;
        public final boolean lowHealth;
        public final boolean cold;
        public final boolean hot;
        public final boolean veryCold;
        public final boolean veryHot;
        public final boolean glideMode;
        public final boolean onFootInHangar;
        public final boolean onFootSocialSpace;
        public final boolean onFootExterior;
        public final boolean breathableAtmosphere;
        public final boolean telepresenceMulticrew;
        public final boolean physicalMulticrew;
        public final boolean fsdHyperdriveCharging;

        private DecodedFlags(int flags, int flags2) {
            // Flags bits
            docked                    = (flags & 0x00000001) != 0;
            landed                    = (flags & 0x00000002) != 0;
            landingGearDown           = (flags & 0x00000004) != 0;
            shieldsUp                 = (flags & 0x00000008) != 0;
            supercruise               = (flags & 0x00000010) != 0;
            flightAssistOff           = (flags & 0x00000020) != 0;
            hardpointsDeployed        = (flags & 0x00000040) != 0;
            inWing                    = (flags & 0x00000080) != 0;
            lightsOn                  = (flags & 0x00000100) != 0;
            cargoScoopDeployed        = (flags & 0x00000200) != 0;
            silentRunning             = (flags & 0x00000400) != 0;
            scoopingFuel              = (flags & 0x00000800) != 0;
            srvHandbrake              = (flags & 0x00001000) != 0;
            srvUsingTurretView        = (flags & 0x00002000) != 0;
            srvTurretRetracted        = (flags & 0x00004000) != 0;
            srvDriveAssist            = (flags & 0x00008000) != 0;
            fsdMassLocked             = (flags & 0x00010000) != 0;
            fsdCharging               = (flags & 0x00020000) != 0;
            fsdCooldown               = (flags & 0x00040000) != 0;
            lowFuel                   = (flags & 0x00080000) != 0;
            overHeating               = (flags & 0x00100000) != 0;
            hasLatLong                = (flags & 0x00200000) != 0;
            isInDanger                = (flags & 0x00400000) != 0;
            beingInterdicted          = (flags & 0x00800000) != 0;
            inMainShip                = (flags & 0x01000000) != 0;
            inFighter                 = (flags & 0x02000000) != 0;
            inSrv                     = (flags & 0x04000000) != 0;
            hudInAnalysisMode         = (flags & 0x08000000) != 0;
            nightVision               = (flags & 0x10000000) != 0;
            altitudeFromAverageRadius = (flags & 0x20000000) != 0;
            fsdJump                   = (flags & 0x40000000) != 0;
            srvHighBeam               = (flags & 0x80000000) != 0;

            // Flags2 bits
            onFoot                    = (flags2 & 0x00000001) != 0;
            inTaxi                    = (flags2 & 0x00000002) != 0;
            inMulticrew               = (flags2 & 0x00000004) != 0;
            onFootInStation           = (flags2 & 0x00000008) != 0;
            onFootOnPlanet            = (flags2 & 0x00000010) != 0;
            aimDownSight              = (flags2 & 0x00000020) != 0;
            lowOxygen                 = (flags2 & 0x00000040) != 0;
            lowHealth                 = (flags2 & 0x00000080) != 0;
            cold                      = (flags2 & 0x00000100) != 0;
            hot                       = (flags2 & 0x00000200) != 0;
            veryCold                  = (flags2 & 0x00000400) != 0;
            veryHot                   = (flags2 & 0x00000800) != 0;
            glideMode                 = (flags2 & 0x00001000) != 0;
            onFootInHangar            = (flags2 & 0x00002000) != 0;
            onFootSocialSpace         = (flags2 & 0x00004000) != 0;
            onFootExterior            = (flags2 & 0x00008000) != 0;
            breathableAtmosphere      = (flags2 & 0x00010000) != 0;
            telepresenceMulticrew     = (flags2 & 0x00020000) != 0;
            physicalMulticrew         = (flags2 & 0x00040000) != 0;
            fsdHyperdriveCharging     = (flags2 & 0x00080000) != 0;
        }

        public static DecodedFlags decode(int flags, int flags2) {
            return new DecodedFlags(flags, flags2);
        }

        @Override
        public String toString() {
            return "DecodedFlags{" +
                    "docked=" + docked +
                    ", landed=" + landed +
                    ", landingGearDown=" + landingGearDown +
                    ", shieldsUp=" + shieldsUp +
                    ", supercruise=" + supercruise +
                    ", flightAssistOff=" + flightAssistOff +
                    ", hardpointsDeployed=" + hardpointsDeployed +
                    ", inWing=" + inWing +
                    ", lightsOn=" + lightsOn +
                    ", cargoScoopDeployed=" + cargoScoopDeployed +
                    ", silentRunning=" + silentRunning +
                    ", scoopingFuel=" + scoopingFuel +
                    ", srvHandbrake=" + srvHandbrake +
                    ", srvUsingTurretView=" + srvUsingTurretView +
                    ", srvTurretRetracted=" + srvTurretRetracted +
                    ", srvDriveAssist=" + srvDriveAssist +
                    ", fsdMassLocked=" + fsdMassLocked +
                    ", fsdCharging=" + fsdCharging +
                    ", fsdCooldown=" + fsdCooldown +
                    ", lowFuel=" + lowFuel +
                    ", overHeating=" + overHeating +
                    ", hasLatLong=" + hasLatLong +
                    ", isInDanger=" + isInDanger +
                    ", beingInterdicted=" + beingInterdicted +
                    ", inMainShip=" + inMainShip +
                    ", inFighter=" + inFighter +
                    ", inSrv=" + inSrv +
                    ", hudInAnalysisMode=" + hudInAnalysisMode +
                    ", nightVision=" + nightVision +
                    ", altitudeFromAverageRadius=" + altitudeFromAverageRadius +
                    ", fsdJump=" + fsdJump +
                    ", srvHighBeam=" + srvHighBeam +
                    ", onFoot=" + onFoot +
                    ", inTaxi=" + inTaxi +
                    ", inMulticrew=" + inMulticrew +
                    ", onFootInStation=" + onFootInStation +
                    ", onFootOnPlanet=" + onFootOnPlanet +
                    ", aimDownSight=" + aimDownSight +
                    ", lowOxygen=" + lowOxygen +
                    ", lowHealth=" + lowHealth +
                    ", cold=" + cold +
                    ", hot=" + hot +
                    ", veryCold=" + veryCold +
                    ", veryHot=" + veryHot +
                    ", glideMode=" + glideMode +
                    ", onFootInHangar=" + onFootInHangar +
                    ", onFootSocialSpace=" + onFootSocialSpace +
                    ", onFootExterior=" + onFootExterior +
                    ", breathableAtmosphere=" + breathableAtmosphere +
                    ", telepresenceMulticrew=" + telepresenceMulticrew +
                    ", physicalMulticrew=" + physicalMulticrew +
                    ", fsdHyperdriveCharging=" + fsdHyperdriveCharging +
                    '}';
        }
    }

    /**
     * Create a new monitor for the given Status.json file.
     *
     * @param statusFile  path to Status.json
     * @param listener    callback for FSD hyperjump charging events (may be null)
     * @param pollMillis  polling interval in milliseconds
     */
    public StatusFileMonitor(Path statusFile, Listener listener, long pollMillis) {
        this.statusFile = Objects.requireNonNull(statusFile, "statusFile");
        this.listener = listener;
        this.pollMillis = pollMillis <= 0 ? 250L : pollMillis;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StatusFileMonitor");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.scheduleWithFixedDelay(this::pollOnce, 0L, pollMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    private void pollOnce() {
        if (!running) {
            return;
        }

        try {
            if (!Files.exists(statusFile)) {
                return;
            }

            String json = Files.readString(statusFile, StandardCharsets.UTF_8);
            if (json == null || json.isEmpty()) {
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Use timestamp to avoid re-processing the exact same frame if the file
            // hasn't actually changed logically.
            Instant ts = null;
            JsonElement tsEl = root.get("timestamp");
            if (tsEl != null && !tsEl.isJsonNull()) {
                try {
                    ts = Instant.parse(tsEl.getAsString());
                } catch (Exception ignored) {
                    // leave ts null if parse fails
                }
            }

            if (ts != null && ts.equals(lastSeenTimestamp)) {
                return; // nothing new
            }

            int flags = getIntOrDefault(root, "Flags", 0);
            int flags2 = getIntOrDefault(root, "Flags2", 0);

            DecodedFlags decoded = DecodedFlags.decode(flags, flags2);

            Long destSystem = null;
            Integer destBody = null;
            String destName = null;

            JsonElement destEl = root.get("Destination");
            if (destEl != null && destEl.isJsonObject()) {
                JsonObject dest = destEl.getAsJsonObject();
                if (dest.has("System") && !dest.get("System").isJsonNull()) {
                    try {
                        destSystem = dest.get("System").getAsLong();
                    } catch (Exception ignored) {
                    }
                }
                if (dest.has("Body") && !dest.get("Body").isJsonNull()) {
                    try {
                        destBody = dest.get("Body").getAsInt();
                    } catch (Exception ignored) {
                    }
                }
                if (dest.has("Name") && !dest.get("Name").isJsonNull()) {
                    destName = dest.get("Name").getAsString();
                }
            }

            StatusSnapshot snapshot = new StatusSnapshot(
                    ts,
                    flags,
                    flags2,
                    decoded,
                    destSystem,
                    destBody,
                    destName
            );

            boolean hyperjumpChargingNow =
                    decoded.fsdCharging &&
                    decoded.fsdHyperdriveCharging &&
                    destSystem != null;

            if (listener != null && hyperjumpChargingNow && !lastHyperjumpCharging) {
                listener.onFsdHyperjumpCharging(snapshot);
            }

            lastHyperjumpCharging = hyperjumpChargingNow;
            lastSeenTimestamp = ts;

        } catch (Exception e) {
            // Swallow exceptions to avoid killing the polling thread;
            // you can replace this with your logging framework.
            e.printStackTrace();
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
}
