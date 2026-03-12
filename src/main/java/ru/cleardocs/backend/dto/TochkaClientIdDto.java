package ru.cleardocs.backend.dto;

/**
 * Client ID из JWT API-ключа Точка Банк (claim "iss").
 * Используется в API вебхуков: GET webhook/v1.0/{client-id}
 */
public record TochkaClientIdDto(String clientId) {
}
