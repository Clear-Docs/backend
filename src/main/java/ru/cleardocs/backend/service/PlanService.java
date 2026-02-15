package ru.cleardocs.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.dto.GetAllPlansDto;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.exception.NotFoundException;
import ru.cleardocs.backend.mapper.PlanMapper;
import ru.cleardocs.backend.repository.PlanRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PlanService {

  private final PlanMapper planMapper;
  private final PlanRepository planRepository;

  public PlanService(PlanMapper planMapper, PlanRepository planRepository) {
    this.planMapper = planMapper;
    this.planRepository = planRepository;
  }

  public GetAllPlansDto getAll() {
    log.info("getAll() - starts");
    List<Plan> plans = planRepository.findAll();
    log.info("getAll() - ends with {} plans", plans.size());
    return new GetAllPlansDto(plans.stream().map(planMapper::toDto).toList());
  }

  public Plan getByCode(PlanCode code) {
    log.info("getByCode() - starts with code = {}", code);
    Optional<Plan> planOptional = planRepository.findByCode(code);
    if (planOptional.isEmpty()) {
      throw new NotFoundException("Plan is not found with code = " + code);
    }
    log.info("getByCode() - ends with plan = {}", planOptional.get());
    return planOptional.get();
  }
}
