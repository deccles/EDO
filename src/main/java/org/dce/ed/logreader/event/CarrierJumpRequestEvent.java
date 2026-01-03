package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public class CarrierJumpRequestEvent extends EliteLogEvent {

    private final String carrierType;
    private final long carrierId;
    private final String systemName;
    private final String body;
    private final long systemAddress;
    private final int bodyId;
    private final Instant departureTime;

    public CarrierJumpRequestEvent(Instant timestamp,
                                  JsonObject json,
                                  String carrierType,
                                  long carrierId,
                                  String systemName,
                                  String body,
                                  long systemAddress,
                                  int bodyId,
                                  Instant departureTime) {
        super(timestamp, EliteEventType.CARRIER_JUMP_REQUEST, json);
        this.carrierType = carrierType;
        this.carrierId = carrierId;
        this.systemName = systemName;
        this.body = body;
        this.systemAddress = systemAddress;
        this.bodyId = bodyId;
        this.departureTime = departureTime;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public long getCarrierId() {
        return carrierId;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getBody() {
        return body;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyId() {
        return bodyId;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }
}
