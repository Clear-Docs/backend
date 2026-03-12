package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Тело запроса Create Webhook (PUT webhook/v1.0/{client-id}).
 * API Точки требует поле webhooks_list — список вебхуков.
 * Документация: https://developers.tochka.com/docs/tochka-api/api/create-webhook-webhook-v-1-0-client-id-put
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaCreateWebhookRequest {

    @JsonProperty("webhooks_list")
    private List<WebhookItem> webhooksList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookItem {
        private String url;
        private String webhookType;
    }
}
