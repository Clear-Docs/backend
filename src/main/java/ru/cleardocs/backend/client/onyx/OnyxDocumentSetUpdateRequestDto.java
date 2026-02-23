package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record OnyxDocumentSetUpdateRequestDto(
    @JsonProperty("id") int id,
    @JsonProperty("description") String description,
    @JsonProperty("cc_pair_ids") List<Integer> ccPairIds,
    @JsonProperty("is_public") boolean isPublic,
    @JsonProperty("users") List<UUID> users,
    @JsonProperty("groups") List<Integer> groups
) {
}
