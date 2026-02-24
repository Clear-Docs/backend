package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OnyxIndexingStatusRequestDto(
    @JsonProperty("get_all_connectors") boolean getAllConnectors
) {
  public static OnyxIndexingStatusRequestDto allConnectors() {
    return new OnyxIndexingStatusRequestDto(true);
  }
}
