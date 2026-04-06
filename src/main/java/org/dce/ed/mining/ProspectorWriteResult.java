package org.dce.ed.mining;

/**
 * Result of a Google Sheets (or similar) prospector write operation.
 */
public final class ProspectorWriteResult {

    public enum Status {
        OK,
        FAILURE
    }

    private final Status status;
    private final String message;
    private final Throwable cause;

    private ProspectorWriteResult(Status status, String message, Throwable cause) {
        this.status = status;
        this.message = message != null ? message : "";
        this.cause = cause;
    }

    public static ProspectorWriteResult ok() {
        return new ProspectorWriteResult(Status.OK, "", null);
    }

    public static ProspectorWriteResult failure(String message) {
        return new ProspectorWriteResult(Status.FAILURE, message, null);
    }

    public static ProspectorWriteResult failure(String message, Throwable cause) {
        return new ProspectorWriteResult(Status.FAILURE, message, cause);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}
