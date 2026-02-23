package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxDocumentSetDto(
    @JsonProperty("id") Integer id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("cc_pair_summaries") List<OnyxConnectorSummaryDto> ccPairSummaries,
    @JsonProperty("federated_connector_summaries") List<OnyxConnectorSummaryDto> federatedConnectorSummaries,
    @JsonProperty("is_public") Boolean isPublic,
    @JsonProperty("users") List<UUID> users,
    @JsonProperty("groups") List<Integer> groups
) {
  public List<OnyxConnectorSummaryDto> ccPairSummaries() {
    return ccPairSummaries != null ? ccPairSummaries : Collections.emptyList();
  }

  public List<OnyxConnectorSummaryDto> federatedConnectorSummaries() {
    return federatedConnectorSummaries != null ? federatedConnectorSummaries : Collections.emptyList();
  }

  public List<UUID> users() {
    return users != null ? users : Collections.emptyList();
  }

  public List<Integer> groups() {
    return groups != null ? groups : Collections.emptyList();
  }

  /**
   * Returns is_public, defaulting to true if null.
   * Must match record component return type (Boolean).
   */
  public Boolean isPublic() {
    return isPublic != null ? isPublic : Boolean.TRUE;
  }
}
