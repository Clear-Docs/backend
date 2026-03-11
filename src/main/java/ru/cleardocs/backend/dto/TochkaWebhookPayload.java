package ru.cleardocs.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Claims из JWT вебхука Точка Банк (acquiringInternetPayment).
 * См. https://developers.tochka.com/docs/tochka-api/opisanie-metodov/vebhuki
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaWebhookPayload {

    @JsonProperty("webhookType")
    private String webhookType;

    @JsonProperty("operationId")
    private String operationId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("consumerId")
    private String consumerId;

    @JsonProperty("customerCode")
    private String customerCode;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("purpose")
    private String purpose;
}
