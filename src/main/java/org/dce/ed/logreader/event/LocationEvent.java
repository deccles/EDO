package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class LocationEvent extends EliteLogEvent {
    private final boolean docked;
    private final boolean taxi;
    private final boolean multicrew;
    private final String starSystem;
    private final long systemAddress;
    private final double[] starPos;
    private final String body;
    private final int bodyId;
    private final String bodyType;

    public LocationEvent(Instant timestamp,
                         JsonObject rawJson,
                         boolean docked,
                         boolean taxi,
                         boolean multicrew,
                         String starSystem,
                         long systemAddress,
                         double[] starPos,
                         String body,
                         int bodyId,
                         String bodyType) {
        super(timestamp, EliteEventType.LOCATION, rawJson);
        this.docked = docked;
        this.taxi = taxi;
        this.multicrew = multicrew;
        this.starSystem = starSystem;
        this.systemAddress = systemAddress;
        this.starPos = starPos;
        this.body = body;
        this.bodyId = bodyId;
        this.bodyType = bodyType;
    }

    public boolean isDocked() {
        return docked;
    }

    public boolean isTaxi() {
        return taxi;
    }

    public boolean isMulticrew() {
        return multicrew;
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
}

