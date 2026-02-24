package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxConnectorIndexingStatusLiteResponseDto(
    @JsonProperty("indexing_statuses") List<OnyxConnectorIndexingStatusLiteDto> indexingStatuses
) {
  public OnyxConnectorIndexingStatusLiteResponseDto {
    indexingStatuses = indexingStatuses != null ? indexingStatuses : Collections.emptyList();
  }
}
