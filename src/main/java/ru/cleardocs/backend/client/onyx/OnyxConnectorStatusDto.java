package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxConnectorStatusDto(
    @JsonProperty("cc_pair_id") Integer ccPairId,
    @JsonProperty("connector") OnyxConnectorSnapshotDto connector,
    @JsonProperty("credential") OnyxCredentialSnapshotDto credential
) {
}
