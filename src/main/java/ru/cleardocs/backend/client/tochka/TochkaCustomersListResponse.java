package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ метода Get Customers List (GET /uapi/open-banking/v1.0/customers).
 * Используется для получения customerCode — нужен для создания платёжных ссылок.
 * customerCode берётся из записи с customerType: "Business".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaCustomersListResponse {

    @JsonProperty("Data")
    private List<Customer> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        /**
         * Уникальный код клиента (9 символов).
         * Используется как tochka.customer-code при создании платёжных ссылок.
         */
        private String customerCode;

        /**
         * Тип клиента. Для платёжных ссылок нужен "Business".
         */
        private String customerType;

        private String name;
        private String inn;
    }
}
