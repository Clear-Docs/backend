package ru.cleardocs.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.cleardocs.backend.client.tochka.TochkaClient;
import ru.cleardocs.backend.client.tochka.TochkaCreateSubscriptionResponse;
import ru.cleardocs.backend.client.tochka.TochkaCustomersListResponse;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.dto.TochkaPaymentRequestDto;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.mapper.TochkaPaymentMapper;
import ru.cleardocs.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TochkaPaymentServiceTest {

    @Mock
    TochkaClient tochkaClient;

    @Mock
    PaymentService paymentService;

    @Mock
    ru.cleardocs.backend.repository.PlanRepository planRepository;

    @Mock
    UserRepository userRepository;

    @Spy
    TochkaPaymentMapper tochkaPaymentMapper;

    @InjectMocks
    TochkaPaymentService tochkaPaymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tochkaPaymentService, "apiKey", "test-key");
        ReflectionTestUtils.setField(tochkaPaymentService, "purpose", "Test");
    }

    @Test
    void createPayment() {
        var paymentLink = "https://merch.example.com/order/?uuid=16ea4c54-bf1d-4e6a-a1ef-53ad55666e43";
        var operationId = "48232c9a-ce82-1593-3cb6-5c85a1ffef8f";

        var plan = Plan.builder().code(PlanCode.MONTH).priceRub(100).build();
        User user = User.builder()
                .id(java.util.UUID.randomUUID())
                .plan(plan)
                .build();

        var subscriptionData = new TochkaCreateSubscriptionResponse.SubscriptionData();
        subscriptionData.setOperationId(operationId);
        subscriptionData.setPaymentLink(paymentLink);
        var mockResponse = TochkaCreateSubscriptionResponse.builder()
                .data(subscriptionData)
                .build();

        var customer = new TochkaCustomersListResponse.Customer("300000092", "Business");
        var customerListData = new TochkaCustomersListResponse.CustomerListData(java.util.List.of(customer));
        var customersResponse = new TochkaCustomersListResponse(customerListData);

        when(planRepository.findByCode(PlanCode.MONTH)).thenReturn(java.util.Optional.of(plan));
        when(tochkaClient.getCustomersList(any())).thenReturn(customersResponse);
        when(tochkaClient.createSubscription(any(), any())).thenReturn(mockResponse);

        var response = tochkaPaymentService.createPayment(new TochkaPaymentRequestDto("MONTH"), user);
        assertEquals(paymentLink, response.paymentUrl());
        verify(userRepository).save(user);
    }
}
