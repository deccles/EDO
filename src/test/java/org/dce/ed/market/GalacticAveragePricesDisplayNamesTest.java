package org.dce.ed.market;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class GalacticAveragePricesDisplayNamesTest {

    @Test
    void displayNamesIncludeTritiumWhenBundledCsvPresent() {
        GalacticAveragePrices p = GalacticAveragePrices.loadDefault();
        List<String> names = p.getAllDisplayNamesSorted();
        assertTrue(names.stream().anyMatch(s -> s.equalsIgnoreCase("tritium")),
                "Expected Tritium in INARA display names for voice-cache warming");
    }
}
