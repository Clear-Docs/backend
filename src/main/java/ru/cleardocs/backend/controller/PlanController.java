package ru.cleardocs.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.dto.GetPlansDto;
import ru.cleardocs.backend.service.PlanService;

@Slf4j
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

  private final PlanService planService;

  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public GetPlansDto getPlans() {
    log.info("getPlans() - starts");
    GetPlansDto response = planService.getPlans();
    log.info("getPlans() - ends with response = {}", response);
    return response;
  }
}
