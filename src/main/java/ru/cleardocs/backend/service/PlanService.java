package ru.cleardocs.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cleardocs.backend.dto.GetPlansDto;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.mapper.PlanMapper;
import ru.cleardocs.backend.repository.PlanRepository;

import java.util.List;

@Slf4j
@Service
public class PlanService {

  private final PlanMapper planMapper;
  private final PlanRepository planRepository;

  public PlanService(PlanMapper planMapper, PlanRepository planRepository) {
    this.planMapper = planMapper;
    this.planRepository = planRepository;
  }

  public GetPlansDto getPlans() {
    log.info("getPlans() - starts");
    List<Plan> plans = planRepository.findAll();
    log.info("getPlans() - ends with {} plans", plans.size());
    return new GetPlansDto(plans.stream().map(planMapper::toDto).toList());
  }
}
