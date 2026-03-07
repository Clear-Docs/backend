package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaClientAcquiringPaymentRequest {

    @JsonProperty("Data")
    private PaymentData data;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentData {

        /**
         * Уникальный код клиента
         * <br>
         * Possible values: 9 characters
         * <br>
         * Example: 300000092
         * <br>
         * Обязательное поле
         */
        private String customerCode;

        /**
         * Сумма платежа
         * <br>
         * Possible values: > 0
         * <br>
         * Example: 1234.00
         * <br>
         * Обязательное поле
         */
        private String amount;

        /**
         * Назначение платежа
         * <br>
         * Possible values: non-empty and <= 140 characters
         * <br>
         * Example: Перевод за оказанные услуги
         * <br>
         * Обязательное поле
         */
        private String purpose;

        /**
         * URL адрес, куда будет переправлен клиент после оплаты услуги
         * <br>
         * Possible values: non-empty and <= 2083 characters
         * <br>
         * Example: https://example.com
         */
        private String redirectUrl;

        /**
         * URL адрес, куда будет переправлен клиент в случае неуспешной оплаты
         * <br>
         * Possible values: non-empty and <= 2083 characters
         * <br>
         * Example: https://example.com/fail
         */
        private String failRedirectUrl;

        /**
         * Способ оплаты
         * <br>
         * Possible values: [sbp, card, tinkoff, dolyame], >= 1
         * <br>
         * Example: ["sbp","card","tinkoff","dolyame"]
         * <br>
         * Обязательное поле
         */
        private List<String> paymentMode;

        /**
         * Предложить покупателю сохранить карту
         * <br>
         * Example: true
         */
        private Boolean saveCard;

        /**
         * Идентификатор покупателя
         * <br>
         * Example: fedac807-078d-45ac-a43b-5c01c57edbf8
         */
        private String consumerId;

        /**
         * Идентификатор торговой точки в интернет-эквайринге
         * <br>
         * Possible values: 15 characters
         * <br>
         * Example: 200000000001056
         */
        private String merchantId;

        /**
         * Создать платёж с двухэтапной оплатой
         */
        private Boolean preAuthorization;

        /**
         * Время жизни платёжной ссылки в минутах
         * <br>
         * Possible values: >= 1 and <= 44640
         * <br>
         * Default value: 10080
         */
        private Integer ttl;

        /**
         * Уникальный номер заказа
         * <br>
         * Possible values: non-empty and <= 45 characters
         */
        private String paymentLinkId;

    }
}
