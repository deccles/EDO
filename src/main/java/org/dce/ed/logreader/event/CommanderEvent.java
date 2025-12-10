package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class CommanderEvent extends EliteLogEvent {
    private final String fid;
    private final String name;

    public CommanderEvent(Instant timestamp,
                          JsonObject rawJson,
                          String fid,
                          String name) {
        super(timestamp, EliteEventType.COMMANDER, rawJson);
        this.fid = fid;
        this.name = name;
    }

    public String getFid() {
        return fid;
    }

    public String getName() {
        return name;
    }
}