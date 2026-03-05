package org.dce.ed.util;

import java.util.Collections;
import java.util.List;

/**
 * Result of fetching body exobiology data from Spansh (landmarks + signals-derived exclusion).
 * When excludeFromExobiology is true, Spansh has signals listed for the body but none are Biological,
 * so the body can be excluded from exobiology lists.
 */
public final class SpanshBodyExobiologyInfo {

    private final List<SpanshLandmark> landmarks;
    private final boolean excludeFromExobiology;

    public SpanshBodyExobiologyInfo(List<SpanshLandmark> landmarks, boolean excludeFromExobiology) {
        this.landmarks = landmarks != null ? landmarks : Collections.emptyList();
        this.excludeFromExobiology = excludeFromExobiology;
    }

    public List<SpanshLandmark> getLandmarks() {
        return landmarks;
    }

    /**
     * True when Spansh has signals for this body and none are Biological (exobiology).
     * Such bodies can be eliminated from exobiology lists.
     */
    public boolean isExcludeFromExobiology() {
        return excludeFromExobiology;
    }
}
