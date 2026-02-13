package ru.cleardocs.backend.dto;

public record PlanDto(String code, String title, int priceRub, int periodDays, LimitDto limit) {
}
