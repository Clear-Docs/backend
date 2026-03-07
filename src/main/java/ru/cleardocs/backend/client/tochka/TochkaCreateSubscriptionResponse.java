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
public class TochkaCreateSubscriptionResponse {

    @JsonProperty("Data")
    private SubscriptionData data;

    @JsonProperty("Links")
    private Object links;

    @JsonProperty("Meta")
    private Object meta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriptionData {

        private String purpose;
        private String amount;
        private String status;
        private String operationId;
        private String paymentLink;
        private String consumerId;
        private Boolean recurring;
    }
}
