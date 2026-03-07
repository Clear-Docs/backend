package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ метода Get Customers List (GET /uapi/open-banking/v1.0/customers).
 * Структура по OpenAPI: Data.Customer[] — см. CustomerListResponseModel.
 * customerCode берётся из записи с customerType: "Business".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TochkaCustomersListResponse {

    @JsonProperty("Data")
    private CustomerListData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerListData {
        @JsonProperty("Customer")
        private List<Customer> customer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        /** Уникальный код клиента (9 символов). Используется для платёжных ссылок. */
        private String customerCode;
        /** Тип клиента. Для платёжных ссылок нужен "Business". */
        private String customerType;
    }
}
