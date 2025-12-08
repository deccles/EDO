package org.dce.ed.logreader;

import java.time.Instant;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * Base type for all parsed Elite Dangerous journal events.
 * Subclasses represent specific event types.
 */
public abstract class EliteLogEvent {

    private final Instant timestamp;
    private final EliteEventType type;
    private final JsonObject rawJson; // for anything we didn't model

    protected EliteLogEvent(Instant timestamp, EliteEventType type, JsonObject rawJson) {
        this.timestamp = timestamp;
        this.type = type;
        this.rawJson = rawJson;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public EliteEventType getType() {
        return type;
    }

    public JsonObject getRawJson() {
        return rawJson;
    }

    // ---------- Specific event types ----------

    public static final class FileheaderEvent extends EliteLogEvent {
        private final int part;
        private final String language;
        private final boolean odyssey;
        private final String gameVersion;
        private final String build;

        public FileheaderEvent(Instant timestamp,
                               JsonObject rawJson,
                               int part,
                               String language,
                               boolean odyssey,
                               String gameVersion,
                               String build) {
            super(timestamp, EliteEventType.FILEHEADER, rawJson);
            this.part = part;
            this.language = language;
            this.odyssey = odyssey;
            this.gameVersion = gameVersion;
            this.build = build;
        }

        public int getPart() {
            return part;
        }

        public String getLanguage() {
            return language;
        }

        public boolean isOdyssey() {
            return odyssey;
        }

        public String getGameVersion() {
            return gameVersion;
        }

        public String getBuild() {
            return build;
        }
    }

    public static final class CommanderEvent extends EliteLogEvent {
        private final String fid;
        private final String name;

        public CommanderEvent(Instant timestamp,
                              JsonObject rawJson,
                              String fid,
                              String name) {
            super(timestamp, EliteEventType.COMMANDER, rawJson);
            this.fid = fid;
            this.name = name;
        }

        public String getFid() {
            return fid;
        }

        public String getName() {
            return name;
        }
    }

    public static final class LoadGameEvent extends EliteLogEvent {
        private final String commander;
        private final String fid;
        private final String ship;
        private final int shipId;
        private final String shipName;
        private final String shipIdent;
        private final double fuelLevel;
        private final double fuelCapacity;
        private final String gameMode;
        private final long credits;

        public LoadGameEvent(Instant timestamp,
                             JsonObject rawJson,
                             String commander,
                             String fid,
                             String ship,
                             int shipId,
                             String shipName,
                             String shipIdent,
                             double fuelLevel,
                             double fuelCapacity,
                             String gameMode,
                             long credits) {
            super(timestamp, EliteEventType.LOAD_GAME, rawJson);
            this.commander = commander;
            this.fid = fid;
            this.ship = ship;
            this.shipId = shipId;
            this.shipName = shipName;
            this.shipIdent = shipIdent;
            this.fuelLevel = fuelLevel;
            this.fuelCapacity = fuelCapacity;
            this.gameMode = gameMode;
            this.credits = credits;
        }

        public String getCommander() {
            return commander;
        }

        public String getFid() {
            return fid;
        }

        public String getShip() {
            return ship;
        }

        public int getShipId() {
            return shipId;
        }

        public String getShipName() {
            return shipName;
        }

        public String getShipIdent() {
            return shipIdent;
        }

        public double getFuelLevel() {
            return fuelLevel;
        }

        public double getFuelCapacity() {
            return fuelCapacity;
        }

        public String getGameMode() {
            return gameMode;
        }

        public long getCredits() {
            return credits;
        }
    }

    public static final class LocationEvent extends EliteLogEvent {
        private final boolean docked;
        private final boolean taxi;
        private final boolean multicrew;
        private final String starSystem;
        private final long systemAddress;
        private final double[] starPos;
        private final String body;
        private final int bodyId;
        private final String bodyType;

        public LocationEvent(Instant timestamp,
                             JsonObject rawJson,
                             boolean docked,
                             boolean taxi,
                             boolean multicrew,
                             String starSystem,
                             long systemAddress,
                             double[] starPos,
                             String body,
                             int bodyId,
                             String bodyType) {
            super(timestamp, EliteEventType.LOCATION, rawJson);
            this.docked = docked;
            this.taxi = taxi;
            this.multicrew = multicrew;
            this.starSystem = starSystem;
            this.systemAddress = systemAddress;
            this.starPos = starPos;
            this.body = body;
            this.bodyId = bodyId;
            this.bodyType = bodyType;
        }

        public boolean isDocked() {
            return docked;
        }

        public boolean isTaxi() {
            return taxi;
        }

        public boolean isMulticrew() {
            return multicrew;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public double[] getStarPos() {
            return starPos;
        }

        public String getBody() {
            return body;
        }

        public int getBodyId() {
            return bodyId;
        }

        public String getBodyType() {
            return bodyType;
        }
    }

    public static final class StartJumpEvent extends EliteLogEvent {
        private final String jumpType;
        private final boolean taxi;
        private final String starSystem;
        private final Long systemAddress;
        private final String starClass;

        public StartJumpEvent(Instant timestamp,
                              JsonObject rawJson,
                              String jumpType,
                              boolean taxi,
                              String starSystem,
                              Long systemAddress,
                              String starClass) {
            super(timestamp, EliteEventType.START_JUMP, rawJson);
            this.jumpType = jumpType;
            this.taxi = taxi;
            this.starSystem = starSystem;
            this.systemAddress = systemAddress;
            this.starClass = starClass;
        }

        public String getJumpType() {
            return jumpType;
        }

        public boolean isTaxi() {
            return taxi;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public Long getSystemAddress() {
            return systemAddress;
        }

        public String getStarClass() {
            return starClass;
        }
    }

    public static final class FsdJumpEvent extends EliteLogEvent {
        private final String starSystem;
        private final long systemAddress;
        private final double[] starPos;
        private final String body;
        private final int bodyId;
        private final String bodyType;
        private final double jumpDist;
        private final double fuelUsed;
        private final double fuelLevel;

        public FsdJumpEvent(Instant timestamp,
                            JsonObject rawJson,
                            String starSystem,
                            long systemAddress,
                            double[] starPos,
                            String body,
                            int bodyId,
                            String bodyType,
                            double jumpDist,
                            double fuelUsed,
                            double fuelLevel) {
            super(timestamp, EliteEventType.FSD_JUMP, rawJson);
            this.starSystem = starSystem;
            this.systemAddress = systemAddress;
            this.starPos = starPos;
            this.body = body;
            this.bodyId = bodyId;
            this.bodyType = bodyType;
            this.jumpDist = jumpDist;
            this.fuelUsed = fuelUsed;
            this.fuelLevel = fuelLevel;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public double[] getStarPos() {
            return starPos;
        }

        public String getBody() {
            return body;
        }

        public int getBodyId() {
            return bodyId;
        }

        public String getBodyType() {
            return bodyType;
        }

        public double getJumpDist() {
            return jumpDist;
        }

        public double getFuelUsed() {
            return fuelUsed;
        }

        public double getFuelLevel() {
            return fuelLevel;
        }
    }

    public static final class FsdTargetEvent extends EliteLogEvent {
        private final String name;
        private final long systemAddress;
        private final String starClass;
        private final int remainingJumpsInRoute;

        public FsdTargetEvent(Instant timestamp,
                              JsonObject rawJson,
                              String name,
                              long systemAddress,
                              String starClass,
                              int remainingJumpsInRoute) {
            super(timestamp, EliteEventType.FSD_TARGET, rawJson);
            this.name = name;
            this.systemAddress = systemAddress;
            this.starClass = starClass;
            this.remainingJumpsInRoute = remainingJumpsInRoute;
        }

        public String getName() {
            return name;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public String getStarClass() {
            return starClass;
        }

        public int getRemainingJumpsInRoute() {
            return remainingJumpsInRoute;
        }
    }

    public static final class SaasignalsFoundEvent extends EliteLogEvent {
        private final String bodyName;
        private final long systemAddress;
        private final int bodyId;
        private final List<Signal> signals;
        private final List<Genus> genuses;

        public SaasignalsFoundEvent(Instant timestamp,
                                    JsonObject rawJson,
                                    String bodyName,
                                    long systemAddress,
                                    int bodyId,
                                    List<Signal> signals,
                                    List<Genus> genuses) {
            super(timestamp, EliteEventType.SAASIGNALS_FOUND, rawJson);
            this.bodyName = bodyName;
            this.systemAddress = systemAddress;
            this.bodyId = bodyId;
            this.signals = signals;
            this.genuses = genuses;
        }

        public String getBodyName() {
            return bodyName;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public int getBodyId() {
            return bodyId;
        }

        public List<Signal> getSignals() {
            return signals;
        }

        public List<Genus> getGenuses() {
            return genuses;
        }

        public static final class Signal {
            private final String type;
            private final String typeLocalised;
            private final int count;

            public Signal(String type, String typeLocalised, int count) {
                this.type = type;
                this.typeLocalised = typeLocalised;
                this.count = count;
            }

            public String getType() {
                return type;
            }

            public String getTypeLocalised() {
                return typeLocalised;
            }

            public int getCount() {
                return count;
            }
        }

        public static final class Genus {
            private final String genus;
            private final String genusLocalised;

            public Genus(String genus, String genusLocalised) {
                this.genus = genus;
                this.genusLocalised = genusLocalised;
            }

            public String getGenus() {
                return genus;
            }

            public String getGenusLocalised() {
                return genusLocalised;
            }
        }
    }

    public static final class StatusEvent extends EliteLogEvent {
        private final int flags;
        private final int flags2;
        private final int[] pips;
        private final int fireGroup;
        private final int guiFocus;
        private final double fuelMain;
        private final double fuelReservoir;
        private final double cargo;
        private final String legalState;
        private final long balance;

        // Extra Status.json fields
        private final Double latitude;
        private final Double longitude;
        private final Double altitude;
        private final Double heading;
        private final String bodyName;
        private final Double planetRadius;

        // Travel destination (added in later journal versions)
        private final Long destinationSystem;
        private final Integer destinationBody;
        private final String destinationName;

        private final DecodedFlags decodedFlags;

        public StatusEvent(Instant timestamp,
                           JsonObject rawJson,
                           int flags,
                           int flags2,
                           int[] pips,
                           int fireGroup,
                           int guiFocus,
                           double fuelMain,
                           double fuelReservoir,
                           double cargo,
                           String legalState,
                           long balance,
                           Double latitude,
                           Double longitude,
                           Double altitude,
                           Double heading,
                           String bodyName,
                           Double planetRadius,
                           Long destinationSystem,
                           Integer destinationBody,
                           String destinationName) {

            super(timestamp, EliteEventType.STATUS, rawJson);
            this.flags = flags;
            this.flags2 = flags2;
            this.pips = (pips != null ? pips : new int[3]);
            this.fireGroup = fireGroup;
            this.guiFocus = guiFocus;
            this.fuelMain = fuelMain;
            this.fuelReservoir = fuelReservoir;
            this.cargo = cargo;
            this.legalState = legalState;
            this.balance = balance;

            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.heading = heading;
            this.bodyName = bodyName;
            this.planetRadius = planetRadius;

            this.destinationSystem = destinationSystem;
            this.destinationBody = destinationBody;
            this.destinationName = destinationName;

            this.decodedFlags = new DecodedFlags(flags, flags2);
        }

        // ---- Raw values ----

        public int getFlags() {
            return flags;
        }

        public int getFlags2() {
            return flags2;
        }

        public int[] getPips() {
            return pips;
        }

        public int getFireGroup() {
            return fireGroup;
        }

        public int getGuiFocus() {
            return guiFocus;
        }

        public double getFuelMain() {
            return fuelMain;
        }

        public double getFuelReservoir() {
            return fuelReservoir;
        }

        public double getCargo() {
            return cargo;
        }

        public String getLegalState() {
            return legalState;
        }

        public long getBalance() {
            return balance;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public Double getAltitude() {
            return altitude;
        }

        public Double getHeading() {
            return heading;
        }

        public String getBodyName() {
            return bodyName;
        }

        public Double getPlanetRadius() {
            return planetRadius;
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

        // ---- Convenience accessors for flags ----

        public DecodedFlags getDecodedFlags() {
            return decodedFlags;
        }

        public boolean isFsdCharging() {
            return decodedFlags.fsdCharging;
        }

        public boolean isFsdHyperdriveCharging() {
            return decodedFlags.fsdHyperdriveCharging;
        }

        public boolean isSupercruise() {
            return decodedFlags.supercruise;
        }

        public boolean isDocked() {
            return decodedFlags.docked;
        }

        public boolean isOnFoot() {
            return decodedFlags.onFoot;
        }

        public boolean isInSrv() {
            return decodedFlags.inSrv;
        }

        // Add more one-liners as needed (inFighter, nightVision, etc)

        public static final class DecodedFlags {

            // Flags (bitfield #1)
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

            // Flags2 (bitfield #2)
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
                // Flags
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

                // Flags2
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
        }

    }

    public static final class ReceiveTextEvent extends EliteLogEvent {
        private final String from;
        private final String message;
        private final String messageLocalised;
        private final String channel;

        public ReceiveTextEvent(Instant timestamp,
                                JsonObject rawJson,
                                String from,
                                String message,
                                String messageLocalised,
                                String channel) {
            super(timestamp, EliteEventType.RECEIVE_TEXT, rawJson);
            this.from = from;
            this.message = message;
            this.messageLocalised = messageLocalised;
            this.channel = channel;
        }

        public String getFrom() {
            return from;
        }

        public String getMessage() {
            return message;
        }

        public String getMessageLocalised() {
            return messageLocalised;
        }

        public String getChannel() {
            return channel;
        }
    }

    public static final class NavRouteEvent extends EliteLogEvent {
        public NavRouteEvent(Instant timestamp, JsonObject rawJson) {
            super(timestamp, EliteEventType.NAV_ROUTE, rawJson);
        }
    }

    public static final class NavRouteClearEvent extends EliteLogEvent {
        public NavRouteClearEvent(Instant timestamp, JsonObject rawJson) {
            super(timestamp, EliteEventType.NAV_ROUTE_CLEAR, rawJson);
        }
    }

    /**
     * Scan event for stars and bodies.
     * Backed by the journal "Scan" event.
     */
    public static final class ScanEvent extends EliteLogEvent {

        private final String bodyName;
        private final int bodyId;
        private final long systemAddress;
        private final double distanceFromArrivalLs;
        private final boolean landable;
        private final String planetClass;
        private final String atmosphere;
        private final String terraformState;
        private final Double surfaceGravity;
        private final Double surfaceTemperature;
        private final String volcanism;
        private final boolean wasDiscovered;
        private final boolean wasMapped;
        private final String starType;

        public ScanEvent(Instant timestamp,
                         JsonObject rawJson,
                         String bodyName,
                         int bodyId,
                         long systemAddress,
                         double distanceFromArrivalLs,
                         boolean landable,
                         String planetClass,
                         String atmosphere,
                         String terraformState,
                         Double surfaceGravity,
                         Double surfaceTemperature,
                         String volcanism,
                         boolean wasDiscovered,
                         boolean wasMapped,
                         String starType) {

            super(timestamp, EliteEventType.SCAN, rawJson);
            this.bodyName = bodyName;
            this.bodyId = bodyId;
            this.systemAddress = systemAddress;
            this.distanceFromArrivalLs = distanceFromArrivalLs;
            this.landable = landable;
            this.planetClass = planetClass;
            this.atmosphere = atmosphere;
            this.terraformState = terraformState;
            this.surfaceGravity = surfaceGravity;
            this.surfaceTemperature = surfaceTemperature;
            this.volcanism = volcanism;
            this.wasDiscovered = wasDiscovered;
            this.wasMapped = wasMapped;
            this.starType = starType;
        }

        public String getBodyName() {
            return bodyName;
        }

        public int getBodyId() {
            return bodyId;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public double getDistanceFromArrivalLs() {
            return distanceFromArrivalLs;
        }

        public boolean isLandable() {
            return landable;
        }

        public String getPlanetClass() {
            return planetClass;
        }

        public String getAtmosphere() {
            return atmosphere;
        }

        public String getTerraformState() {
            return terraformState;
        }

        public Double getSurfaceGravity() {
            return surfaceGravity;
        }

        /** Surface temperature in Kelvin (may be null if not present). */
        public Double getSurfaceTemperature() {
            return surfaceTemperature;
        }

        /** Raw Volcanism string from the journal (may be null/empty). */
        public String getVolcanism() {
            return volcanism;
        }

        public boolean isWasDiscovered() {
            return wasDiscovered;
        }

        public boolean isWasMapped() {
            return wasMapped;
        }

        public String getStarType() {
            return starType;
        }
    }

    /**
     * FSSDiscoveryScan ("honk") – summary of bodies known in the system.
     */
    public static final class FssDiscoveryScanEvent extends EliteLogEvent {

        private final double progress;
        private final int bodyCount;
        private final int nonBodyCount;
        private final String systemName;
        private final long systemAddress;

        public FssDiscoveryScanEvent(Instant timestamp,
                                     JsonObject rawJson,
                                     double progress,
                                     int bodyCount,
                                     int nonBodyCount,
                                     String systemName,
                                     long systemAddress) {
            super(timestamp, EliteEventType.FSS_DISCOVERY_SCAN, rawJson);
            this.progress = progress;
            this.bodyCount = bodyCount;
            this.nonBodyCount = nonBodyCount;
            this.systemName = systemName;
            this.systemAddress = systemAddress;
        }

        public double getProgress() {
            return progress;
        }

        public int getBodyCount() {
            return bodyCount;
        }

        public int getNonBodyCount() {
            return nonBodyCount;
        }

        public String getSystemName() {
            return systemName;
        }

        public long getSystemAddress() {
            return systemAddress;
        }
    }

    /**
     * FSSAllBodiesFound – fired when the game considers all bodies in the
     * current system discovered via the FSS.
     */
    public static final class FssAllBodiesFoundEvent extends EliteLogEvent {

        private final String systemName;
        private final long systemAddress;
        private final int bodyCount;

        public FssAllBodiesFoundEvent(Instant timestamp,
                                      JsonObject rawJson,
                                      String systemName,
                                      long systemAddress,
                                      int bodyCount) {
            super(timestamp, EliteEventType.FSS_ALL_BODIES_FOUND, rawJson);
            this.systemName = systemName;
            this.systemAddress = systemAddress;
            this.bodyCount = bodyCount;
        }

        public String getSystemName() {
            return systemName;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public int getBodyCount() {
            return bodyCount;
        }
    }

    

    /**
     * FSSBodySignals – very similar to SAASignalsFound, but coming from the FSS.
     * We reuse the SaasignalsFoundEvent.Signal type for convenience.
     */
    public static final class FssBodySignalsEvent extends EliteLogEvent {

        private final String bodyName;
        private final long systemAddress;
        private final int bodyId;
        private final List<SaasignalsFoundEvent.Signal> signals;

        public FssBodySignalsEvent(Instant timestamp,
                                   JsonObject rawJson,
                                   String bodyName,
                                   long systemAddress,
                                   int bodyId,
                                   List<SaasignalsFoundEvent.Signal> signals) {

            super(timestamp, EliteEventType.FSS_BODY_SIGNAL_DISCOVERED, rawJson);
            this.bodyName = bodyName;
            this.systemAddress = systemAddress;
            this.bodyId = bodyId;
            this.signals = signals;
        }

        public String getBodyName() {
            return bodyName;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public int getBodyId() {
            return bodyId;
        }

        public List<SaasignalsFoundEvent.Signal> getSignals() {
            return signals;
        }
    }
    
    /**
     * ScanOrganic – fired when you analyse an exobiology sample on a body.
     * Gives us the specific Genus/Species on that body.
     */
    public static final class ScanOrganicEvent extends EliteLogEvent {

        private final long systemAddress;
        private final String bodyName;
        private final int bodyId;
        private final String scanType;
        private final String genus;
        private final String genusLocalised;
        private final String species;
        private final String speciesLocalised;

        public ScanOrganicEvent(Instant timestamp,
                                JsonObject rawJson,
                                long systemAddress,
                                String bodyName,
                                int bodyId,
                                String scanType,
                                String genus,
                                String genusLocalised,
                                String species,
                                String speciesLocalised) {
            super(timestamp, EliteEventType.SCAN_ORGANIC, rawJson);
            this.systemAddress = systemAddress;
            this.bodyName = bodyName;
            this.bodyId = bodyId;
            this.scanType = scanType;
            this.genus = genus;
            this.genusLocalised = genusLocalised;
            this.species = species;
            this.speciesLocalised = speciesLocalised;
        }

        public long getSystemAddress() {
            return systemAddress;
        }

        public String getBodyName() {
            return bodyName;
        }

        public int getBodyId() {
            return bodyId;
        }

        public String getScanType() {
            return scanType;
        }

        public String getGenus() {
            return genus;
        }

        public String getGenusLocalised() {
            return genusLocalised;
        }

        public String getSpecies() {
            return species;
        }

        public String getSpeciesLocalised() {
            return speciesLocalised;
        }
    }

    /**
     * Generic catch-all event when we don't have a specific subclass yet.
     */
    public static final class GenericEvent extends EliteLogEvent {
        public GenericEvent(Instant timestamp, EliteEventType type, JsonObject rawJson) {
            super(timestamp, type, rawJson);
        }
    }

}
