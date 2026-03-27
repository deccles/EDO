package org.dce.ed;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import org.junit.jupiter.api.Test;

/**
 * Two tabs ⇒ two {@link RouteSession} instances; base route lists must not be shared.
 */
class FleetCarrierTabPanelIsolationTest {

    @Test
    void shipAndFleetTabsUseDistinctRouteSessions() {
        RouteTabPanel ship = new RouteTabPanel(() -> false);
        FleetCarrierTabPanel fleet = new FleetCarrierTabPanel(() -> false);
        Object a = ship.routeSessionForTests();
        Object b = fleet.routeSessionForTests();
        assertNotSame(a, b);
    }
}
