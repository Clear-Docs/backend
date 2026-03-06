package ru.cleardocs.backend.client.tochka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TochkaClient {

    private final RestTemplate restTemplate;

    @Value("${tochka.customer-code:}")
    private String customerCode;

    @Value("${tochka.merchant-id:}")
    private String merchantId;

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
            log.error("Ошибка при создании формы оплаты ТочкаБнак: {}", e.getMessage());
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
            List<String> paymentMode,
            String merchantId) {

        return createAcquiringPayment(apiKey, TochkaClientAcquiringPaymentRequest.builder()
                .data(TochkaClientAcquiringPaymentRequest.PaymentData.builder()
                        .customerCode(customerCode)
                        .amount(amount.toString())
                        .purpose(purpose)
                        .paymentMode(paymentMode)
                        .merchantId(merchantId)
                        .build())
                .build());
    }
}
