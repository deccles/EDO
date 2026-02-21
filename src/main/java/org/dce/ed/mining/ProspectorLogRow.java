package org.dce.ed.mining;

import java.time.Instant;

/**
 * One row of prospector log data (run, body, timestamp, material, amounts, email).
 */
public final class ProspectorLogRow {

    private final int run;
    private final String fullBodyName;
    private final Instant timestamp;
    private final String material;
    private final double percent;
    private final double beforeAmount;
    private final double afterAmount;
    private final double difference;
    private final String emailAddress;

    public ProspectorLogRow(int run, String fullBodyName, Instant timestamp, String material,
                           double percent, double beforeAmount, double afterAmount, double difference,
                           String emailAddress) {
        this.run = run;
        this.fullBodyName = fullBodyName != null ? fullBodyName : "";
        this.timestamp = timestamp;
        this.material = material != null ? material : "";
        this.percent = percent;
        this.beforeAmount = beforeAmount;
        this.afterAmount = afterAmount;
        this.difference = difference;
        this.emailAddress = emailAddress != null ? emailAddress : "";
    }

    public int getRun() {
        return run;
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

    public String getEmailAddress() {
        return emailAddress;
    }
}
