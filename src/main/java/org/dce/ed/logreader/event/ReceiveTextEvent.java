package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public final class ReceiveTextEvent extends EliteLogEvent {
    private final String from;
    private final String message;
    private final String messageLocalised;
    private final String channel;

    public ReceiveTextEvent(Instant timestamp,
                            JsonObject rawJson,
                            String from,
                            String message,
                            String messageLocalised,
                            String channel) {
        super(timestamp, EliteEventType.RECEIVE_TEXT, rawJson);
        this.from = from;
        this.message = message;
        this.messageLocalised = messageLocalised;
        this.channel = channel;
    }

    public String getFrom() {
        return from;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageLocalised() {
        return messageLocalised;
    }

    public String getChannel() {
        return channel;
    }
}
