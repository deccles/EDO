package org.dce.ed.cache;

/**
 * Lightweight system cache metadata for fast status checks.
 */
public final class CachedSystemSummary {
    public final long systemAddress;
    public final String systemName;
    public final Integer totalBodies;
    public final Double fssProgress;
    public final Boolean allBodiesFound;
    public final int cachedBodyCount;

    public CachedSystemSummary(long systemAddress,
            String systemName,
            Integer totalBodies,
            Double fssProgress,
            Boolean allBodiesFound,
            int cachedBodyCount) {
        this.systemAddress = systemAddress;
        this.systemName = systemName;
        this.totalBodies = totalBodies;
        this.fssProgress = fssProgress;
        this.allBodiesFound = allBodiesFound;
        this.cachedBodyCount = cachedBodyCount;
    }
}
