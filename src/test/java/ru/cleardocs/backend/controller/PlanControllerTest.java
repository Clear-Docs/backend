package ru.cleardocs.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import ru.cleardocs.backend.config.TestFirebaseConfig;
import org.springframework.test.web.servlet.MockMvc;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.dto.GetAllPlansDto;
import ru.cleardocs.backend.dto.LimitDto;
import ru.cleardocs.backend.dto.PlanDto;
import ru.cleardocs.backend.service.PlanService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
@Import(TestFirebaseConfig.class)
@ActiveProfiles("test")
class PlanControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  PlanService planService;

  @MockitoBean
  ru.cleardocs.backend.service.UserService userService;

  @Test
  void getAll_returnsPlans() throws Exception {
    var plans = new GetAllPlansDto(List.of(
        new PlanDto(PlanCode.FREE, "Бесплатный", 0, 30, new LimitDto(1)),
        new PlanDto(PlanCode.MONTH, "Подписка на месяц", 990, 30, new LimitDto(10))
    ));
    when(planService.getAll()).thenReturn(plans);

    mockMvc.perform(get("/api/v1/plans"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plans").isArray())
        .andExpect(jsonPath("$.plans.length()").value(2))
        .andExpect(jsonPath("$.plans[0].code").value("FREE"))
        .andExpect(jsonPath("$.plans[0].title").value("Бесплатный"))
        .andExpect(jsonPath("$.plans[1].code").value("MONTH"))
        .andExpect(jsonPath("$.plans[1].priceRub").value(990));
  }

  @Test
  void getAll_emptyList_returnsEmptyArray() throws Exception {
    when(planService.getAll()).thenReturn(new GetAllPlansDto(List.of()));

    mockMvc.perform(get("/api/v1/plans"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plans").isArray())
        .andExpect(jsonPath("$.plans.length()").value(0));
  }
}
