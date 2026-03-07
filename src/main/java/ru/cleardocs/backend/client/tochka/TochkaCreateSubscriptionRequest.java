package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaCreateSubscriptionRequest {

    @JsonProperty("Data")
    private SubscriptionData data;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubscriptionData {

        private String customerCode;
        private String amount;
        private String purpose;
        private Boolean saveCard;
        private String consumerId;

        @JsonProperty("Options")
        private SubscriptionOptions options;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubscriptionOptions {

        private Integer trancheCount;
        private String period;
    }
}
