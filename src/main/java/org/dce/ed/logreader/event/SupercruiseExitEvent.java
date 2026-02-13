package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class SupercruiseExitEvent extends EliteLogEvent {

    private final boolean taxi;
    private final boolean multicrew;
    private final String starSystem;
    private final long systemAddress;
    private final String body;
    private final int bodyId;
    private final String bodyType;

    public SupercruiseExitEvent(Instant timestamp,
                               JsonObject rawJson,
                               boolean taxi,
                               boolean multicrew,
                               String starSystem,
                               long systemAddress,
                               String body,
                               int bodyId,
                               String bodyType) {
        super(timestamp, EliteEventType.SUPERCRUISE_EXIT, rawJson);
        this.taxi = taxi;
        this.multicrew = multicrew;
        this.starSystem = starSystem;
        this.systemAddress = systemAddress;
        this.body = body;
        this.bodyId = bodyId;
        this.bodyType = bodyType;
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
