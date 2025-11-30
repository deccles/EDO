package org.dce.ed.logreader;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Parses individual Elite Dangerous journal JSON records into strongly-typed events.
 */
public class EliteLogParser {

    private final Gson gson = new Gson(); // kept for future use if needed

    public EliteLogEvent parseRecord(String jsonLine) {
        JsonObject obj = JsonParser.parseString(jsonLine).getAsJsonObject();

        String eventName = obj.has("event") ? obj.get("event").getAsString() : "Status";
        EliteEventType type = EliteEventType.fromJournalName(eventName);
        Instant ts = Instant.parse(obj.get("timestamp").getAsString());

        switch (type) {
            case FILEHEADER:
                return parseFileheader(ts, obj);
            case COMMANDER:
                return parseCommander(ts, obj);
            case LOAD_GAME:
                return parseLoadGame(ts, obj);
            case LOCATION:
                return parseLocation(ts, obj);
            case START_JUMP:
                return parseStartJump(ts, obj);
            case FSD_JUMP:
                return parseFsdJump(ts, obj);
            case FSD_TARGET:
                return parseFsdTarget(ts, obj);
            case SAASIGNALS_FOUND:
                return parseSaaSignalsFound(ts, obj);
            case SCAN:
                return parseScan(ts, obj);
            case FSS_DISCOVERY_SCAN:
                return parseFssDiscoveryScan(ts, obj);
            case FSS_BODY_SIGNAL_DISCOVERED:
                return parseFssBodySignals(ts, obj);
            case NAV_ROUTE:
                return new EliteLogEvent.NavRouteEvent(ts, obj);
            case NAV_ROUTE_CLEAR:
                return new EliteLogEvent.NavRouteClearEvent(ts, obj);
            case RECEIVE_TEXT:
                return parseReceiveText(ts, obj);
            case STATUS:
                return parseStatus(ts, obj);
            default:
                // For now, map all other events to GenericEvent, but keep the type and raw JSON.
                return new EliteLogEvent.GenericEvent(ts, type, obj);
        }
    }

    private EliteLogEvent.FileheaderEvent parseFileheader(Instant ts, JsonObject obj) {
        int part = obj.has("part") ? obj.get("part").getAsInt() : 0;
        String language = obj.has("language") ? obj.get("language").getAsString() : null;
        boolean odyssey = obj.has("Odyssey") && obj.get("Odyssey").getAsBoolean();
        String gameVersion = obj.has("gameversion") ? obj.get("gameversion").getAsString() : null;
        String build = obj.has("build") ? obj.get("build").getAsString() : null;

        return new EliteLogEvent.FileheaderEvent(ts, obj, part, language, odyssey, gameVersion, build);
    }

    private EliteLogEvent.CommanderEvent parseCommander(Instant ts, JsonObject obj) {
        String fid = obj.has("FID") ? obj.get("FID").getAsString() : null;
        String name = obj.has("Name") ? obj.get("Name").getAsString() : null;
        return new EliteLogEvent.CommanderEvent(ts, obj, fid, name);
    }

    private EliteLogEvent.LoadGameEvent parseLoadGame(Instant ts, JsonObject obj) {
        String commander = getString(obj, "Commander");
        String fid = getString(obj, "FID");
        String ship = getString(obj, "Ship");
        int shipId = obj.has("ShipID") ? obj.get("ShipID").getAsInt() : -1;
        String shipName = getString(obj, "ShipName");
        String shipIdent = getString(obj, "ShipIdent");
        double fuelLevel = obj.has("FuelLevel") ? obj.get("FuelLevel").getAsDouble() : 0.0;
        double fuelCapacity = obj.has("FuelCapacity") ? obj.get("FuelCapacity").getAsDouble() : 0.0;
        String gameMode = getString(obj, "GameMode");
        long credits = obj.has("Credits") ? obj.get("Credits").getAsLong() : 0L;

        return new EliteLogEvent.LoadGameEvent(
                ts, obj,
                commander, fid,
                ship, shipId,
                shipName, shipIdent,
                fuelLevel, fuelCapacity,
                gameMode, credits
        );
    }

    private EliteLogEvent.LocationEvent parseLocation(Instant ts, JsonObject obj) {
        boolean docked = obj.has("Docked") && obj.get("Docked").getAsBoolean();
        boolean taxi = obj.has("Taxi") && obj.get("Taxi").getAsBoolean();
        boolean multicrew = obj.has("Multicrew") && obj.get("Multicrew").getAsBoolean();
        String starSystem = getString(obj, "StarSystem");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;

        double[] starPos = null;
        if (obj.has("StarPos") && obj.get("StarPos").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("StarPos");
            if (arr.size() == 3) {
                starPos = new double[]{
                        arr.get(0).getAsDouble(),
                        arr.get(1).getAsDouble(),
                        arr.get(2).getAsDouble()
                };
            }
        }

        String body = getString(obj, "Body");
        int bodyId = obj.has("BodyID") ? obj.get("BodyID").getAsInt() : -1;
        String bodyType = getString(obj, "BodyType");

        return new EliteLogEvent.LocationEvent(
                ts, obj,
                docked, taxi, multicrew,
                starSystem, systemAddress,
                starPos, body, bodyId, bodyType
        );
    }

    private EliteLogEvent.StartJumpEvent parseStartJump(Instant ts, JsonObject obj) {
        String jumpType = getString(obj, "JumpType");
        boolean taxi = obj.has("Taxi") && obj.get("Taxi").getAsBoolean();
        String starSystem = getString(obj, "StarSystem");
        Long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : null;
        String starClass = getString(obj, "StarClass");
        return new EliteLogEvent.StartJumpEvent(ts, obj, jumpType, taxi, starSystem, systemAddress, starClass);
    }

    private EliteLogEvent.FsdJumpEvent parseFsdJump(Instant ts, JsonObject obj) {
        String starSystem = getString(obj, "StarSystem");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;

        double[] starPos = null;
        if (obj.has("StarPos") && obj.get("StarPos").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("StarPos");
            if (arr.size() == 3) {
                starPos = new double[]{
                        arr.get(0).getAsDouble(),
                        arr.get(1).getAsDouble(),
                        arr.get(2).getAsDouble()
                };
            }
        }

        String body = getString(obj, "Body");
        int bodyId = obj.has("BodyID") ? obj.get("BodyID").getAsInt() : -1;
        String bodyType = getString(obj, "BodyType");
        double jumpDist = obj.has("JumpDist") ? obj.get("JumpDist").getAsDouble() : 0.0;
        double fuelUsed = obj.has("FuelUsed") ? obj.get("FuelUsed").getAsDouble() : 0.0;
        double fuelLevel = obj.has("FuelLevel") ? obj.get("FuelLevel").getAsDouble() : 0.0;

        return new EliteLogEvent.FsdJumpEvent(
                ts, obj,
                starSystem, systemAddress, starPos,
                body, bodyId, bodyType,
                jumpDist, fuelUsed, fuelLevel
        );
    }

    private EliteLogEvent.FsdTargetEvent parseFsdTarget(Instant ts, JsonObject obj) {
        String name = getString(obj, "Name");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;
        String starClass = getString(obj, "StarClass");
        int remaining = obj.has("RemainingJumpsInRoute") ? obj.get("RemainingJumpsInRoute").getAsInt() : 0;
        return new EliteLogEvent.FsdTargetEvent(ts, obj, name, systemAddress, starClass, remaining);
    }

    private EliteLogEvent.SaasignalsFoundEvent parseSaaSignalsFound(Instant ts, JsonObject obj) {
        String bodyName = getString(obj, "BodyName");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;
        int bodyId = obj.has("BodyID") ? obj.get("BodyID").getAsInt() : -1;

        List<EliteLogEvent.SaasignalsFoundEvent.Signal> signals = new ArrayList<>();
        if (obj.has("Signals") && obj.get("Signals").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("Signals")) {
                JsonObject so = e.getAsJsonObject();
                String type = getString(so, "Type");
                String typeLocalised = getString(so, "Type_Localised");
                int count = so.has("Count") ? so.get("Count").getAsInt() : 0;
                signals.add(new EliteLogEvent.SaasignalsFoundEvent.Signal(type, typeLocalised, count));
            }
        }

        List<EliteLogEvent.SaasignalsFoundEvent.Genus> genuses = new ArrayList<>();
        if (obj.has("Genuses") && obj.get("Genuses").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("Genuses")) {
                JsonObject go = e.getAsJsonObject();
                String genus = getString(go, "Genus");
                String genusLocalised = getString(go, "Genus_Localised");
                genuses.add(new EliteLogEvent.SaasignalsFoundEvent.Genus(genus, genusLocalised));
            }
        }

        return new EliteLogEvent.SaasignalsFoundEvent(ts, obj, bodyName, systemAddress, bodyId, signals, genuses);
    }

    private EliteLogEvent.ReceiveTextEvent parseReceiveText(Instant ts, JsonObject obj) {
        String from = getString(obj, "From");
        String msg = getString(obj, "Message");
        String msgLoc = getString(obj, "Message_Localised");
        String channel = getString(obj, "Channel");
        return new EliteLogEvent.ReceiveTextEvent(ts, obj, from, msg, msgLoc, channel);
    }

    private EliteLogEvent.StatusEvent parseStatus(Instant ts, JsonObject obj) {
        int flags = obj.has("Flags") ? obj.get("Flags").getAsInt() : 0;
        int flags2 = obj.has("Flags2") ? obj.get("Flags2").getAsInt() : 0;

        int[] pips = new int[3];
        if (obj.has("Pips") && obj.get("Pips").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("Pips");
            for (int i = 0; i < Math.min(3, arr.size()); i++) {
                pips[i] = arr.get(i).getAsInt();
            }
        }

        int fireGroup = obj.has("FireGroup") ? obj.get("FireGroup").getAsInt() : 0;
        int guiFocus = obj.has("GuiFocus") ? obj.get("GuiFocus").getAsInt() : 0;

        double fuelMain = 0.0;
        double fuelReservoir = 0.0;
        if (obj.has("Fuel") && obj.get("Fuel").isJsonObject()) {
            JsonObject fuel = obj.getAsJsonObject("Fuel");
            fuelMain = fuel.has("FuelMain") ? fuel.get("FuelMain").getAsDouble() : 0.0;
            fuelReservoir = fuel.has("FuelReservoir") ? fuel.get("FuelReservoir").getAsDouble() : 0.0;
        }

        double cargo = obj.has("Cargo") ? obj.get("Cargo").getAsDouble() : 0.0;
        String legalState = getString(obj, "LegalState");
        long balance = obj.has("Balance") ? obj.get("Balance").getAsLong() : 0L;

        return new EliteLogEvent.StatusEvent(
                ts, obj,
                flags, flags2,
                pips, fireGroup, guiFocus,
                fuelMain, fuelReservoir,
                cargo, legalState, balance
        );
    }

    private EliteLogEvent.ScanEvent parseScan(Instant ts, JsonObject obj) {
        String bodyName = getString(obj, "BodyName");
        int bodyId = obj.has("BodyID") ? obj.get("BodyID").getAsInt() : -1;
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;
        double distanceLs = obj.has("DistanceFromArrivalLS")
                ? obj.get("DistanceFromArrivalLS").getAsDouble()
                : Double.NaN;
        boolean landable = obj.has("Landable") && obj.get("Landable").getAsBoolean();
        String planetClass = getString(obj, "PlanetClass");
        String atmosphere = getString(obj, "Atmosphere");
        String terraformState = getString(obj, "TerraformState");
        Double surfaceGravity = obj.has("SurfaceGravity")
                ? obj.get("SurfaceGravity").getAsDouble()
                : null;
        boolean wasDiscovered = obj.has("WasDiscovered") && obj.get("WasDiscovered").getAsBoolean();
        boolean wasMapped = obj.has("WasMapped") && obj.get("WasMapped").getAsBoolean();
        String starType = getString(obj, "StarType");

        return new EliteLogEvent.ScanEvent(
                ts,
                obj,
                bodyName,
                bodyId,
                systemAddress,
                distanceLs,
                landable,
                planetClass,
                atmosphere,
                terraformState,
                surfaceGravity,
                wasDiscovered,
                wasMapped,
                starType
        );
    }

    private EliteLogEvent.FssDiscoveryScanEvent parseFssDiscoveryScan(Instant ts, JsonObject obj) {
        double progress = obj.has("Progress") ? obj.get("Progress").getAsDouble() : 0.0;
        int bodyCount = obj.has("BodyCount") ? obj.get("BodyCount").getAsInt() : 0;
        int nonBodyCount = obj.has("NonBodyCount") ? obj.get("NonBodyCount").getAsInt() : 0;
        String systemName = getString(obj, "SystemName");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;

        return new EliteLogEvent.FssDiscoveryScanEvent(
                ts,
                obj,
                progress,
                bodyCount,
                nonBodyCount,
                systemName,
                systemAddress
        );
    }


    private EliteLogEvent.FssBodySignalsEvent parseFssBodySignals(Instant ts, JsonObject obj) {
        String bodyName = getString(obj, "BodyName");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;
        int bodyId = obj.has("BodyID") ? obj.get("BodyID").getAsInt() : -1;

        List<EliteLogEvent.SaasignalsFoundEvent.Signal> signals = new ArrayList<>();
        if (obj.has("Signals") && obj.get("Signals").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("Signals")) {
                JsonObject so = e.getAsJsonObject();
                String type = getString(so, "Type");
                String typeLocalised = getString(so, "Type_Localised");
                int count = so.has("Count") ? so.get("Count").getAsInt() : 0;
                signals.add(new EliteLogEvent.SaasignalsFoundEvent.Signal(type, typeLocalised, count));
            }
        }

        return new EliteLogEvent.FssBodySignalsEvent(
                ts,
                obj,
                bodyName,
                systemAddress,
                bodyId,
                signals
        );
    }

    
    private String getString(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull()
                ? obj.get(field).getAsString()
                : null;
    }
}
