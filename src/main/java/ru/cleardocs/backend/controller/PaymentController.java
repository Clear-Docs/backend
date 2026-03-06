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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.dto.TochkaPaymentResponseDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.service.TochkaPaymentService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pay")
public class PaymentController {

    private final TochkaPaymentService tochkaPaymentService;

    @Operation(summary = "Инициализация платежа через Точка банк")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная Инициализация платежа через Точка банк",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = TochkaPaymentResponseDto.class))}),
            @ApiResponse(responseCode = "500", description = "Ошибка инициализации платежа")
    })
    @PostMapping("/tochka/createPayment")
    public ResponseEntity<TochkaPaymentResponseDto> createTochkaPayment(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tochkaPaymentService.createPayment(user));
    }

}
