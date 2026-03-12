package ru.cleardocs.backend.client.tochka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TochkaClient {

    private final RestTemplate restTemplate;

    /**
     * Метод для создания ссылки на оплату
     *
     * @param requestDto параметры ссылки на оплату
     * @return ссылка на оплату и статус
     */
    public TochkaClientAcquiringPaymentResponse createAcquiringPayment(String apiKey, TochkaClientAcquiringPaymentRequest requestDto) {
        String url = "https://enter.tochka.com/uapi/acquiring/v1.0/payments";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<TochkaClientAcquiringPaymentRequest> entity = new HttpEntity<>(requestDto, headers);
        log.debug("Запрос к АПИ ТочкаБанк для создания формы оплаты: {}, requestDto: {}", url, requestDto);

        try {
            return restTemplate.postForObject(url, entity, TochkaClientAcquiringPaymentResponse.class);
        } catch (Exception e) {
            logTochkaError("создании формы оплаты", url, e);
            throw e;
        }
    }

    /**
     * Метод для создания ссылки на оплату
     */
    public TochkaClientAcquiringPaymentResponse createAcquiringPayment(
            String apiKey,
            String customerCode,
            BigDecimal amount,
            String purpose,
            List<String> paymentMode) {

        return createAcquiringPayment(apiKey, TochkaClientAcquiringPaymentRequest.builder()
                .data(TochkaClientAcquiringPaymentRequest.PaymentData.builder()
                        .customerCode(customerCode)
                        .amount(amount.toString())
                        .purpose(purpose)
                        .paymentMode(paymentMode)
                        .build())
                .build());
    }

    /**
     * Создать подписку (Create Subscription).
     * POST /acquiring/v1.0/subscriptions
     *
     * @param apiKey JWT-токен
     * @param request параметры подписки
     * @return ответ с operationId и paymentLink
     */
    public TochkaCreateSubscriptionResponse createSubscription(String apiKey, TochkaCreateSubscriptionRequest request) {
        String url = "https://enter.tochka.com/uapi/acquiring/v1.0/subscriptions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<TochkaCreateSubscriptionRequest> entity = new HttpEntity<>(request, headers);
        log.debug("Запрос к АПИ ТочкаБанк Create Subscription: {}", url);

        try {
            return restTemplate.postForObject(url, entity, TochkaCreateSubscriptionResponse.class);
        } catch (Exception e) {
            logTochkaError("создании подписки", url, e);
            throw e;
        }
    }

    /**
     * Установить статус подписки (Set Subscription Status).
     * POST /acquiring/v1.0/subscriptions/{operationId}/status
     * Используется для отмены подписки (status=Cancelled).
     *
     * @param apiKey      JWT-токен
     * @param operationId идентификатор подписки
     * @return ответ с результатом операции
     */
    public TochkaSetSubscriptionStatusResponse setSubscriptionStatus(String apiKey, String operationId) {
        String url = "https://enter.tochka.com/uapi/acquiring/v1.0/subscriptions/" + operationId + "/status";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        var request = TochkaSetSubscriptionStatusRequest.cancelled();
        HttpEntity<TochkaSetSubscriptionStatusRequest> entity = new HttpEntity<>(request, headers);
        log.debug("Запрос к АПИ ТочкаБанк Set Subscription Status: {} operationId={}", url, operationId);

        try {
            return restTemplate.postForObject(url, entity, TochkaSetSubscriptionStatusResponse.class);
        } catch (Exception e) {
            logTochkaError("отмене подписки", url, e);
            throw e;
        }
    }

    /**
     * Получить список клиентов (Get Customers List).
     * Используется для получения customerCode — нужен для tochka.customer-code.
     * customerCode берётся из записи с customerType: "Business".
     *
     * @param apiKey JWT-токен из «Интеграции и API» в интернет-банке
     * @return список клиентов с customerCode, customerType и др.
     */
    public TochkaCustomersListResponse getCustomersList(String apiKey) {
        String url = "https://enter.tochka.com/uapi/open-banking/v1.0/customers";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.debug("Запрос к АПИ ТочкаБанк Get Customers List: {}", url);
        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, TochkaCustomersListResponse.class);
            return response.getBody();
        } catch (Exception e) {
            logTochkaError("получении списка клиентов", url, e);
            throw e;
        }
    }

    private void logTochkaError(String operation, String url, Exception e) {
        if (e instanceof RestClientResponseException re) {
            String body = re.getResponseBodyAsString();
            String bodySummary = (body != null && (body.stripLeading().startsWith("<!") || body.stripLeading().startsWith("<html")))
                    ? "[HTML error page, length=" + body.length() + "]"
                    : body;
            log.error("Ошибка при {} ТочкаБанк: status={}, responseBody={}, url={}",
                    operation, re.getStatusCode(), bodySummary, url, e);
        } else {
            log.error("Ошибка при {} ТочкаБанк: {}, url={}", operation, e.getMessage(), url, e);
        }
    }
}
