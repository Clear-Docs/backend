package ru.cleardocs.backend.mapper;

import org.springframework.stereotype.Component;
import ru.cleardocs.backend.dto.PlanDto;
import ru.cleardocs.backend.entity.Plan;

@Component
public class PlanMapper {

  private final LimitMapper limitMapper;

  public PlanMapper(LimitMapper limitMapper) {
    this.limitMapper = limitMapper;
  }

  public PlanDto toDto(Plan plan) {
    return new PlanDto(plan.getCode(), plan.getTitle(), plan.getPriceRub(), plan.getPeriodDays(), limitMapper.toDto(plan.getLimit()));
  }
}
