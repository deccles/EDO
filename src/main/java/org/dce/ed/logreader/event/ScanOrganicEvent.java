package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * ScanOrganic – fired when you analyse an exobiology sample on a body.
 * Gives us the specific Genus/Species on that body.
 */
public final class ScanOrganicEvent extends EliteLogEvent {

    private final long systemAddress;
    private final String bodyName;
    private final int bodyId;
    private final String scanType;
    private final String genus;
    private final String genusLocalised;
    private final String species;
    private final String speciesLocalised;

    public ScanOrganicEvent(Instant timestamp,
                            JsonObject rawJson,
                            long systemAddress,
                            String bodyName,
                            int bodyId,
                            String scanType,
                            String genus,
                            String genusLocalised,
                            String species,
                            String speciesLocalised) {
        super(timestamp, EliteEventType.SCAN_ORGANIC, rawJson);
        
        this.systemAddress = systemAddress;
        this.bodyName = bodyName;
        this.bodyId = bodyId;
        this.scanType = scanType;
        this.genus = genus;
        this.genusLocalised = genusLocalised;
        this.species = species;
        this.speciesLocalised = speciesLocalised;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public String getBodyName() {
        return bodyName;
    }

    public int getBodyId() {
        return bodyId;
    }

    public String getScanType() {
        return scanType;
    }

    public String getGenus() {
        return genus;
    }

    public String getGenusLocalised() {
        return genusLocalised;
    }

    public String getSpecies() {
        return species;
    }

    public String getSpeciesLocalised() {
        return speciesLocalised;
    }
}
