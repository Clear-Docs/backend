package ru.cleardocs.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.cleardocs.backend.service.TochkaPaymentService;

/**
 * После старта приложения регистрирует вебхук Точка Банк (Create Webhook),
 * чтобы оплаты приходили на /api/v1/pay/webhook/tochka.
 * Только для профиля prod. При ошибке логируем и не падаем — ручная регистрация через API остаётся возможной.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class TochkaWebhookStartupRegistration {

    private final TochkaPaymentService tochkaPaymentService;

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("Tochka webhook: auto-registering on startup");
            tochkaPaymentService.registerTochkaWebhook();
            log.info("Tochka webhook: auto-registration done");
        } catch (Exception e) {
            log.warn("Tochka webhook: auto-registration failed (webhook may be unregistered): {} — call POST /api/v1/pay/tochka/register-webhook with Firebase auth to register manually", e.getMessage());
        }
    }
}
