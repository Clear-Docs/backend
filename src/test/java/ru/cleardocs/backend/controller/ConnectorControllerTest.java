package ru.cleardocs.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.cleardocs.backend.client.onyx.OnyxClient;
import ru.cleardocs.backend.config.TestFirebaseConfig;
import ru.cleardocs.backend.security.WithMockFirebaseUser;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestFirebaseConfig.class)
@ActiveProfiles("test")
class ConnectorControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  OnyxClient onyxClient;

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE")
  void getConnectors_authenticatedUserWithoutDocSet_returnsEmptyList() throws Exception {
    mockMvc.perform(get("/api/v1/connectors")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectors").isArray())
        .andExpect(jsonPath("$.connectors").isEmpty());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void getConnectors_noConnectorsInDatabase_returnsEmptyList() throws Exception {
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/connectors")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectors").isArray())
        .andExpect(jsonPath("$.connectors").isEmpty());
  }

  @Test
  void getConnectors_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/connectors"))
        .andExpect(status().isUnauthorized());
  }
}
