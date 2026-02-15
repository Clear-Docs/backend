package ru.cleardocs.backend.dto;

import java.util.List;

public record GetAllPlansDto(List<PlanDto> plans) {
}
