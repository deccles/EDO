package org.dce.ed.logreader;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.dce.ed.logreader.EliteLogEvent.GenericEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteClearEvent;
import org.dce.ed.logreader.EliteLogEvent.NavRouteEvent;
import org.dce.ed.logreader.event.CommanderEvent;
import org.dce.ed.logreader.event.FileheaderEvent;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FsdTargetEvent;
import org.dce.ed.logreader.event.FssAllBodiesFoundEvent;
import org.dce.ed.logreader.event.FssBodySignalsEvent;
import org.dce.ed.logreader.event.FssDiscoveryScanEvent;
import org.dce.ed.logreader.event.LoadGameEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.logreader.event.ReceiveTextEvent;
import org.dce.ed.logreader.event.SaasignalsFoundEvent;
import org.dce.ed.logreader.event.ScanEvent;
import org.dce.ed.logreader.event.ScanOrganicEvent;
import org.dce.ed.logreader.event.StartJumpEvent;
import org.dce.ed.logreader.event.StatusEvent;

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

            // NEW: system/bodies-related events
            case SCAN:
                return parseScan(ts, obj);
            case SCAN_ORGANIC:
            	return parseScanOrganic(ts, obj);
            case FSS_DISCOVERY_SCAN:
                return parseFssDiscoveryScan(ts, obj);
            case FSS_ALL_BODIES_FOUND:
                return new FssAllBodiesFoundEvent(
                    ts,
                    obj,
                    obj.has("SystemName") ? obj.get("SystemName").getAsString() : null,
                    		obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L,
                    				obj.has("Count") ? obj.get("Count").getAsInt() : 0
                );
            case FSS_BODY_SIGNAL_DISCOVERED:
                return parseFssBodySignals(ts, obj);

            case NAV_ROUTE:
                return new NavRouteEvent(ts, obj);
            case NAV_ROUTE_CLEAR:
                return new NavRouteClearEvent(ts, obj);
            case RECEIVE_TEXT:
                return parseReceiveText(ts, obj);
            case STATUS:
                return parseStatus(ts, obj);
            default:
                // For everything else, fall back to generic event.
                return new GenericEvent(ts, type, obj);
        }
    }

    private FileheaderEvent parseFileheader(Instant ts, JsonObject obj) {
        int part = obj.has("part") ? obj.get("part").getAsInt() : 0;
        String language = obj.has("language") ? obj.get("language").getAsString() : null;
        boolean odyssey = obj.has("Odyssey") && obj.get("Odyssey").getAsBoolean();
        String gameVersion = obj.has("gameversion") ? obj.get("gameversion").getAsString() : null;
        String build = obj.has("build") ? obj.get("build").getAsString() : null;

        return new FileheaderEvent(ts, obj, part, language, odyssey, gameVersion, build);
    }

    private CommanderEvent parseCommander(Instant ts, JsonObject obj) {
        String fid = obj.has("FID") ? obj.get("FID").getAsString() : null;
        String name = obj.has("Name") ? obj.get("Name").getAsString() : null;
        return new CommanderEvent(ts, obj, fid, name);
    }

    private LoadGameEvent parseLoadGame(Instant ts, JsonObject obj) {
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

        return new LoadGameEvent(
                ts, obj,
                commander, fid,
                ship, shipId,
                shipName, shipIdent,
                fuelLevel, fuelCapacity,
                gameMode, credits
        );
    }

    private LocationEvent parseLocation(Instant ts, JsonObject obj) {
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

        return new LocationEvent(
                ts, obj,
                docked, taxi, multicrew,
                starSystem, systemAddress,
                starPos, body, bodyId, bodyType
        );
    }

    private StartJumpEvent parseStartJump(Instant ts, JsonObject obj) {
        String jumpType = getString(obj, "JumpType");
        boolean taxi = obj.has("Taxi") && obj.get("Taxi").getAsBoolean();
        String starSystem = getString(obj, "StarSystem");
        Long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : null;
        String starClass = getString(obj, "StarClass");
        return new StartJumpEvent(ts, obj, jumpType, taxi, starSystem, systemAddress, starClass);
    }

    private FsdJumpEvent parseFsdJump(Instant ts, JsonObject obj) {
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

        return new FsdJumpEvent(
                ts, obj,
                starSystem, systemAddress, starPos,
                body, bodyId, bodyType,
                jumpDist, fuelUsed, fuelLevel
        );
    }

    private FsdTargetEvent parseFsdTarget(Instant ts, JsonObject obj) {
        String name = getString(obj, "Name");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;
        String starClass = getString(obj, "StarClass");
        int remaining = obj.has("RemainingJumpsInRoute") ? obj.get("RemainingJumpsInRoute").getAsInt() : 0;
        return new FsdTargetEvent(ts, obj, name, systemAddress, starClass, remaining);
    }

    private SaasignalsFoundEvent parseSaaSignalsFound(Instant ts, JsonObject obj) {
        String bodyName = getString(obj, "BodyName");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;
        int bodyId = obj.has("BodyID") ? obj.get("BodyID").getAsInt() : -1;

        List<SaasignalsFoundEvent.Signal> signals = new ArrayList<>();
        if (obj.has("Signals") && obj.get("Signals").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("Signals")) {
                JsonObject so = e.getAsJsonObject();
                String type = getString(so, "Type");
                String typeLocalised = getString(so, "Type_Localised");
                int count = so.has("Count") ? so.get("Count").getAsInt() : 0;
                signals.add(new SaasignalsFoundEvent.Signal(type, typeLocalised, count));
            }
        }

        List<SaasignalsFoundEvent.Genus> genuses = new ArrayList<>();
        if (obj.has("Genuses") && obj.get("Genuses").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("Genuses")) {
                JsonObject go = e.getAsJsonObject();
                String genus = getString(go, "Genus");
                String genusLocalised = getString(go, "Genus_Localised");
                genuses.add(new SaasignalsFoundEvent.Genus(genus, genusLocalised));
            }
        }

        return new SaasignalsFoundEvent(ts, obj, bodyName, systemAddress, bodyId, signals, genuses);
    }

    private ReceiveTextEvent parseReceiveText(Instant ts, JsonObject obj) {
        String from = getString(obj, "From");
        String msg = getString(obj, "Message");
        String msgLoc = getString(obj, "Message_Localised");
        String channel = getString(obj, "Channel");
        return new ReceiveTextEvent(ts, obj, from, msg, msgLoc, channel);
    }

    private StatusEvent parseStatus(Instant ts, JsonObject obj) {
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

        // Extra Status.json fields
        Double latitude = obj.has("Latitude") && !obj.get("Latitude").isJsonNull()
                ? obj.get("Latitude").getAsDouble()
                : null;
        Double longitude = obj.has("Longitude") && !obj.get("Longitude").isJsonNull()
                ? obj.get("Longitude").getAsDouble()
                : null;
        Double altitude = obj.has("Altitude") && !obj.get("Altitude").isJsonNull()
                ? obj.get("Altitude").getAsDouble()
                : null;
        Double heading = obj.has("Heading") && !obj.get("Heading").isJsonNull()
                ? obj.get("Heading").getAsDouble()
                : null;
        String bodyName = getString(obj, "BodyName");
        Double planetRadius = obj.has("PlanetRadius") && !obj.get("PlanetRadius").isJsonNull()
                ? obj.get("PlanetRadius").getAsDouble()
                : null;

        // Destination
        Long destSystem = null;
        Integer destBody = null;
        String destName = null;
        if (obj.has("Destination") && obj.get("Destination").isJsonObject()) {
            JsonObject dest = obj.getAsJsonObject("Destination");
            if (dest.has("System") && !dest.get("System").isJsonNull()) {
                try {
                    destSystem = dest.get("System").getAsLong();
                } catch (Exception ignored) { }
            }
            if (dest.has("Body") && !dest.get("Body").isJsonNull()) {
                try {
                    destBody = dest.get("Body").getAsInt();
                } catch (Exception ignored) { }
            }
            if (dest.has("Name") && !dest.get("Name").isJsonNull()) {
                destName = dest.get("Name").getAsString();
            }
        }

        return new StatusEvent(
                ts,
                obj,
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
    }
    private ScanOrganicEvent parseScanOrganic(Instant ts, JsonObject json) {
        long systemAddress = json.has("SystemAddress")
                ? json.get("SystemAddress").getAsLong()
                : 0L;

        // In ScanOrganic, "Body" is the body index (BodyID), not a name.
        int bodyId = json.has("Body") ? json.get("Body").getAsInt() : -1;

        String scanType = getString(json, "ScanType");
        String genus = getString(json, "Genus");
        String genusLocalised = getString(json, "Genus_Localised");
        String species = getString(json, "Species");
        String speciesLocalised = getString(json, "Species_Localised");

        // There is no body name in ScanOrganic. We pass null here; the
        // SystemTabPanel will already have the name from the Scan/FSS data.
        String bodyName = null;

        return new ScanOrganicEvent(
                ts,
                json,
                systemAddress,
                bodyName,
                bodyId,
                scanType,
                genus,
                genusLocalised,
                species,
                speciesLocalised
        );
    }

    private ScanEvent parseScan(Instant ts, JsonObject obj) {
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
        Double surfaceTemp = obj.has("SurfaceTemperature")
                ? obj.get("SurfaceTemperature").getAsDouble()
                : null;
        String volcanism = getString(obj, "Volcanism");
        boolean wasDiscovered = obj.has("WasDiscovered") && obj.get("WasDiscovered").getAsBoolean();
        boolean wasMapped = obj.has("WasMapped") && obj.get("WasMapped").getAsBoolean();
        String starType = getString(obj, "StarType");

        return new ScanEvent(
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
                surfaceTemp,
                volcanism,
                wasDiscovered,
                wasMapped,
                starType
        );
    }

    private FssDiscoveryScanEvent parseFssDiscoveryScan(Instant ts, JsonObject obj) {
        double progress = obj.has("Progress") ? obj.get("Progress").getAsDouble() : 0.0;
        int bodyCount = obj.has("BodyCount") ? obj.get("BodyCount").getAsInt() : 0;
        int nonBodyCount = obj.has("NonBodyCount") ? obj.get("NonBodyCount").getAsInt() : 0;
        String systemName = getString(obj, "SystemName");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;

        return new FssDiscoveryScanEvent(
                ts,
                obj,
                progress,
                bodyCount,
                nonBodyCount,
                systemName,
                systemAddress
        );
    }


    private FssBodySignalsEvent parseFssBodySignals(Instant ts, JsonObject obj) {
        String bodyName = getString(obj, "BodyName");
        long systemAddress = obj.has("SystemAddress") ? obj.get("SystemAddress").getAsLong() : 0L;
        int bodyId = obj.has("BodyID") ? obj.get("BodyID").getAsInt() : -1;

        List<SaasignalsFoundEvent.Signal> signals = new ArrayList<>();
        if (obj.has("Signals") && obj.get("Signals").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("Signals")) {
                JsonObject so = e.getAsJsonObject();
                String type = getString(so, "Type");
                String typeLocalised = getString(so, "Type_Localised");
                int count = so.has("Count") ? so.get("Count").getAsInt() : 0;
                signals.add(new SaasignalsFoundEvent.Signal(type, typeLocalised, count));
            }
        }

        return new FssBodySignalsEvent(
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
