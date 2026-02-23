package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxDocumentSetDto(
    @JsonProperty("id") Integer id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("cc_pair_summaries") List<OnyxConnectorSummaryDto> ccPairSummaries,
    @JsonProperty("federated_connector_summaries") List<OnyxConnectorSummaryDto> federatedConnectorSummaries
) {
  public List<OnyxConnectorSummaryDto> ccPairSummaries() {
    return ccPairSummaries != null ? ccPairSummaries : Collections.emptyList();
  }

  public List<OnyxConnectorSummaryDto> federatedConnectorSummaries() {
    return federatedConnectorSummaries != null ? federatedConnectorSummaries : Collections.emptyList();
  }
}
