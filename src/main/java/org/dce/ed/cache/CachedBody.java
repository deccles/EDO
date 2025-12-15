package org.dce.ed.cache;

import java.util.Set;

/**
 * Represents one body as stored in the cache.
 */
public class CachedBody {
    public String name;
    public int bodyId;
    public String starSystem;
	public double[] starPos;
	
    public double distanceLs;
    public Double gravityMS;
    public boolean landable;
    public boolean hasBio;
    public boolean hasGeo;
    public boolean highValue;

    public String planetClass;
    public String atmosphere;
    public String atmoOrType;

    public Double surfaceTempK;
    public String volcanism;

    public String bodyName;
    public String parentStar;
    public String nebula; 
    
    // NEW: raw EDSM discovery info
    public String discoveryCommander;

    // NEW: confirmed genera observed (ScanOrganic / DSS)
    public Set<String> observedGenusPrefixes;   // may be null if none known
    
    // Full "truth" names like "Bacterium Nebulus", "Stratum Tectonicas", etc.
    public Set<String> observedBioDisplayNames;  // may be null

}