package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TochkaClientAcquiringPaymentRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSerialization() throws JsonProcessingException {
        TochkaClientAcquiringPaymentRequest request = TochkaClientAcquiringPaymentRequest.builder()
                .data(TochkaClientAcquiringPaymentRequest.PaymentData.builder()
                        .customerCode("300000092")
                        .amount("1234.00")
                        .purpose("Перевод за оказанные услуги")
                        .redirectUrl("https://example.com")
                        .failRedirectUrl("https://example.com/fail")
                        .paymentMode(List.of("sbp", "card", "tinkoff", "dolyame"))
                        .saveCard(true)
                        .consumerId("fedac807-078d-45ac-a43b-5c01c57edbf8")
                        .merchantId("200000000001056")
                        .preAuthorization(true)
                        .ttl(10080)
                        .paymentLinkId("string")
                        .build())
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"Data\""));
        assertTrue(json.contains("\"customerCode\":\"300000092\""));
        assertTrue(json.contains("\"amount\":\"1234.00\""));
        assertTrue(json.contains("\"paymentMode\":[\"sbp\",\"card\",\"tinkoff\",\"dolyame\"]"));
        assertTrue(json.contains("\"saveCard\":true"));
        assertTrue(json.contains("\"ttl\":10080"));
    }
}
