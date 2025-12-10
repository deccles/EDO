package org.dce.ed.logreader;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.dce.ed.logreader.event.CommanderEvent;
import org.dce.ed.logreader.event.FileheaderEvent;
import org.dce.ed.logreader.event.FsdJumpEvent;
import org.dce.ed.logreader.event.FsdTargetEvent;
import org.dce.ed.logreader.event.LoadGameEvent;
import org.dce.ed.logreader.event.LocationEvent;
import org.dce.ed.logreader.event.ReceiveTextEvent;
import org.dce.ed.logreader.event.SaasignalsFoundEvent;
import org.dce.ed.logreader.event.StartJumpEvent;
import org.dce.ed.logreader.event.StatusEvent;

import com.google.gson.JsonObject;

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
                return formatFileheader(ts, (FileheaderEvent) event);
            case COMMANDER:
                return formatCommander(ts, (CommanderEvent) event);
            case LOAD_GAME:
                return formatLoadGame(ts, (LoadGameEvent) event);
            case LOCATION:
                return formatLocation(ts, (LocationEvent) event);
            case START_JUMP:
                return formatStartJump(ts, (StartJumpEvent) event);
            case FSD_JUMP:
                return formatFsdJump(ts, (FsdJumpEvent) event);
            case FSD_TARGET:
                return formatFsdTarget(ts, (FsdTargetEvent) event);
            case SAASIGNALS_FOUND:
                return formatSaaSignalsFound(ts, (SaasignalsFoundEvent) event);
            case NAV_ROUTE:
                return ts + " [NAV_ROUTE] Nav route updated";
            case NAV_ROUTE_CLEAR:
                return ts + " [NAV_ROUTE_CLEAR] Nav route cleared";
            case STATUS:
                return formatStatus(ts, (StatusEvent) event);
            case RECEIVE_TEXT:
                return formatReceiveText(ts, (ReceiveTextEvent) event);
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

    private static String formatFileheader(String ts, FileheaderEvent e) {
        return ts + " [FILEHEADER] part=" + e.getPart()
                + " odyssey=" + e.isOdyssey()
                + " gameVersion=" + safe(e.getGameVersion())
                + " build=" + safe(e.getBuild());
    }

    private static String formatCommander(String ts, CommanderEvent e) {
        return ts + " [COMMANDER] name=" + safe(e.getName())
                + " fid=" + safe(e.getFid());
    }

    private static String formatLoadGame(String ts, LoadGameEvent e) {
        return ts + " [LOAD_GAME] commander=" + safe(e.getCommander())
                + " ship=" + safe(e.getShip())
                + " shipName=" + safe(e.getShipName())
                + " fuel=" + e.getFuelLevel() + "/" + e.getFuelCapacity()
                + " mode=" + safe(e.getGameMode())
                + " credits=" + e.getCredits();
    }

    private static String formatLocation(String ts, LocationEvent e) {
        return ts + " [LOCATION] system=" + safe(e.getStarSystem())
                + " body=" + safe(e.getBody())
                + " bodyType=" + safe(e.getBodyType())
                + " docked=" + e.isDocked()
                + " taxi=" + e.isTaxi()
                + " multicrew=" + e.isMulticrew();
    }

    private static String formatStartJump(String ts, StartJumpEvent e) {
        return ts + " [START_JUMP] type=" + safe(e.getJumpType())
                + " system=" + safe(e.getStarSystem())
                + " starClass=" + safe(e.getStarClass())
                + " taxi=" + e.isTaxi();
    }

    private static String formatFsdJump(String ts, FsdJumpEvent e) {
        return ts + " [FSD_JUMP] system=" + safe(e.getStarSystem())
                + " body=" + safe(e.getBody())
                + " bodyType=" + safe(e.getBodyType())
                + " jumpDist=" + e.getJumpDist()
                + " fuelUsed=" + e.getFuelUsed()
                + " fuelLevel=" + e.getFuelLevel();
    }

    private static String formatFsdTarget(String ts, FsdTargetEvent e) {
        return ts + " [FSD_TARGET] name=" + safe(e.getName())
                + " starClass=" + safe(e.getStarClass())
                + " remainingJumps=" + e.getRemainingJumpsInRoute();
    }

    private static String formatSaaSignalsFound(String ts, SaasignalsFoundEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append(ts)
          .append(" [SAASIGNALS_FOUND] body=")
          .append(safe(e.getBodyName()));

        if (e.getSignals() != null && !e.getSignals().isEmpty()) {
            sb.append(" signals=");
            boolean first = true;
            for (SaasignalsFoundEvent.Signal s : e.getSignals()) {
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
            for (SaasignalsFoundEvent.Genus g : e.getGenuses()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(g.getGenusLocalised() != null ? g.getGenusLocalised() : g.getGenus());
            }
        }

        return sb.toString();
    }

    private static String formatStatus(String ts, StatusEvent e) {
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

    private static String formatReceiveText(String ts, ReceiveTextEvent e) {
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
