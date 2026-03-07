package ru.cleardocs.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cleardocs.backend.client.tochka.TochkaClient;
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

    @Value("${tochka.customer-code:}")
    private String customerCode;

    @Value("${tochka.merchant-id:}")
    private String merchantId;

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
        if (request.planId() == null) {
            throw new CreatePaymentException("planId is required");
        }
        var plan = planRepository.findById(request.planId()).orElseThrow(() -> new CreatePaymentException("Plan not found with id: " + request.planId()));
        BigDecimal amount = BigDecimal.valueOf(plan.getPriceRub());
        var response = tochkaClient.createAcquiringPayment(apiKey, customerCode, amount, purpose, paymentMode, merchantId);

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
}
