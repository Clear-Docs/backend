package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaSetSubscriptionStatusResponse {

    @JsonProperty("Data")
    private Boolean data;

    @JsonProperty("Links")
    private Object links;

    @JsonProperty("Meta")
    private Object meta;
}
