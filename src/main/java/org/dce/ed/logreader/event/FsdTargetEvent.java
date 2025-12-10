package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class FsdTargetEvent extends EliteLogEvent {
    private final String name;
    private final long systemAddress;
    private final String starClass;
    private final int remainingJumpsInRoute;

    public FsdTargetEvent(Instant timestamp,
                          JsonObject rawJson,
                          String name,
                          long systemAddress,
                          String starClass,
                          int remainingJumpsInRoute) {
        super(timestamp, EliteEventType.FSD_TARGET, rawJson);
        this.name = name;
        this.systemAddress = systemAddress;
        this.starClass = starClass;
        this.remainingJumpsInRoute = remainingJumpsInRoute;
    }

    public String getName() {
        return name;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public String getStarClass() {
        return starClass;
    }

    public int getRemainingJumpsInRoute() {
        return remainingJumpsInRoute;
    }
}