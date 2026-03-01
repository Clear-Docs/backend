package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OnyxConnectorCreateRequestDto(
    @JsonProperty("name") String name,
    @JsonProperty("source") String source,
    @JsonProperty("input_type") String inputType,
    @JsonProperty("connector_specific_config") Map<String, Object> connectorSpecificConfig,
    @JsonProperty("access_type") String accessType,
    @JsonProperty("groups") List<Integer> groups,
    @JsonProperty("refresh_freq") Integer refreshFreq,
    @JsonProperty("prune_freq") Integer pruneFreq,
    @JsonProperty("indexing_start") String indexingStart
) {
  public static OnyxConnectorCreateRequestDto forFileConnector(
      String name,
      List<String> fileLocations,
      List<String> fileNames
  ) {
    return new OnyxConnectorCreateRequestDto(
        name,
        "file",
        "load_state",
        Map.of(
            "file_locations", fileLocations,
            "file_names", fileNames != null ? fileNames : fileLocations
        ),
        "public",
        List.of(),
        null,
        null,
        null
    );
  }

  public static OnyxConnectorCreateRequestDto forWebConnector(String name, String url) {
    return new OnyxConnectorCreateRequestDto(
        name,
        "web",
        "load_state",
        Map.of(
            "base_url", url,
            "web_connector_type", "recursive",
            "scroll_before_scraping", false
        ),
        "public",
        List.of(),
        86400,   // refresh_freq (24 hours)
        432000,  // prune_freq (5 days)
        null     // indexing_start
    );
  }
}
