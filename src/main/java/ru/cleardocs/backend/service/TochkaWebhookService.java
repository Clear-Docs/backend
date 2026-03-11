package ru.cleardocs.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cleardocs.backend.constant.PaymentStatus;
import ru.cleardocs.backend.dto.TochkaWebhookPayload;
import ru.cleardocs.backend.entity.Payment;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.repository.PaymentRepository;
import ru.cleardocs.backend.repository.UserRepository;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

@Slf4j
@Service
@RequiredArgsConstructor
public class TochkaWebhookService {

    private static final String WEBHOOK_TYPE_ACQUIRING = "acquiringInternetPayment";
    private static final String STATUS_APPROVED = "APPROVED";

    /**
     * Публичный ключ Точка Банка (JWK) для верификации JWT вебхука (RS256).
     * Источник: https://developers.tochka.com/docs/tochka-api/opisanie-metodov/vebhuki,
     * https://enter.tochka.com/doc/openapi/static/keys/public
     * Переопределение: tochka.webhook-public-key
     */
    private static final String DEFAULT_TOCHKA_PUBLIC_KEY_JSON = """
            {"kty":"RSA","e":"AQAB","n":"rwm77av7GIttq-JF1itEgLCGEZW_zz16RlUQVYlLbJtyRSu61fCec_rroP6PxjXU2uLzUOaGaLgAPeUZAJrGuVp9nryKgbZceHckdHDYgJd9TsdJ1MYUsXaOb9joN9vmsCscBx1lwSlFQyNQsHUsrjuDk-opf6RCuazRQ9gkoDCX70HV8WBMFoVm-YWQKJHZEaIQxg_DU4gMFyKRkDGKsYKA0POL-UgWA1qkg6nHY5BOMKaqxbc5ky87muWB5nNk4mfmsckyFv9j1gBiXLKekA_y4UwG2o1pbOLpJS3bP_c95rm4M9ZBmGXqfOQhbjz8z-s9C11i-jmOQ2ByohS-ST3E5sqBzIsxxrxyQDTw--bZNhzpbciyYW4GfkkqyeYoOPd_84jPTBDKQXssvj8ZOj2XboS77tvEO1n1WlwUzh8HPCJod5_fEgSXuozpJtOggXBv0C2ps7yXlDZf-7Jar0UYc_NJEHJF-xShlqd6Q3sVL02PhSCM-ibn9DN9BKmD"}
            """;

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${tochka.webhook-public-key:}")
    private String webhookPublicKeyJson;

    /**
     * Обрабатывает сырое тело вебхука (JWT-строка).
     * Верифицирует подпись, парсит payload; при webhookType=acquiringInternetPayment и status=APPROVED
     * обновляет платёж и тариф пользователя.
     *
     * @param rawBody тело POST-запроса (JWT)
     * @return true если вебхук принят и обработан (всегда возвращать 200)
     */
    @Transactional
    public boolean handleWebhook(String rawBody) {
        int bodyLen = rawBody != null ? rawBody.length() : 0;
        log.info("Tochka webhook: request received, bodyLength={}", bodyLen);

        if (rawBody == null || rawBody.isBlank()) {
            log.warn("Tochka webhook: empty body, ignoring");
            return true;
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(rawBody.trim());
            RSAPublicKey publicKey = getPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                log.warn("Tochka webhook: invalid signature (JWT parsed but verification failed), check tochka.webhook-public-key");
                return true;
            }
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
            TochkaWebhookPayload payload = objectMapper.convertValue(claims, TochkaWebhookPayload.class);

            log.info("Tochka webhook: parsed webhookType={}, status={}, operationId={}, amount={}",
                    payload.getWebhookType(), payload.getStatus(), payload.getOperationId(), payload.getAmount());

            if (!WEBHOOK_TYPE_ACQUIRING.equals(payload.getWebhookType())) {
                log.info("Tochka webhook: ignoring (expected type={}, got {})", WEBHOOK_TYPE_ACQUIRING, payload.getWebhookType());
                return true;
            }
            if (!STATUS_APPROVED.equals(payload.getStatus())) {
                log.info("Tochka webhook: ignoring (expected status={}, got {}), operationId={}", STATUS_APPROVED, payload.getStatus(), payload.getOperationId());
                return true;
            }

            String operationId = payload.getOperationId();
            if (operationId == null || operationId.isBlank()) {
                log.warn("Tochka webhook: missing operationId in APPROVED payload, ignoring");
                return true;
            }

            paymentRepository.findByExternalId(operationId).ifPresentOrElse(
                    this::applyPaymentSuccess,
                    () -> log.warn("Tochka webhook: payment not found for operationId={} (payment.externalId must equal subscription operationId)", operationId)
            );
            return true;
        } catch (Exception e) {
            log.error("Tochka webhook: failed to process body, bodyLength={}, error={}", bodyLen, e.getMessage(), e);
            return true; // всё равно 200, чтобы Точка не слала повторно
        }
    }

    private void applyPaymentSuccess(Payment payment) {
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("Tochka webhook: payment already SUCCESS, operationId={}, skipping", payment.getExternalId());
            return;
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        User user = payment.getUser();
        if (user != null && payment.getPlan() != null) {
            user.setPlan(payment.getPlan());
            userRepository.save(user);
            log.info("Tochka webhook: SUCCESS — user {} plan updated to {}, paymentId={}, operationId={}",
                    user.getId(), payment.getPlan().getCode(), payment.getId(), payment.getExternalId());
        } else {
            log.warn("Tochka webhook: payment {} applied but user or plan is null (userId={}, planId={})",
                    payment.getId(), user != null ? user.getId() : null, payment.getPlan() != null ? payment.getPlan().getId() : null);
        }
    }

    private RSAPublicKey getPublicKey() {
        String json = (webhookPublicKeyJson != null && !webhookPublicKeyJson.isBlank())
                ? webhookPublicKeyJson.trim()
                : DEFAULT_TOCHKA_PUBLIC_KEY_JSON.trim();
        try {
            JWK jwk = JWK.parse(json);
            if (!(jwk instanceof RSAKey rsaKey)) {
                throw new IllegalStateException("Tochka webhook key must be RSA");
            }
            return rsaKey.toRSAPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid tochka.webhook-public-key", e);
        }
    }
}
