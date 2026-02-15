package ru.cleardocs.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.dto.GetAllPlansDto;
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
  public ResponseEntity<GetAllPlansDto> getAll() {
    log.info("getAll() - starts");
    GetAllPlansDto response = planService.getAll();
    log.info("getAll() - ends with response = {}", response);
    return ResponseEntity.ok(response);
  }
}
