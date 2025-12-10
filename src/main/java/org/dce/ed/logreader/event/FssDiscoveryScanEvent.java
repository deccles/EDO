package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * FSSDiscoveryScan ("honk") – summary of bodies known in the system.
 */
public final class FssDiscoveryScanEvent extends EliteLogEvent {

    private final double progress;
    private final int bodyCount;
    private final int nonBodyCount;
    private final String systemName;
    private final long systemAddress;

    public FssDiscoveryScanEvent(Instant timestamp,
                                 JsonObject rawJson,
                                 double progress,
                                 int bodyCount,
                                 int nonBodyCount,
                                 String systemName,
                                 long systemAddress) {
        super(timestamp, EliteEventType.FSS_DISCOVERY_SCAN, rawJson);
        this.progress = progress;
        this.bodyCount = bodyCount;
        this.nonBodyCount = nonBodyCount;
        this.systemName = systemName;
        this.systemAddress = systemAddress;
    }

    public double getProgress() {
        return progress;
    }

    public int getBodyCount() {
        return bodyCount;
    }

    public int getNonBodyCount() {
        return nonBodyCount;
    }

    public String getSystemName() {
        return systemName;
    }

    public long getSystemAddress() {
        return systemAddress;
    }
}