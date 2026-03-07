package org.dce.ed.mining;

import java.time.Instant;

/**
 * One row of prospector log data (run, asteroid, body, timestamp, material, amounts, core, commander, duds).
 * Column order: Run, Asteroid, Timestamp, Type, Percentage, Before Amount, After Amount, Actual, Core, Body, Duds, Commander.
 */
public final class ProspectorLogRow {

    private final int run;
    private final String asteroidId;
    private final String fullBodyName;
    private final Instant timestamp;
    private final String material;
    private final double percent;
    private final double beforeAmount;
    private final double afterAmount;
    private final double difference;
    private final String commanderName;
    private final String coreType;
    private final int duds;

    /** Constructor for backward compatibility (asteroid "", core "", duds 0). */
    public ProspectorLogRow(int run, String fullBodyName, Instant timestamp, String material,
                           double percent, double beforeAmount, double afterAmount, double difference,
                           String commanderName) {
        this(run, "", fullBodyName, timestamp, material, percent, beforeAmount, afterAmount, difference, commanderName, "", 0);
    }

    public ProspectorLogRow(int run, String asteroidId, String fullBodyName, Instant timestamp, String material,
                           double percent, double beforeAmount, double afterAmount, double difference,
                           String commanderName, String coreType, int duds) {
        this.run = run;
        this.asteroidId = asteroidId != null ? asteroidId : "";
        this.fullBodyName = fullBodyName != null ? fullBodyName : "";
        this.timestamp = timestamp;
        this.material = material != null ? material : "";
        this.percent = percent;
        this.beforeAmount = beforeAmount;
        this.afterAmount = afterAmount;
        this.difference = difference;
        this.commanderName = commanderName != null ? commanderName : "";
        this.coreType = coreType != null ? coreType : "";
        this.duds = duds;
    }

    public int getRun() {
        return run;
    }

    /** Asteroid ID in order: A, B, ..., Z, AA, AB, ... */
    public String getAsteroidId() {
        return asteroidId;
    }

    public String getFullBodyName() {
        return fullBodyName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMaterial() {
        return material;
    }

    public double getPercent() {
        return percent;
    }

    public double getBeforeAmount() {
        return beforeAmount;
    }

    public double getAfterAmount() {
        return afterAmount;
    }

    public double getDifference() {
        return difference;
    }

    public String getCommanderName() {
        return commanderName;
    }

    /** Core (motherlode) material type if this was a core asteroid, otherwise empty. */
    public String getCoreType() {
        return coreType;
    }

    /** Number of prospector limpets fired (duds) before this one that generated inventory. */
    public int getDuds() {
        return duds;
    }

    /** @deprecated Use {@link #getCommanderName()}. */
    public String getEmailAddress() {
        return commanderName;
    }
}
