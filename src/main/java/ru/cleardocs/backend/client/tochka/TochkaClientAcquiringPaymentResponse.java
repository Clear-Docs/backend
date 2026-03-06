package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaClientAcquiringPaymentResponse {

    @JsonProperty("Data")
    private PaymentData data;

    @JsonProperty("Links")
    private Links links;

    @JsonProperty("Meta")
    private Meta meta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {

        /**
         * Назначение платежа
         * Example: Футболка женская молочная
         */
        private String purpose;

        /**
         * Статус операции
         * Possible values: [CREATED, APPROVED, ON-REFUND, REFUNDED, EXPIRED, REFUNDED_PARTIALLY, AUTHORIZED, WAIT_FULL_PAYMENT]
         * Example: CREATED
         */
        private String status;

        /**
         * Сумма платежа
         * Example: 1234.00
         */
        private String amount;

        /**
         * Идентификатор платежа
         * Example: 48232c9a-ce82-1593-3cb6-5c85a1ffef8f
         */
        private String operationId;

        /**
         * Ссылка на оплату
         * Example: https://merch.example.com/order/?uuid=16ea4c54-bf1d-4e6a-a1ef-53ad55666e43
         */
        private String paymentLink;

        /**
         * Идентификатор покупателя
         * Example: fedac807-078d-45ac-a43b-5c01c57edbf8
         */
        private String consumerId;

        /**
         * Идентификатор торговой точки в интернет-эквайринге
         * Example: 200000000001056
         */
        private String merchantId;

        /**
         * Создать платёж с двухэтапной оплатой
         */
        private Boolean preAuthorization;

        /**
         * Время жизни платёжной ссылки в минутах
         */
        private Integer ttl;

        /**
         * Уникальный номер заказа
         */
        private String paymentLinkId;

        /**
         * Способ оплаты
         */
        private List<String> paymentMode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private String self;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private Integer totalPages;
    }
}
