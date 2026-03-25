package org.dce.ed.cache;

import java.util.ArrayList;
import java.util.List;

/**
     * Represents a cached system and its bodies.
     */
    public class CachedSystem {
        public long systemAddress;
        public String systemName;
        public double starPos[];
        public List<CachedBody> bodies = new ArrayList<>();

    /**
     * Exobiology running total (expected credits, unsold).
     * Stored in the same cache payload as the last visited system so it persists
     * across tool restarts.
     */
    public Long exobiologyCreditsTotalUnsold;

        public Integer totalBodies;
        public Integer nonBodyCount;
        public Double fssProgress;
        public Boolean allBodiesFound;
    }
