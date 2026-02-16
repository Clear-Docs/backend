package ru.cleardocs.backend.dto;

import ru.cleardocs.backend.constant.PlanCode;

public record PlanDto(PlanCode code, String title, int priceRub, int periodDays, LimitDto limit) {
}
