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

        public Integer totalBodies;
        public Integer nonBodyCount;
        public Double fssProgress;
        public Boolean allBodiesFound;
    }
