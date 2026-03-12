package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Тело запроса Create Webhook (PUT webhook/v1.0/{client-id}).
 * Документация: https://developers.tochka.com/docs/tochka-api/api/create-webhook-webhook-v-1-0-client-id-put
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaCreateWebhookRequest {

    private String url;
    private String webhookType;
}
