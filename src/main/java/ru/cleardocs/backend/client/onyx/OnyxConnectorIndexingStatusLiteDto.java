package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxConnectorIndexingStatusLiteDto(
    @JsonProperty("cc_pair_id") Integer ccPairId,
    @JsonProperty("cc_pair_status") String ccPairStatus
) {
}
