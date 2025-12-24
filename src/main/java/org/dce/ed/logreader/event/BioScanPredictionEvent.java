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

	public Boolean getBonusApplies() {
		return bonusApplies;
	}

	public void setBonusApplies(Boolean bonusApplies) {
		this.bonusApplies = bonusApplies;
	}

	private final String bodyName;
    private final int bodyId;
    private final String starSystem;
    private final List<BioCandidate> candidates;
	private Boolean bonusApplies;
    
    public BioScanPredictionEvent(Instant timestamp,
                     JsonObject rawJson,
                     String bodyName,
                     int bodyId,
                     String starSystem,
                     Boolean bonusApplies,
                     List<BioCandidate> candidates) {
        super(timestamp, EliteEventType.SCAN, rawJson);
        this.bodyName = bodyName;
        this.bodyId = bodyId;
        this.starSystem = starSystem;
        this.bonusApplies = bonusApplies;
        this.candidates = candidates;
    }

    public List<BioCandidate> getCandidates() {
		return candidates;
	}
    
	public String getBodyName() {
        return bodyName.replaceAll(starSystem,  "");
    }

    public int getBodyId() {
        return bodyId;
    }
    
    public String getStarSystem() {
		return starSystem;
	}
}