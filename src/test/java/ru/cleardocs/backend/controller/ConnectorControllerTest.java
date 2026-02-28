package ru.cleardocs.backend.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.cleardocs.backend.client.onyx.OnyxClient;
import ru.cleardocs.backend.dto.EntityConnectorDto;
import ru.cleardocs.backend.client.onyx.OnyxCreateConnectorResponseDto;
import ru.cleardocs.backend.client.onyx.OnyxDocumentSetDto;
import ru.cleardocs.backend.client.onyx.OnyxFileUploadResponseDto;
import ru.cleardocs.backend.config.TestFirebaseConfig;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.repository.UserRepository;
import ru.cleardocs.backend.security.WithMockFirebaseUser;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

  @MockitoBean
  UserRepository userRepository;

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE")
  void getConnectors_authenticatedUserWithoutDocSet_returnsEmptyList() throws Exception {
    mockMvc.perform(get("/api/v1/connectors")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectors").isArray())
        .andExpect(jsonPath("$.connectors").isEmpty())
        .andExpect(jsonPath("$.canAdd").value(true));
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void getConnectors_noConnectorsInDatabase_returnsEmptyList() throws Exception {
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/connectors")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectors").isArray())
        .andExpect(jsonPath("$.connectors").isEmpty())
        .andExpect(jsonPath("$.canAdd").value(true));
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void getConnectors_limitReached_returnsCanAddFalse() throws Exception {
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(
        List.of(new EntityConnectorDto(1, "Connector 1", "file", "ACTIVE"))
    );

    mockMvc.perform(get("/api/v1/connectors")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectors").isArray())
        .andExpect(jsonPath("$.connectors.length()").value(1))
        .andExpect(jsonPath("$.connectors[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.canAdd").value(false));
  }

  @Test
  void getConnectors_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/connectors"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void createFileConnector_authenticatedUser_returns201() throws Exception {
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(List.of());
    when(onyxClient.getDocumentSetById(42)).thenReturn(Optional.of(
        new OnyxDocumentSetDto(42, "Documents", "", List.of(), List.of(), true, List.of(), List.of())
    ));

    when(onyxClient.uploadFiles(any())).thenReturn(
        new OnyxFileUploadResponseDto(List.of("file-id-1"), List.of("doc.pdf"), null)
    );

    when(onyxClient.createFileConnector(
        eq("My Connector"),
        anyList(),
        anyList()
    )).thenReturn(new OnyxCreateConnectorResponseDto(true, "Created", 123));

    MockMultipartFile file = new MockMultipartFile(
        "files",
        "doc.pdf",
        MediaType.APPLICATION_PDF_VALUE,
        "test content".getBytes()
    );

    mockMvc.perform(
            multipart("/api/v1/connectors")
                .file(file)
                .param("name", "My Connector")
                .with(securityContext(SecurityContextHolder.getContext()))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(123))
        .andExpect(jsonPath("$.name").value("My Connector"))
        .andExpect(jsonPath("$.type").value("file"));
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE")
  void createFileConnector_userWithoutDocSet_createsDocumentSetAndSavesToDb() throws Exception {
    when(onyxClient.uploadFiles(any())).thenReturn(
        new OnyxFileUploadResponseDto(List.of("file-id-1"), List.of("doc.pdf"), null)
    );
    when(onyxClient.createFileConnector(eq("My Connector"), anyList(), anyList()))
        .thenReturn(new OnyxCreateConnectorResponseDto(true, "Created", 456));
    when(onyxClient.createDocumentSet(
        argThat(name -> name != null && name.startsWith("Documents") && name.contains("test@example.com")),
        eq(""),
        eq(List.of(456)))).thenReturn(123);

    MockMultipartFile file = new MockMultipartFile(
        "files",
        "doc.pdf",
        MediaType.APPLICATION_PDF_VALUE,
        "test content".getBytes()
    );

    mockMvc.perform(
            multipart("/api/v1/connectors")
                .file(file)
                .param("name", "My Connector")
                .with(securityContext(SecurityContextHolder.getContext()))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(456))
        .andExpect(jsonPath("$.name").value("My Connector"))
        .andExpect(jsonPath("$.type").value("file"));

    var captor = org.mockito.ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User savedUser = captor.getValue();
    assertNotNull(savedUser.getDocSetId());
    assertEquals(123, savedUser.getDocSetId());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void createUrlConnector_authenticatedUser_returns201() throws Exception {
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(List.of());
    when(onyxClient.getDocumentSetById(42)).thenReturn(Optional.of(
        new OnyxDocumentSetDto(42, "Documents", "", List.of(), List.of(), true, List.of(), List.of())
    ));
    when(onyxClient.createUrlConnector(eq("My Site"), eq("https://example.com")))
        .thenReturn(new OnyxCreateConnectorResponseDto(true, "Created", 123));

    mockMvc.perform(post("/api/v1/connectors/url")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"My Site\",\"url\":\"https://example.com\"}")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(123))
        .andExpect(jsonPath("$.name").value("My Site"))
        .andExpect(jsonPath("$.type").value("web"));
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE")
  void createUrlConnector_userWithoutDocSet_createsDocumentSetAndSavesToDb() throws Exception {
    when(onyxClient.createUrlConnector(eq("My Site"), eq("https://example.com")))
        .thenReturn(new OnyxCreateConnectorResponseDto(true, "Created", 456));
    when(onyxClient.createDocumentSet(
        argThat(name -> name != null && name.startsWith("Documents") && name.contains("test@example.com")),
        eq(""),
        eq(List.of(456)))).thenReturn(123);

    mockMvc.perform(post("/api/v1/connectors/url")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"My Site\",\"url\":\"https://example.com\"}")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(456))
        .andExpect(jsonPath("$.name").value("My Site"))
        .andExpect(jsonPath("$.type").value("web"));

    var captor = org.mockito.ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User savedUser = captor.getValue();
    assertNotNull(savedUser.getDocSetId());
    assertEquals(123, savedUser.getDocSetId());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void createUrlConnector_missingName_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/connectors/url")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"url\":\"https://example.com\"}")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void createUrlConnector_missingUrl_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/connectors/url")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"My Site\"}")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void createUrlConnector_limitReached_returns400() throws Exception {
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(
        List.of(new EntityConnectorDto(1, "Connector 1", "file", "ACTIVE"))
    );

    mockMvc.perform(post("/api/v1/connectors/url")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"My Site\",\"url\":\"https://example.com\"}")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createUrlConnector_unauthenticated_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/connectors/url")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"My Site\",\"url\":\"https://example.com\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void updateConnector_statusPaused_putsConnectorOnPause() throws Exception {
    EntityConnectorDto connector = new EntityConnectorDto(123, "My Connector", "file", "ACTIVE");
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(List.of(connector));

    mockMvc.perform(patch("/api/v1/connectors/123")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"paused\"}")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isNoContent());

    verify(onyxClient).pauseConnector(123);
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void deleteConnector_deletesConnectorInOnyx() throws Exception {
    EntityConnectorDto connector1 = new EntityConnectorDto(123, "My Connector", "file", "ACTIVE");
    EntityConnectorDto connector2 = new EntityConnectorDto(456, "Other Connector", "file", "ACTIVE");
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(List.of(connector1, connector2));

    mockMvc.perform(delete("/api/v1/connectors/123")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isNoContent());

    verify(onyxClient).deleteConnector(123);
    verify(onyxClient, never()).updateDocumentSet(any());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void deleteConnector_lastConnector_deletesConnectorInOnyx() throws Exception {
    EntityConnectorDto connector = new EntityConnectorDto(123, "My Connector", "file", "ACTIVE");
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(List.of(connector));

    mockMvc.perform(delete("/api/v1/connectors/123")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isNoContent());

    verify(onyxClient).deleteConnector(123);
    verify(onyxClient, never()).updateDocumentSet(any());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE", docSetId = 42)
  void deleteConnector_connectorNotInUserDocSet_returns404() throws Exception {
    when(onyxClient.getConnectorsByDocSetId(42)).thenReturn(
        List.of(new EntityConnectorDto(456, "Other Connector", "file", "ACTIVE"))
    );

    mockMvc.perform(delete("/api/v1/connectors/123")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockFirebaseUser(email = "test@example.com", name = "Test User", planCode = "FREE")
  void deleteConnector_userWithoutDocSet_returns404() throws Exception {
    mockMvc.perform(delete("/api/v1/connectors/123")
            .with(securityContext(SecurityContextHolder.getContext())))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteConnector_unauthenticated_returns401() throws Exception {
    mockMvc.perform(delete("/api/v1/connectors/123"))
        .andExpect(status().isUnauthorized());
  }
}
