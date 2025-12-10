package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * FSSAllBodiesFound – fired when the game considers all bodies in the
 * current system discovered via the FSS.
 */
public class FssAllBodiesFoundEvent extends EliteLogEvent {

    private final String systemName;
    private final long systemAddress;
    private final int bodyCount;

    public FssAllBodiesFoundEvent(Instant timestamp,
                                  JsonObject rawJson,
                                  String systemName,
                                  long systemAddress,
                                  int bodyCount) {
        super(timestamp, EliteEventType.FSS_ALL_BODIES_FOUND, rawJson);
        this.systemName = systemName;
        this.systemAddress = systemAddress;
        this.bodyCount = bodyCount;
    }

    public String getSystemName() {
        return systemName;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyCount() {
        return bodyCount;
    }
}


