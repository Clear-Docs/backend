package ru.cleardocs.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cleardocs.backend.client.tochka.TochkaClient;
import ru.cleardocs.backend.client.tochka.TochkaCustomersListResponse;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.constant.PaymentStatus;
import ru.cleardocs.backend.constant.PaymentSystemEnum;
import ru.cleardocs.backend.dto.TochkaPaymentRequestDto;
import ru.cleardocs.backend.dto.TochkaPaymentResponseDto;
import ru.cleardocs.backend.entity.Payment;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.CreatePaymentException;
import ru.cleardocs.backend.mapper.TochkaPaymentMapper;
import ru.cleardocs.backend.repository.PlanRepository;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TochkaPaymentService {

    private final TochkaClient tochkaClient;
    private final TochkaPaymentMapper tochkaPaymentMapper;
    private final PaymentService paymentService;
    private final PlanRepository planRepository;

    @Value("${tochka.api-key:}")
    private String apiKey;

    @Value("${tochka.purpose:\"\"}")
    private String purpose;

    @Value("${tochka.payment-modes:sbp,card,tinkoff}")
    private List<String> paymentMode;

    /**
     * Создать платеж через точка банк
     *
     * @param user пользователь
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
        var plan = planRepository.findByCode(planCode).orElseThrow(() -> new CreatePaymentException("Plan not found with code: " + planCode));
        BigDecimal amount = BigDecimal.valueOf(plan.getPriceRub());

        var customersResponse = tochkaClient.getCustomersList(apiKey);
        var customerCode = resolveBusinessCustomerCode(customersResponse);
        log.info("Tochka createPayment: using customerCode={}, planCode={}, amount={}", customerCode, planCode, amount);

        var response = tochkaClient.createAcquiringPayment(apiKey, customerCode, amount, purpose, paymentMode);

        var payment = Payment.builder()
                .paymentSystem(PaymentSystemEnum.TOCHKA)
                .paymentStatus(PaymentStatus.PENDING)
                .user(user)
                .plan(plan)
                .amount(amount)
                .externalId(response.getData().getOperationId())
                .build();
        paymentService.save(payment);
        return tochkaPaymentMapper.mapToPaymentResponseDto(response);
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
