package ru.cleardocs.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateConnectorRequestDto(
    @JsonProperty("status") String status
) {
  public boolean isPaused() {
    return status != null && "paused".equalsIgnoreCase(status);
  }

  public boolean isActive() {
    return status != null && "active".equalsIgnoreCase(status);
  }
}
