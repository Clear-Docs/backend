package ru.cleardocs.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.client.tochka.TochkaCustomersListResponse;
import ru.cleardocs.backend.dto.TochkaPaymentRequestDto;
import ru.cleardocs.backend.dto.TochkaPaymentResponseDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.service.TochkaPaymentService;
import ru.cleardocs.backend.service.TochkaWebhookService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pay")
public class PaymentController {

    private final TochkaPaymentService tochkaPaymentService;
    private final TochkaWebhookService tochkaWebhookService;

    @Operation(summary = "Инициализация платежа через Точка банк")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная Инициализация платежа через Точка банк",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = TochkaPaymentResponseDto.class))}),
            @ApiResponse(responseCode = "500", description = "Ошибка инициализации платежа")
    })
    @PostMapping("/tochka/createPayment")
    public ResponseEntity<TochkaPaymentResponseDto> createTochkaPayment(@RequestBody TochkaPaymentRequestDto request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tochkaPaymentService.createPayment(request, user));
    }

    @Operation(summary = "Отписаться от подписки Точка Банк")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Подписка успешно отменена"),
            @ApiResponse(responseCode = "400", description = "Нет активной подписки для отмены"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "500", description = "Ошибка API Точка Банк")
    })
    @PostMapping("/tochka/unsubscribe")
    public ResponseEntity<Void> unsubscribeTochka(@AuthenticationPrincipal User user) {
        tochkaPaymentService.unsubscribe(user);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получить список клиентов Точка Банк (customerCode для настройки)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список клиентов с customerCode (нужен с customerType: Business)"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "500", description = "Ошибка API Точка Банк или tochka.api-key не настроен")
    })
    @GetMapping("/tochka/customers")
    public ResponseEntity<TochkaCustomersListResponse> getTochkaCustomers() {
        return ResponseEntity.ok(tochkaPaymentService.getCustomersList());
    }

    /**
     * Callback от Точка Банк при оплате по платёжной ссылке (вебхук acquiringInternetPayment).
     * Тело запроса — JWT (Content-Type: text/plain). Не требует авторизации; подпись проверяется ключом Точки.
     * При status=APPROVED обновляет платёж и тариф пользователя.
     */
    @Operation(summary = "Вебхук Точка Банк (оплата прошла)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Вебхук принят (всегда возвращать 200)"),
    })
    @PostMapping(value = "/webhook/tochka", consumes = "text/plain")
    public ResponseEntity<Void> tochkaWebhook(@RequestBody String body) {
        tochkaWebhookService.handleWebhook(body);
        return ResponseEntity.ok().build();
    }

}
