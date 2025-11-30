package org.dce.ed.logreader;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility to format EliteLogEvent instances as human-readable log lines.
 * By default, it prints something for every event, including GenericEvent.
 */
public final class EliteLogEventFormatter {

    // Local time zone, e.g. "2025-11-28 22:17:03"
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withZone(LOCAL_ZONE);

    private EliteLogEventFormatter() {
    }

    public static String format(EliteLogEvent event) {
        if (event == null) {
            return "<null event>";
        }

        String ts = formatLocalTime(event.getTimestamp());
        EliteEventType type = event.getType();

        switch (type) {
            case FILEHEADER:
                return formatFileheader(ts, (EliteLogEvent.FileheaderEvent) event);
            case COMMANDER:
                return formatCommander(ts, (EliteLogEvent.CommanderEvent) event);
            case LOAD_GAME:
                return formatLoadGame(ts, (EliteLogEvent.LoadGameEvent) event);
            case LOCATION:
                return formatLocation(ts, (EliteLogEvent.LocationEvent) event);
            case START_JUMP:
                return formatStartJump(ts, (EliteLogEvent.StartJumpEvent) event);
            case FSD_JUMP:
                return formatFsdJump(ts, (EliteLogEvent.FsdJumpEvent) event);
            case FSD_TARGET:
                return formatFsdTarget(ts, (EliteLogEvent.FsdTargetEvent) event);
            case SAASIGNALS_FOUND:
                return formatSaaSignalsFound(ts, (EliteLogEvent.SaasignalsFoundEvent) event);
            case NAV_ROUTE:
                return ts + " [NAV_ROUTE] Nav route updated";
            case NAV_ROUTE_CLEAR:
                return ts + " [NAV_ROUTE_CLEAR] Nav route cleared";
            case STATUS:
                return formatStatus(ts, (EliteLogEvent.StatusEvent) event);
            case RECEIVE_TEXT:
                return formatReceiveText(ts, (EliteLogEvent.ReceiveTextEvent) event);
            default:
                // Generic / unknown events: show local timestamp + [eventName] + JSON minus timestamp/event
                return formatGeneric(ts, event);
        }
    }

    private static String formatLocalTime(Instant instant) {
        if (instant == null) {
            return "<no-ts>";
        }
        return TS_FORMAT.format(instant);
    }

    private static String formatFileheader(String ts, EliteLogEvent.FileheaderEvent e) {
        return ts + " [FILEHEADER] part=" + e.getPart()
                + " odyssey=" + e.isOdyssey()
                + " gameVersion=" + safe(e.getGameVersion())
                + " build=" + safe(e.getBuild());
    }

    private static String formatCommander(String ts, EliteLogEvent.CommanderEvent e) {
        return ts + " [COMMANDER] name=" + safe(e.getName())
                + " fid=" + safe(e.getFid());
    }

    private static String formatLoadGame(String ts, EliteLogEvent.LoadGameEvent e) {
        return ts + " [LOAD_GAME] commander=" + safe(e.getCommander())
                + " ship=" + safe(e.getShip())
                + " shipName=" + safe(e.getShipName())
                + " fuel=" + e.getFuelLevel() + "/" + e.getFuelCapacity()
                + " mode=" + safe(e.getGameMode())
                + " credits=" + e.getCredits();
    }

    private static String formatLocation(String ts, EliteLogEvent.LocationEvent e) {
        return ts + " [LOCATION] system=" + safe(e.getStarSystem())
                + " body=" + safe(e.getBody())
                + " bodyType=" + safe(e.getBodyType())
                + " docked=" + e.isDocked()
                + " taxi=" + e.isTaxi()
                + " multicrew=" + e.isMulticrew();
    }

    private static String formatStartJump(String ts, EliteLogEvent.StartJumpEvent e) {
        return ts + " [START_JUMP] type=" + safe(e.getJumpType())
                + " system=" + safe(e.getStarSystem())
                + " starClass=" + safe(e.getStarClass())
                + " taxi=" + e.isTaxi();
    }

    private static String formatFsdJump(String ts, EliteLogEvent.FsdJumpEvent e) {
        return ts + " [FSD_JUMP] system=" + safe(e.getStarSystem())
                + " body=" + safe(e.getBody())
                + " bodyType=" + safe(e.getBodyType())
                + " jumpDist=" + e.getJumpDist()
                + " fuelUsed=" + e.getFuelUsed()
                + " fuelLevel=" + e.getFuelLevel();
    }

    private static String formatFsdTarget(String ts, EliteLogEvent.FsdTargetEvent e) {
        return ts + " [FSD_TARGET] name=" + safe(e.getName())
                + " starClass=" + safe(e.getStarClass())
                + " remainingJumps=" + e.getRemainingJumpsInRoute();
    }

    private static String formatSaaSignalsFound(String ts, EliteLogEvent.SaasignalsFoundEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append(ts)
          .append(" [SAASIGNALS_FOUND] body=")
          .append(safe(e.getBodyName()));

        if (e.getSignals() != null && !e.getSignals().isEmpty()) {
            sb.append(" signals=");
            boolean first = true;
            for (EliteLogEvent.SaasignalsFoundEvent.Signal s : e.getSignals()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(s.getType());
                sb.append("(").append(s.getCount()).append(")");
            }
        }

        if (e.getGenuses() != null && !e.getGenuses().isEmpty()) {
            sb.append(" genuses=");
            boolean first = true;
            for (EliteLogEvent.SaasignalsFoundEvent.Genus g : e.getGenuses()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(g.getGenusLocalised() != null ? g.getGenusLocalised() : g.getGenus());
            }
        }

        return sb.toString();
    }

    private static String formatStatus(String ts, EliteLogEvent.StatusEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append(ts)
          .append(" [STATUS] flags=").append(e.getFlags())
          .append(" flags2=").append(e.getFlags2())
          .append(" guiFocus=").append(e.getGuiFocus())
          .append(" fuelMain=").append(e.getFuelMain())
          .append(" fuelRes=").append(e.getFuelReservoir())
          .append(" cargo=").append(e.getCargo())
          .append(" legal=").append(safe(e.getLegalState()))
          .append(" balance=").append(e.getBalance());

        int[] pips = e.getPips();
        if (pips != null && pips.length == 3) {
            sb.append(" pips=[")
              .append(pips[0]).append(",")
              .append(pips[1]).append(",")
              .append(pips[2]).append("]");
        }

        return sb.toString();
    }

    private static String formatReceiveText(String ts, EliteLogEvent.ReceiveTextEvent e) {
        return ts + " [RECEIVE_TEXT] from=" + safe(e.getFrom())
                + " channel=" + safe(e.getChannel())
                + " msg=" + safe(e.getMessageLocalised() != null
                                  ? e.getMessageLocalised()
                                  : e.getMessage());
    }

    /**
     * Fallback for generic/unknown events:
     *   <local-ts> [EventName] {json minus "timestamp" and "event"}
     */
    private static String formatGeneric(String ts, EliteLogEvent event) {
        String label = extractEventName(event);
        JsonObject raw = event.getRawJson();

        String jsonTail = "";
        if (raw != null) {
            JsonObject copy = raw.deepCopy();
            copy.remove("timestamp");
            copy.remove("event");
            String trimmed = copy.entrySet().isEmpty() ? "" : copy.toString();
            if (!trimmed.isEmpty()) {
                jsonTail = " " + trimmed;
            }
        }

        return ts + " [" + label + "]" + jsonTail;
    }

    /**
     * Try to get the raw journal "event" field; fall back to enum type.
     */
    private static String extractEventName(EliteLogEvent event) {
        if (event == null) {
            return "UNKNOWN";
        }
        JsonObject raw = event.getRawJson();
        if (raw != null && raw.has("event") && !raw.get("event").isJsonNull()) {
            try {
                return raw.get("event").getAsString();
            } catch (Exception ignored) {
                // fall through
            }
        }
        return event.getType() != null ? event.getType().name() : "UNKNOWN";
    }

    private static String safe(String s) {
        return s == null ? "<null>" : s;
    }
}
