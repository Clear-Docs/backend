package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OnyxDeletionAttemptRequestDto(
    @JsonProperty("connector_id") int connectorId,
    @JsonProperty("credential_id") int credentialId
) {
}
