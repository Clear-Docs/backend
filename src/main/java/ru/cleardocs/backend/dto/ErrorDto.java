package ru.cleardocs.backend.dto;

import java.time.LocalDateTime;

public record ErrorDto(String message, int status, LocalDateTime timestamp) {
}
