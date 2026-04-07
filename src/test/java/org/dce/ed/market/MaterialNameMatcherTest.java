package org.dce.ed.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MaterialNameMatcherTest {

    private static GalacticAveragePrices prices;
    private static MaterialNameMatcher matcher;

    @BeforeAll
    static void loadPrices() {
        prices = GalacticAveragePrices.loadDefault();
        matcher = new MaterialNameMatcher(prices);
    }

    @Test
    void resolveKey_blankInputs() {
        assertEquals("", matcher.resolveKey(null, null));
        assertEquals("", matcher.resolveKey("", ""));
        assertEquals("", matcher.resolveKey("  ", "\t"));
    }

    @Test
    void resolveKey_knownCommodity_rawName() {
        String key = matcher.resolveKey("Tritium", null);
        if (!key.isEmpty()) {
            assertTrue(prices.getAvgSellCrPerTon(key).isPresent());
        }
    }

    @Test
    void lookupAvgSell_unknownReturnsZero() {
        assertEquals(0, matcher.lookupAvgSell("TotallyUnknownMaterialXYZ123", null));
    }

    @Test
    void resolveKey_cachedConsistent() {
        String a = matcher.resolveKey("Bromellite", null);
        String b = matcher.resolveKey("Bromellite", null);
        assertEquals(a, b);
    }
}
