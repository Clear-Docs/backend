package ru.cleardocs.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import ru.cleardocs.backend.config.TestFirebaseConfig;
import org.springframework.test.web.servlet.MockMvc;
import ru.cleardocs.backend.dto.GetMeDto;
import ru.cleardocs.backend.dto.LimitDto;
import ru.cleardocs.backend.dto.PlanDto;
import ru.cleardocs.backend.dto.UserDto;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.security.WithMockFirebaseUser;
import ru.cleardocs.backend.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestFirebaseConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  UserService userService;

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE")
  void getMe_authenticated_returnsUserInfo() throws Exception {
    var userDto = new UserDto("test@example.com", "Test User",
        new PlanDto(PlanCode.FREE, "Бесплатный", 0, 30, new LimitDto(1)));
    when(userService.getMe(any())).thenReturn(new GetMeDto(userDto));

    mockMvc.perform(get("/api/v1/users/me")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value("test@example.com"))
        .andExpect(jsonPath("$.user.name").value("Test User"))
        .andExpect(jsonPath("$.user.plan.code").value("FREE"));
  }

  @Test
  void getMe_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized());
  }
}
