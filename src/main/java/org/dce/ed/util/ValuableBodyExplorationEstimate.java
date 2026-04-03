package org.dce.ed.util;

import java.util.Locale;

import org.dce.ed.state.BodyInfo;

/**
 * Fallback tier credits when mass/terraform data is missing, plus display formatting.
 * Primary estimates use {@link ExplorationBodyCredits} (FSS + DSS + Odyssey mapping + efficient map).
 */
public final class ValuableBodyExplorationEstimate {

    private static final long ELW_TYPICAL = 1_200_000L;
    private static final long WW_AW_TYPICAL = 500_000L;

    /** Used when a body is high-value but class/terraform parsing yields no tier (should be rare). */
    public static final long TERRAFORMABLE_FALLBACK = 300_000L;

    private ValuableBodyExplorationEstimate() {
    }

    private static String toLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /**
     * @return credits to display, or null if the body is not a valuable exploration target by class/terraform
     */
    public static Long estimateCredits(String planetClass, String terraformState) {
        String pc = toLower(planetClass);
        String tf = toLower(terraformState);
        boolean terraformable = tf.contains("terraformable");
        if (pc.contains("earth-like") || pc.contains("earthlike")) {
            return Long.valueOf(ELW_TYPICAL);
        }
        if (pc.contains("water world")) {
            return Long.valueOf(WW_AW_TYPICAL);
        }
        if (pc.contains("ammonia world")) {
            return Long.valueOf(WW_AW_TYPICAL);
        }
        if (terraformable) {
            return Long.valueOf(TERRAFORMABLE_FALLBACK);
        }
        return null;
    }

    /**
     * Credits for UI: {@link ExplorationBodyCredits} when possible, else persisted/cache tier fallback.
     */
    public static long resolveCreditsForDisplay(BodyInfo b) {
        if (b == null || !b.isHighValue()) {
            return 0L;
        }
        long fromFormula = ExplorationBodyCredits.achievableExplorationTotalCredits(b);
        if (fromFormula > 0) {
            return fromFormula;
        }
        Long stored = b.getValuableBodyExplorationCredits();
        if (stored != null && stored.longValue() > 0) {
            return stored.longValue();
        }
        Long est = estimateCredits(b.getPlanetClass(), b.getTerraformState());
        return est != null ? est.longValue() : TERRAFORMABLE_FALLBACK;
    }

    public static String formatCredits(long credits) {
        if (credits <= 0) {
            return "";
        }
        return ExplorationBodyCredits.formatAbbreviatedCredits(credits) + " Cr";
    }
}
