package org.dce.ed.logreader.event;

import java.time.Instant;

import org.dce.ed.logreader.EliteEventType;
import org.dce.ed.logreader.EliteLogEvent;

import com.google.gson.JsonObject;

public final class LoadGameEvent extends EliteLogEvent {
    private final String commander;
    private final String fid;
    private final String ship;
    private final int shipId;
    private final String shipName;
    private final String shipIdent;
    private final double fuelLevel;
    private final double fuelCapacity;
    private final String gameMode;
    private final long credits;

    public LoadGameEvent(Instant timestamp,
                         JsonObject rawJson,
                         String commander,
                         String fid,
                         String ship,
                         int shipId,
                         String shipName,
                         String shipIdent,
                         double fuelLevel,
                         double fuelCapacity,
                         String gameMode,
                         long credits) {
        super(timestamp, EliteEventType.LOAD_GAME, rawJson);
        this.commander = commander;
        this.fid = fid;
        this.ship = ship;
        this.shipId = shipId;
        this.shipName = shipName;
        this.shipIdent = shipIdent;
        this.fuelLevel = fuelLevel;
        this.fuelCapacity = fuelCapacity;
        this.gameMode = gameMode;
        this.credits = credits;
    }

    public String getCommander() {
        return commander;
    }

    public String getFid() {
        return fid;
    }

    public String getShip() {
        return ship;
    }

    public int getShipId() {
        return shipId;
    }

    public String getShipName() {
        return shipName;
    }

    public String getShipIdent() {
        return shipIdent;
    }

    public double getFuelLevel() {
        return fuelLevel;
    }

    public double getFuelCapacity() {
        return fuelCapacity;
    }

    public String getGameMode() {
        return gameMode;
    }

    public long getCredits() {
        return credits;
    }
}
