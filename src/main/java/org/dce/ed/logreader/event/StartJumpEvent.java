package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class StartJumpEvent extends EliteLogEvent {
    private final String jumpType;
    private final boolean taxi;
    private final String starSystem;
    private final Long systemAddress;
    private final String starClass;

    public StartJumpEvent(Instant timestamp,
                          JsonObject rawJson,
                          String jumpType,
                          boolean taxi,
                          String starSystem,
                          Long systemAddress,
                          String starClass) {
        super(timestamp, EliteEventType.START_JUMP, rawJson);
        this.jumpType = jumpType;
        this.taxi = taxi;
        this.starSystem = starSystem;
        this.systemAddress = systemAddress;
        this.starClass = starClass;
    }

    public String getJumpType() {
        return jumpType;
    }

    public boolean isTaxi() {
        return taxi;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public Long getSystemAddress() {
        return systemAddress;
    }

    public String getStarClass() {
        return starClass;
    }
}