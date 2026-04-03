package org.dce.ed.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuelScoopStarClassTest {

    @Test
    void obafgkm_and_subtypes_scoopable() {
        assertTrue(FuelScoopStarClass.isFuelScoopable("G"));
        assertTrue(FuelScoopStarClass.isFuelScoopable("K"));
        assertTrue(FuelScoopStarClass.isFuelScoopable("k"));
        assertTrue(FuelScoopStarClass.isFuelScoopable("K_ORANGE_SUPER_GIANT"));
        assertTrue(FuelScoopStarClass.isFuelScoopable("AeBe"));
    }

    @Test
    void non_scoopable_prefixes() {
        assertFalse(FuelScoopStarClass.isFuelScoopable(null));
        assertFalse(FuelScoopStarClass.isFuelScoopable(""));
        assertFalse(FuelScoopStarClass.isFuelScoopable(" "));
        assertFalse(FuelScoopStarClass.isFuelScoopable("N"));
        assertFalse(FuelScoopStarClass.isFuelScoopable("D"));
        assertFalse(FuelScoopStarClass.isFuelScoopable("H"));
        assertFalse(FuelScoopStarClass.isFuelScoopable("L"));
        assertFalse(FuelScoopStarClass.isFuelScoopable("TTS"));
        assertFalse(FuelScoopStarClass.isFuelScoopable("W"));
    }
}
