package org.dce.ed;

import java.util.List;

import org.dce.ed.logreader.event.FsdTargetEvent;
import org.dce.ed.logreader.event.StatusEvent;

/**
 * Holds route target and destination state driven by journal events.
 * Encapsulates "side-trip clearing" logic so it can be unit tested.
 * Used by {@link RouteTabPanel}; UI and file I/O remain in the panel.
 */
public final class RouteTargetState {

    private String targetSystemName = null;
    private long targetSystemAddress = 0L;
    private Long destinationSystemAddress = null;
    private Integer destinationBodyId = null;
    private String destinationName = null;

    /** True when the last apply cleared the target (side-trip clear). */
    private boolean clearedSideTrip = false;

    public record RouteSystemRef(String systemName, long systemAddress) {}

    public String getTargetSystemName() {
        return targetSystemName;
    }

    public long getTargetSystemAddress() {
        return targetSystemAddress;
    }

    public Long getDestinationSystemAddress() {
        return destinationSystemAddress;
    }

    public Integer getDestinationBodyId() {
        return destinationBodyId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    /** True if the last {@link #applyStatusEvent} cleared the target (side-trip). */
    public boolean wasSideTripCleared() {
        return clearedSideTrip;
    }

    /** Clears target and destination (e.g. on NavRouteClear). */
    public void applyNavRouteClear() {
        targetSystemName = null;
        targetSystemAddress = 0L;
        destinationSystemAddress = null;
        destinationBodyId = null;
        destinationName = null;
        clearedSideTrip = false;
    }

    /** Updates target from FSD target event when not in hyperspace and jump timer not running. */
    public void applyFsdTargetEvent(FsdTargetEvent e, boolean inHyperspace, boolean timerRunning) {
        clearedSideTrip = false;
        if (inHyperspace || timerRunning) {
            return;
        }
        String newName = e != null ? e.getName() : null;
        long newAddr = e != null ? e.getSystemAddress() : 0L;
        if (newName == null || newName.isBlank() || newAddr == 0L) {
            targetSystemName = null;
            targetSystemAddress = 0L;
        } else {
            targetSystemName = newName;
            targetSystemAddress = newAddr;
        }
    }

    /**
     * Updates destination from Status and applies side-trip clearing:
     * if Status destination is blank or refers to a system on the route,
     * clear any off-route target.
     *
     * @param e         Status event
     * @param baseRoute Route systems (name + address) for "on route" check
     * @return true if the target was cleared (caller should rebuild and return early)
     */
    public boolean applyStatusEvent(StatusEvent e, List<RouteSystemRef> baseRoute) {
        clearedSideTrip = false;
        if (e == null) {
            return false;
        }
        destinationSystemAddress = e.getDestinationSystem();
        Integer body = e.getDestinationBody();
        destinationBodyId = (body != null && body.intValue() != 0) ? body : null;
        destinationName = e.getDestinationDisplayName();

        String statusDestName = destinationName;
        if (statusDestName == null || statusDestName.isBlank()) {
            if (targetSystemName != null) {
                targetSystemName = null;
                targetSystemAddress = 0L;
                clearedSideTrip = true;
            }
            return clearedSideTrip;
        }

        boolean statusDestIsOnRoute = false;
        boolean targetIsOnRoute = false;
        if (baseRoute != null && !baseRoute.isEmpty()) {
            for (RouteSystemRef ref : baseRoute) {
                if (ref == null) {
                    continue;
                }
                if (statusDestName.equals(ref.systemName())) {
                    statusDestIsOnRoute = true;
                }
                if (targetSystemName != null && !targetSystemName.isBlank()) {
                    if (targetSystemName.equals(ref.systemName())) {
                        targetIsOnRoute = true;
                    }
                }
                if (targetSystemAddress != 0L && ref.systemAddress() != 0L) {
                    if (ref.systemAddress() == targetSystemAddress) {
                        targetIsOnRoute = true;
                    }
                }
                if (statusDestIsOnRoute && targetIsOnRoute) {
                    break;
                }
            }
        }

        if (statusDestIsOnRoute && targetSystemName != null && !targetIsOnRoute) {
            targetSystemName = null;
            targetSystemAddress = 0L;
            clearedSideTrip = true;
        }
        return clearedSideTrip;
    }
}
