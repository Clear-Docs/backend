package ru.cleardocs.backend.client.onyx;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OnyxCcStatusUpdateRequestDto(
    @JsonProperty("status") String status
) {
  public static OnyxCcStatusUpdateRequestDto paused() {
    return new OnyxCcStatusUpdateRequestDto("PAUSED");
  }

  public static OnyxCcStatusUpdateRequestDto active() {
    return new OnyxCcStatusUpdateRequestDto("ACTIVE");
  }
}
