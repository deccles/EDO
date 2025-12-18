package org.dce.ed.logreader.event;

import java.time.Instant;
import java.util.List;

import org.dce.ed.exobiology.ExobiologyData.BioCandidate;
import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * Scan event for stars and bodies.
 * Backed by the journal "Scan" event.
 */
public class BioScanPredictionEvent extends EliteLogEvent {
	
    private final String bodyName;
    private final int bodyId;
    private final String starSystem;
    private final List<BioCandidate> candidates;

    public BioScanPredictionEvent(Instant timestamp,
                     JsonObject rawJson,
                     String bodyName,
                     int bodyId,
                     String starSystem,
                     List<BioCandidate> candidates) {
        super(timestamp, EliteEventType.SCAN, rawJson);
        this.bodyName = bodyName;
        this.bodyId = bodyId;
        this.starSystem = starSystem;
        this.candidates = candidates;
    }

	public String getBodyName() {
        return bodyName;
    }

    public int getBodyId() {
        return bodyId;
    }
    
    public String getStarSystem() {
		return starSystem;
	}
}