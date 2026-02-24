package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxCredentialSnapshotDto(
    @JsonProperty("id") Integer id
) {
}
