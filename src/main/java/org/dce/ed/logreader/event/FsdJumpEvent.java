package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class FsdJumpEvent extends EliteLogEvent {
    private final String starSystem;
    private final long systemAddress;
    private final double[] starPos;
    private final String body;
    private final int bodyId;
    private final String bodyType;
    private final double jumpDist;
    private final double fuelUsed;
    private final double fuelLevel;

    public FsdJumpEvent(Instant timestamp,
                        JsonObject rawJson,
                        String starSystem,
                        long systemAddress,
                        double[] starPos,
                        String body,
                        int bodyId,
                        String bodyType,
                        double jumpDist,
                        double fuelUsed,
                        double fuelLevel) {
        super(timestamp, EliteEventType.FSD_JUMP, rawJson);
        this.starSystem = starSystem;
        this.systemAddress = systemAddress;
        this.starPos = starPos;
        this.body = body;
        this.bodyId = bodyId;
        this.bodyType = bodyType;
        this.jumpDist = jumpDist;
        this.fuelUsed = fuelUsed;
        this.fuelLevel = fuelLevel;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public double[] getStarPos() {
        return starPos;
    }

    public String getBody() {
        return body;
    }

    public int getBodyId() {
        return bodyId;
    }

    public String getBodyType() {
        return bodyType;
    }

    public double getJumpDist() {
        return jumpDist;
    }

    public double getFuelUsed() {
        return fuelUsed;
    }

    public double getFuelLevel() {
        return fuelLevel;
    }
}
