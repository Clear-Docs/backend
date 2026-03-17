package ru.cleardocs.backend.exception;

/**
 * Thrown when Onyx rejects connector creation because a connector with this name already exists.
 * Used to retry with a new unique name (same pattern as document set naming).
 */
public class ConnectorNameDuplicateException extends RuntimeException {
  private final String rejectedName;

  public ConnectorNameDuplicateException(String rejectedName) {
    super("Connector name already exists: " + rejectedName);
    this.rejectedName = rejectedName;
  }

  public String getRejectedName() {
    return rejectedName;
  }
}
