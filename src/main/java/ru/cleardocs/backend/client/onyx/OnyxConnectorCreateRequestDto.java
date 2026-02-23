package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record OnyxConnectorCreateRequestDto(
    @JsonProperty("name") String name,
    @JsonProperty("source") String source,
    @JsonProperty("input_type") String inputType,
    @JsonProperty("connector_specific_config") Map<String, Object> connectorSpecificConfig,
    @JsonProperty("access_type") String accessType,
    @JsonProperty("groups") List<Integer> groups
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
        List.of()
    );
  }
}
