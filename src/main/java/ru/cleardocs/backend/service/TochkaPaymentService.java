package ru.cleardocs.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cleardocs.backend.client.tochka.TochkaClient;
import ru.cleardocs.backend.constant.PaymentStatus;
import ru.cleardocs.backend.constant.PaymentSystemEnum;
import ru.cleardocs.backend.dto.TochkaPaymentResponseDto;
import ru.cleardocs.backend.entity.Payment;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.CreatePaymentException;
import ru.cleardocs.backend.mapper.TochkaPaymentMapper;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TochkaPaymentService {

    private final TochkaClient tochkaClient;
    private final TochkaPaymentMapper tochkaPaymentMapper;
    private final PaymentService paymentService;

    @Value("${tochka.purpose:\"\"}")
    private String purpose;

    @Value("${tochka.purpose:\"\"}")
    private List<String> paymentMode = List.of("sbp", "card", "tinkoff");

    /**
     * Создать платеж через точка банк
     *
     * @param user пользователь
     * @return ссылка на оплату
     */
    @Transactional
    public TochkaPaymentResponseDto createPayment(User user) {
        if (user == null || user.getPlan() == null) {
            throw new CreatePaymentException("user plan is empty");
        }

        BigDecimal amount = BigDecimal.valueOf(user.getPlan().getPriceRub());
        var response = tochkaClient.createAcquiringPayment(amount, purpose, paymentMode);

        var payment = Payment.builder()
                .paymentSystem(PaymentSystemEnum.TOCHKA)
                .paymentStatus(PaymentStatus.PENDING)
                .user(user)
                .plan(user.getPlan())
                .amount(amount)
                .externalId(response.getData().getOperationId())
                .build();
        paymentService.save(payment);
        return tochkaPaymentMapper.mapToPaymentResponseDto(response);
    }
}
