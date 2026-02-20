package org.dce.ed;

import org.dce.ed.logreader.event.StatusEvent;

/**
 * Pure logic for System tab target/destination from Status events.
 * Extracted so "destination is other system" and effective dest name/body can be unit tested.
 */
public final class SystemTabTargetLogic {

    private SystemTabTargetLogic() {}

    /**
     * True when the Status destination refers to a different system (e.g. next jump target),
     * so we should not highlight any body in the current system.
     */
    public static boolean isDestinationOtherSystem(StatusEvent e, long currentSystemAddress) {
        if (e == null) {
            return false;
        }
        Long destSystem = e.getDestinationSystem();
        return destSystem != null
                && currentSystemAddress != 0L
                && destSystem.longValue() != currentSystemAddress;
    }

    /**
     * Destination body for the current system, or null if destination is another system or body id is 0.
     */
    public static Integer effectiveDestBody(StatusEvent e, long currentSystemAddress) {
        if (e == null || isDestinationOtherSystem(e, currentSystemAddress)) {
            return null;
        }
        Integer b = e.getDestinationBody();
        return (b != null && b.intValue() != 0) ? b : null;
    }

    /**
     * Destination display name when in current system, or null if destination is another system or blank.
     */
    public static String effectiveDestName(StatusEvent e, long currentSystemAddress) {
        if (e == null || isDestinationOtherSystem(e, currentSystemAddress)) {
            return null;
        }
        String s = e.getDestinationDisplayName();
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }
}
