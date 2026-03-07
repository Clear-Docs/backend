package ru.cleardocs.backend.dto;

/**
 * Запрос на создание платежа через Точка Банк.
 * planCode — код плана из GET /api/v1/plans (например MONTH, FREE).
 */
public record TochkaPaymentRequestDto(String planCode) {
}
