package org.dce.ed.route;

/**
 * Fuel scoop eligibility from Elite Dangerous journal / NavRoute {@code StarClass} strings.
 * <p>
 * Scoopable stars are main-sequence O, B, A, F, G, K, M and their in-game subclasses
 * (e.g. {@code K_ORANGE_SUPER_GIANT}), which all begin with one of those letters.
 */
public final class FuelScoopStarClass {

    private FuelScoopStarClass() {
    }

    public static boolean isFuelScoopable(String starClass) {
        if (starClass == null) {
            return false;
        }
        String s = starClass.trim();
        if (s.isEmpty()) {
            return false;
        }
        char c = Character.toUpperCase(s.charAt(0));
        return c == 'O' || c == 'B' || c == 'A' || c == 'F' || c == 'G' || c == 'K' || c == 'M';
    }
}
