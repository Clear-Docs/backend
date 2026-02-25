package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request for Onyx POST /persona (create persona).
 * See Onyx API: PersonaUpsertRequest.
 */
public record OnyxPersonaUpsertRequestDto(
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("document_set_ids") List<Integer> documentSetIds,
    @JsonProperty("num_chunks") int numChunks,
    @JsonProperty("is_public") boolean isPublic,
    @JsonProperty("recency_bias") String recencyBias,
    @JsonProperty("llm_filter_extraction") boolean llmFilterExtraction,
    @JsonProperty("llm_relevance_filter") boolean llmRelevanceFilter,
    @JsonProperty("tool_ids") List<Integer> toolIds,
    @JsonProperty("system_prompt") String systemPrompt,
    @JsonProperty("task_prompt") String taskPrompt,
    @JsonProperty("datetime_aware") boolean datetimeAware
) {
  public static OnyxPersonaUpsertRequestDto forDocumentSet(String name, int docSetId) {
    return new OnyxPersonaUpsertRequestDto(
        name != null ? name : "Chat Assistant",
        "",
        List.of(docSetId),
        25,
        true,
        "base_decay",
        false,
        false,
        List.of(1), // internal_search tool — необходим для поиска по документам
        "",
        "",
        false
    );
  }
}
