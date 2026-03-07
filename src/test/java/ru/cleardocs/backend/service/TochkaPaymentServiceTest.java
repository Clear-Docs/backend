package ru.cleardocs.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cleardocs.backend.client.tochka.TochkaClient;
import ru.cleardocs.backend.client.tochka.TochkaClientAcquiringPaymentResponse;
import ru.cleardocs.backend.client.tochka.TochkaCustomersListResponse;
import ru.cleardocs.backend.dto.TochkaPaymentRequestDto;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.mapper.TochkaPaymentMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TochkaPaymentServiceTest {

    @Mock
    TochkaClient tochkaClient;

    @Mock
    PaymentService paymentService;

    @Mock
    ru.cleardocs.backend.repository.PlanRepository planRepository;

    @Spy
    TochkaPaymentMapper tochkaPaymentMapper;

    @InjectMocks
    TochkaPaymentService tochkaPaymentService;

    @Test
    void createPayment() {
        var paymentLink = "https://merch.example.com/order/?uuid=16ea4c54-bf1d-4e6a-a1ef-53ad55666e43";
        var operationId = "48232c9a-ce82-1593-3cb6-5c85a1ffef8f";

        UUID userId = UUID.fromString("fedac807-078d-45ac-a43b-5c01c57edbf8");
        User user = User.builder()
                .id(userId)
                .plan(Plan.builder().priceRub(100).build())
                .build();

        TochkaClientAcquiringPaymentResponse mockResponse = new TochkaClientAcquiringPaymentResponse();
        TochkaClientAcquiringPaymentResponse.PaymentData data = new TochkaClientAcquiringPaymentResponse.PaymentData();
        data.setOperationId(operationId);
        data.setPaymentLink(paymentLink);
        mockResponse.setData(data);

        var customer = new TochkaCustomersListResponse.Customer("300000092", "Business", "Test", null);
        var customersResponse = new TochkaCustomersListResponse(java.util.List.of(customer));

        when(planRepository.findById(any())).thenReturn(java.util.Optional.of(user.getPlan()));
        when(tochkaClient.getCustomersList(any())).thenReturn(customersResponse);
        when(tochkaClient.createAcquiringPayment(any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        var response = tochkaPaymentService.createPayment(new TochkaPaymentRequestDto(UUID.randomUUID()), user);
        assertEquals(paymentLink, response.paymentUrl());
    }
}
