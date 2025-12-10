package org.dce.ed.logreader.event;

import java.time.Instant;
import java.util.List;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

/**
 * FSSBodySignals – very similar to SAASignalsFound, but coming from the FSS.
 * We reuse the SaasignalsFoundEvent.Signal type for convenience.
 */
public final class FssBodySignalsEvent extends EliteLogEvent {

    private final String bodyName;
    private final long systemAddress;
    private final int bodyId;
    private final List<SaasignalsFoundEvent.Signal> signals;

    public FssBodySignalsEvent(Instant timestamp,
                               JsonObject rawJson,
                               String bodyName,
                               long systemAddress,
                               int bodyId,
                               List<SaasignalsFoundEvent.Signal> signals) {

        super(timestamp, EliteEventType.FSS_BODY_SIGNAL_DISCOVERED, rawJson);
        this.bodyName = bodyName;
        this.systemAddress = systemAddress;
        this.bodyId = bodyId;
        this.signals = signals;
    }

    public String getBodyName() {
        return bodyName;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyId() {
        return bodyId;
    }

    public List<SaasignalsFoundEvent.Signal> getSignals() {
        return signals;
    }
}