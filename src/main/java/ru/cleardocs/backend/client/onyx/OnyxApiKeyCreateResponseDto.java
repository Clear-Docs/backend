package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnyxApiKeyCreateResponseDto(
    @JsonProperty("api_key") String apiKey,
    @JsonProperty("key") String key
) {
  /** Returns the API key string (Onyx may use either api_key or key field). */
  public String getKeyValue() {
    if (apiKey != null && !apiKey.isBlank()) {
      return apiKey;
    }
    return key;
  }
}
