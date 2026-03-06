package ru.cleardocs.backend.client.tochka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TochkaClientAcquiringPaymentResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeserialization() throws Exception {
        String json = """
                {
                  "Data": {
                    "purpose": "Футболка женская",
                    "status": "CREATED",
                    "amount": "1234.00",
                    "operationId": "48232c9a-ce82-1593-3cb6-5c85a1ffef8f",
                    "paymentLink": "https://merch.example.com/order/?uuid=16ea4c54-bf1d-4e6a-a1ef-53ad55666e43",
                    "consumerId": "fedac807-078d-45ac-a43b-5c01c57edbf8",
                    "merchantId": "200000000001056",
                    "preAuthorization": true,
                    "ttl": 10080,
                    "paymentLinkId": "string",
                    "paymentMode": [
                      "sbp",
                      "card",
                      "tinkoff",
                      "dolyame"
                    ]
                  },
                  "Links": {
                    "self": "https://enter.tochka.com/uapi"
                  },
                  "Meta": {
                    "totalPages": 1
                  }
                }
                """;

        TochkaClientAcquiringPaymentResponse response = objectMapper.readValue(json, TochkaClientAcquiringPaymentResponse.class);

        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("Футболка женская", response.getData().getPurpose());
        assertEquals("CREATED", response.getData().getStatus());
        assertEquals("1234.00", response.getData().getAmount());
        assertEquals("48232c9a-ce82-1593-3cb6-5c85a1ffef8f", response.getData().getOperationId());
        assertEquals("https://merch.example.com/order/?uuid=16ea4c54-bf1d-4e6a-a1ef-53ad55666e43", response.getData().getPaymentLink());
        assertEquals("fedac807-078d-45ac-a43b-5c01c57edbf8", response.getData().getConsumerId());
        assertEquals("200000000001056", response.getData().getMerchantId());
        assertTrue(response.getData().getPreAuthorization());
        assertEquals(10080, response.getData().getTtl());
        assertEquals("string", response.getData().getPaymentLinkId());
        assertNotNull(response.getData().getPaymentMode());
        assertEquals(4, response.getData().getPaymentMode().size());
        assertTrue(response.getData().getPaymentMode().contains("sbp"));

        assertNotNull(response.getLinks());
        assertEquals("https://enter.tochka.com/uapi", response.getLinks().getSelf());

        assertNotNull(response.getMeta());
        assertEquals(1, response.getMeta().getTotalPages());
    }
}
