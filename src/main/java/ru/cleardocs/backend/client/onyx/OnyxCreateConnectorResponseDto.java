package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxCreateConnectorResponseDto(
    @JsonProperty("success") Boolean success,
    @JsonProperty("message") String message,
    @JsonProperty("data") Integer data
) {
}
