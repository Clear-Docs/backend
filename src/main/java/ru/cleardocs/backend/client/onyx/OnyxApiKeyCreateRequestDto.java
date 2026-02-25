package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OnyxApiKeyCreateRequestDto(
    @JsonProperty("name") String name,
    @JsonProperty("role") String role
) {
}
