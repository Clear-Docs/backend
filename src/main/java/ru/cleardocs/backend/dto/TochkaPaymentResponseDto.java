package ru.cleardocs.backend.dto;

/**
 * Ответ с ссылкой на оплату. Клиент должен открывать paymentUrl в новом окне (target="_blank").
 */
public record TochkaPaymentResponseDto(String paymentUrl, boolean openInNewWindow) {

    public TochkaPaymentResponseDto(String paymentUrl) {
        this(paymentUrl, true);
    }
}
