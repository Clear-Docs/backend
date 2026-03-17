package ru.cleardocs.backend.exception;

/**
 * Thrown when Onyx rejects document set update because the set is still syncing (indexing).
 */
public class DocumentSetSyncingException extends RuntimeException {
  public DocumentSetSyncingException(String message) {
    super(message);
  }
}
