package ru.cleardocs.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cleardocs.backend.client.tochka.TochkaCreateSubscriptionRequest;
import ru.cleardocs.backend.client.tochka.TochkaCreateSubscriptionResponse;
import ru.cleardocs.backend.client.tochka.TochkaClient;
import ru.cleardocs.backend.client.tochka.TochkaCustomersListResponse;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.constant.PaymentStatus;
import ru.cleardocs.backend.constant.PaymentSystemEnum;
import ru.cleardocs.backend.dto.TochkaPaymentRequestDto;
import ru.cleardocs.backend.dto.TochkaPaymentResponseDto;
import ru.cleardocs.backend.entity.Payment;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.CreatePaymentException;
import ru.cleardocs.backend.mapper.TochkaPaymentMapper;
import ru.cleardocs.backend.repository.PlanRepository;
import ru.cleardocs.backend.repository.UserRepository;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TochkaPaymentService {

    private final TochkaClient tochkaClient;
    private final TochkaPaymentMapper tochkaPaymentMapper;
    private final PaymentService paymentService;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @Value("${tochka.api-key:}")
    private String apiKey;

    @Value("${tochka.purpose:\"\"}")
    private String purpose;

    @Value("${tochka.subscription.tranche-count:1}")
    private int subscriptionTrancheCount;

    /**
     * Создать подписку через Точка Банк (Create Subscription).
     * Возвращает ссылку на первую оплату — после оплаты подписка активируется, списания ежемесячно.
     *
     * @param request planCode (например MONTH)
     * @param user    пользователь
     * @return ссылка на оплату
     */
    @Transactional
    public TochkaPaymentResponseDto createPayment(TochkaPaymentRequestDto request, User user) {
        if (request.planCode() == null || request.planCode().isBlank()) {
            throw new CreatePaymentException("planCode is required");
        }
        PlanCode planCode;
        try {
            planCode = PlanCode.valueOf(request.planCode().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CreatePaymentException("Unknown planCode: " + request.planCode() + ". Use code from GET /api/v1/plans (e.g. MONTH)");
        }
        Plan plan = planRepository.findByCode(planCode).orElseThrow(() -> new CreatePaymentException("Plan not found with code: " + planCode));
        BigDecimal amount = BigDecimal.valueOf(plan.getPriceRub());

        var customersResponse = tochkaClient.getCustomersList(apiKey);
        var customerCode = resolveBusinessCustomerCode(customersResponse);
        log.info("Tochka createSubscription: using customerCode={}, planCode={}, amount={}", customerCode, planCode, amount);

        var subscriptionRequest = TochkaCreateSubscriptionRequest.builder()
                .data(TochkaCreateSubscriptionRequest.SubscriptionData.builder()
                        .customerCode(customerCode)
                        .amount(amount.toString())
                        .purpose(purpose)
                        .consumerId(user.getId().toString())
                        .options(TochkaCreateSubscriptionRequest.SubscriptionOptions.builder()
                                .period("Month")
                                .trancheCount(subscriptionTrancheCount)
                                .build())
                        .build())
                .build();

        TochkaCreateSubscriptionResponse response = tochkaClient.createSubscription(apiKey, subscriptionRequest);

        var operationId = response.getData().getOperationId();
        user.setTochkaSubscriptionOperationId(operationId);
        userRepository.save(user);

        var payment = Payment.builder()
                .paymentSystem(PaymentSystemEnum.TOCHKA)
                .paymentStatus(PaymentStatus.PENDING)
                .user(user)
                .plan(plan)
                .amount(amount)
                .externalId(operationId)
                .build();
        paymentService.save(payment);

        return tochkaPaymentMapper.mapToPaymentResponseDto(response);
    }

    /**
     * Отписаться от подписки (Set Subscription Status = Cancelled).
     * Обновляет план пользователя на FREE и очищает tochkaSubscriptionOperationId.
     *
     * @param user пользователь
     */
    @Transactional
    public void unsubscribe(User user) {
        String operationId = user.getTochkaSubscriptionOperationId();
        if (operationId == null || operationId.isBlank()) {
            throw new CreatePaymentException("No active subscription to cancel");
        }

        tochkaClient.setSubscriptionStatus(apiKey, operationId);

        Plan freePlan = planRepository.findByCode(PlanCode.FREE)
                .orElseThrow(() -> new CreatePaymentException("FREE plan not found"));
        user.setPlan(freePlan);
        user.setTochkaSubscriptionOperationId(null);
        userRepository.save(user);

        log.info("Tochka unsubscribe: user={} subscription {} cancelled", user.getId(), operationId);
    }

    /**
     * Извлечь customerCode из ответа Get Customers List.
     * Берётся из первой записи с customerType: "Business".
     */
    private String resolveBusinessCustomerCode(TochkaCustomersListResponse customersResponse) {
        var data = customersResponse != null ? customersResponse.getData() : null;
        var customers = data != null ? data.getCustomer() : null;
        if (customers == null || customers.isEmpty()) {
            log.warn("Tochka Get Customers List: empty or null response - check API key and ReadCustomerData permission");
            throw new CreatePaymentException("Tochka API returned empty customers list");
        }
        log.debug("Tochka Get Customers List: {} client(s) - {}", customers.size(),
                customers.stream()
                        .map(c -> String.format("%s(customerType=%s)", c.getCustomerCode(), c.getCustomerType()))
                        .toList());

        var businessCode = customers.stream()
                .filter(c -> "Business".equalsIgnoreCase(c.getCustomerType()))
                .map(TochkaCustomersListResponse.Customer::getCustomerCode)
                .findFirst();

        if (businessCode.isEmpty()) {
            var types = customers.stream().map(TochkaCustomersListResponse.Customer::getCustomerType).distinct().toList();
            log.warn("Tochka Get Customers List: no Business customer found. customerTypes in response: {}", types);
            throw new CreatePaymentException("No Business customer found in Tochka. Check API permissions (ReadCustomerData)");
        }
        var code = businessCode.get();
        log.info("Tochka Get Customers List: {} client(s), selected customerCode={} (Business)", customers.size(), code);
        return code;
    }

    /**
     * Получить список клиентов Точка Банк (Get Customers List).
     * Используется для отладки и проверки customerCode.
     */
    public TochkaCustomersListResponse getCustomersList() {
        return tochkaClient.getCustomersList(apiKey);
    }
}
