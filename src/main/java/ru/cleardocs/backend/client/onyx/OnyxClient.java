package ru.cleardocs.backend.client.onyx;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import ru.cleardocs.backend.dto.EntityConnectorDto;
import ru.cleardocs.backend.exception.BadRequestException;
import ru.cleardocs.backend.exception.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class OnyxClient {

  private static final String PATH_DOCUMENT_SET = "/document-set";
  private static final String PATH_ADMIN_DOCUMENT_SET = "/admin/document-set";
  private static final String PATH_ADMIN_CONNECTOR_UPLOAD = "/admin/connector/file/upload";
  private static final String PATH_ADMIN_CONNECTOR_CREATE = "/admin/connector-with-mock-credential";
  private static final String PATH_ADMIN_CONNECTOR_STATUS = "/admin/connector/status";
  private static final String PATH_ADMIN_INDEXING_STATUS = "/admin/connector/indexing-status";
  private static final String PATH_ADMIN_DELETION_ATTEMPT = "/admin/deletion-attempt";
  private static final String PATH_ADMIN_CC_PAIR = "/admin/cc-pair";
  private static final String PATH_ADMIN_API_KEY = "/admin/api-key";
  private static final String PATH_PERSONA = "/persona";
  private static final String PATH_CHAT_CREATE_SESSION = "/chat/create-chat-session";
  private static final String PATH_CHAT_SEND_MESSAGE = "/chat/send-chat-message";

  private final RestTemplate restTemplate;
  private final RestTemplate onyxStreamingRestTemplate;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String managePath;
  private final String apiKey;

  public OnyxClient(
      @Value("${onyx.base-url:http://155.212.162.11:3000/api}") String baseUrl,
      @Value("${onyx.manage-path:/manage}") String managePath,
      @Value("${onyx.api-key:}") String apiKey,
      @Autowired RestTemplate restTemplate,
      @Autowired @Qualifier("onyxStreamingRestTemplate") RestTemplate onyxStreamingRestTemplate,
      @Autowired ObjectMapper objectMapper
  ) {
    this.restTemplate = restTemplate;
    this.onyxStreamingRestTemplate = onyxStreamingRestTemplate;
    this.objectMapper = objectMapper;
    this.baseUrl = baseUrl.replaceAll("/$", "");
    this.managePath = managePath.replaceAll("/$", "");
    this.apiKey = apiKey;
  }

  private String url(String path) {
    return baseUrl + managePath + path;
  }

  /** URL for API paths not under /manage (e.g. /persona). */
  private String urlApi(String path) {
    return baseUrl + path;
  }

  /**
   * Fetches all document sets from Onyx and returns connectors for the given docSetId.
   * Returns empty list if doc set is not found or on API error.
   */
  public List<EntityConnectorDto> getConnectorsByDocSetId(Integer docSetId) {
    if (docSetId == null) {
      return List.of();
    }
    try {
      List<OnyxDocumentSetDto> documentSets = fetchAllDocumentSets();
      Optional<OnyxDocumentSetDto> ourDocSet = documentSets.stream()
          .filter(ds -> docSetId.equals(ds.id()))
          .findFirst();
      if (ourDocSet.isEmpty()) {
        log.debug("Document set with id {} not found in Onyx response", docSetId);
        return List.of();
      }
      Map<Integer, String> statusByCcPairId = buildStatusByCcPairId();
      return mapToEntityConnectors(ourDocSet.get(), statusByCcPairId);
    } catch (Exception e) {
      log.warn("Failed to fetch connectors from Onyx for docSetId={}: {}", docSetId, e.getMessage());
      return List.of();
    }
  }

  /**
   * Returns all connector names from all document sets in Onyx (global uniqueness check).
   */
  public Set<String> getAllConnectorNames() {
    try {
      List<OnyxDocumentSetDto> docSets = fetchAllDocumentSets();
      return docSets.stream()
          .flatMap(ds -> Stream.concat(
              ds.ccPairSummaries().stream(),
              ds.federatedConnectorSummaries().stream()))
          .map(OnyxConnectorSummaryDto::name)
          .filter(n -> n != null && !n.isBlank())
          .collect(Collectors.toSet());
    } catch (Exception e) {
      log.warn("Failed to fetch all connector names from Onyx: {}", e.getMessage());
      return Set.of();
    }
  }

  private List<OnyxDocumentSetDto> fetchAllDocumentSets() {
    HttpHeaders headers = new HttpHeaders();
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<List<OnyxDocumentSetDto>> response = restTemplate.exchange(
        url(PATH_DOCUMENT_SET),
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<List<OnyxDocumentSetDto>>() {}
    );
    return response.getBody() != null ? response.getBody() : List.of();
  }

  private Map<Integer, String> buildStatusByCcPairId() {
    Map<Integer, String> statusByCcPairId = new HashMap<>();
    for (OnyxConnectorIndexingStatusLiteResponseDto resp : fetchIndexingStatus()) {
      for (OnyxConnectorIndexingStatusLiteDto item : resp.indexingStatuses()) {
        if (item.ccPairId() != null) {
          if (item.ccPairStatus() == null) {
            throw new IllegalStateException("Onyx returned null cc_pair_status for cc_pair_id=" + item.ccPairId());
          }
          statusByCcPairId.put(item.ccPairId(), item.ccPairStatus());
        }
      }
    }
    return statusByCcPairId;
  }

  private List<EntityConnectorDto> mapToEntityConnectors(OnyxDocumentSetDto docSet, Map<Integer, String> statusByCcPairId) {
    List<EntityConnectorDto> result = new ArrayList<>();
    for (OnyxConnectorSummaryDto cc : docSet.ccPairSummaries()) {
      String status = cc.id() != null ? statusByCcPairId.getOrDefault(cc.id(), "UNKNOWN") : "UNKNOWN";
      result.add(new EntityConnectorDto(cc.id(), cc.name(), cc.source(), status));
    }
    for (OnyxConnectorSummaryDto fc : docSet.federatedConnectorSummaries()) {
      String status = fc.id() != null ? statusByCcPairId.getOrDefault(fc.id(), "UNKNOWN") : "UNKNOWN";
      result.add(new EntityConnectorDto(fc.id(), fc.name(), fc.source(), status));
    }
    return result;
  }

  /**
   * Uploads files to Onyx file store. Returns file_paths (FileStore IDs) and file_names
   * for use in connector creation.
   */
  public OnyxFileUploadResponseDto uploadFiles(MultipartFile[] files) throws IOException {
    String url = url(PATH_ADMIN_CONNECTOR_UPLOAD);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    for (MultipartFile file : files) {
      if (file.isEmpty()) {
        continue;
      }
      Resource resource = new ByteArrayResource(file.getBytes()) {
        @Override
        public String getFilename() {
          return file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        }
      };
      body.add("files", resource);
    }

    HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
    ResponseEntity<OnyxFileUploadResponseDto> response = restTemplate.exchange(
        url,
        HttpMethod.POST,
        entity,
        OnyxFileUploadResponseDto.class
    );
    if (response.getBody() == null) {
      throw new IOException("Onyx file upload returned empty response");
    }
    return response.getBody();
  }

  /**
   * Creates a file connector with mock credential in Onyx. Triggers indexing immediately.
   * Returns the CC pair ID (connector-credential pair ID).
   */
  public OnyxCreateConnectorResponseDto createFileConnector(
      String name,
      List<String> fileLocations,
      List<String> fileNames
  ) {
    String url = url(PATH_ADMIN_CONNECTOR_CREATE);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }

    OnyxConnectorCreateRequestDto request = OnyxConnectorCreateRequestDto.forFileConnector(
        name,
        fileLocations,
        fileNames
    );
    HttpEntity<OnyxConnectorCreateRequestDto> entity = new HttpEntity<>(request, headers);
    ResponseEntity<OnyxCreateConnectorResponseDto> response = restTemplate.exchange(
        url,
        HttpMethod.POST,
        entity,
        OnyxCreateConnectorResponseDto.class
    );
    if (response.getBody() == null) {
      throw new RuntimeException("Onyx create connector returned empty response");
    }
    return response.getBody();
  }

  /**
   * Creates a web URL connector with mock credential in Onyx. Always uses recursive crawling.
   * Returns the CC pair ID (connector-credential pair ID).
   */
  public OnyxCreateConnectorResponseDto createUrlConnector(String name, String url) {
    String requestUrl = url(PATH_ADMIN_CONNECTOR_CREATE);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }

    OnyxConnectorCreateRequestDto request = OnyxConnectorCreateRequestDto.forWebConnector(name, url);
    HttpEntity<OnyxConnectorCreateRequestDto> entity = new HttpEntity<>(request, headers);
    ResponseEntity<OnyxCreateConnectorResponseDto> response = restTemplate.exchange(
        requestUrl,
        HttpMethod.POST,
        entity,
        OnyxCreateConnectorResponseDto.class
    );
    if (response.getBody() == null) {
      throw new RuntimeException("Onyx create URL connector returned empty response");
    }
    return response.getBody();
  }

  /**
   * Deletes a connector in Onyx via deletion-attempt (same as Onyx UI).
   * Requires connector_id and credential_id from connector status.
   * EntityConnectorDto.id is cc_pair_id; we resolve to connector_id/credential_id.
   * Connector must be PAUSED before deletion; otherwise throws BadRequestException.
   */
  public void deleteConnector(int ccPairId) {
    String ccPairStatus = getCcPairStatus(ccPairId);
    if (!"PAUSED".equalsIgnoreCase(ccPairStatus)) {
      throw new BadRequestException("Connector must be paused before deletion. Current status: " + ccPairStatus);
    }

    List<OnyxConnectorStatusDto> statuses = fetchConnectorStatus();
    OnyxConnectorStatusDto status = statuses.stream()
        .filter(s -> ccPairId == s.ccPairId())
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Connector not found in Onyx status: cc_pair_id=" + ccPairId));

    if (status.connector() == null || status.connector().id() == null
        || status.credential() == null || status.credential().id() == null) {
      throw new IllegalStateException("Connector or credential id missing for cc_pair_id=" + ccPairId);
    }

    int connectorId = status.connector().id();
    int credentialId = status.credential().id();
    createDeletionAttempt(connectorId, credentialId);
  }

  /**
   * Fetches cc_pair_status from Onyx indexing-status API.
   * Returns status string (e.g. PAUSED, ACTIVE, SCHEDULED).
   * Throws IllegalStateException if Onyx returns null cc_pair_status.
   * Throws NotFoundException if connector not found.
   */
  public String getCcPairStatus(int ccPairId) {
    List<OnyxConnectorIndexingStatusLiteResponseDto> responses = fetchIndexingStatus();
    for (OnyxConnectorIndexingStatusLiteResponseDto resp : responses) {
      for (OnyxConnectorIndexingStatusLiteDto item : resp.indexingStatuses()) {
        if (ccPairId == item.ccPairId()) {
          if (item.ccPairStatus() == null) {
            throw new IllegalStateException("Onyx returned null cc_pair_status for cc_pair_id=" + ccPairId);
          }
          return item.ccPairStatus();
        }
      }
    }
    throw new NotFoundException("Connector not found in Onyx indexing status: cc_pair_id=" + ccPairId);
  }

  private List<OnyxConnectorIndexingStatusLiteResponseDto> fetchIndexingStatus() {
    String requestUrl = url(PATH_ADMIN_INDEXING_STATUS);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    OnyxIndexingStatusRequestDto request = OnyxIndexingStatusRequestDto.allConnectors();
    HttpEntity<OnyxIndexingStatusRequestDto> entity = new HttpEntity<>(request, headers);
    ResponseEntity<List<OnyxConnectorIndexingStatusLiteResponseDto>> response = restTemplate.exchange(
        requestUrl,
        HttpMethod.POST,
        entity,
        new ParameterizedTypeReference<List<OnyxConnectorIndexingStatusLiteResponseDto>>() {}
    );
    return response.getBody() != null ? response.getBody() : List.of();
  }

  private List<OnyxConnectorStatusDto> fetchConnectorStatus() {
    String requestUrl = url(PATH_ADMIN_CONNECTOR_STATUS);
    HttpHeaders headers = new HttpHeaders();
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<List<OnyxConnectorStatusDto>> response = restTemplate.exchange(
        requestUrl,
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<List<OnyxConnectorStatusDto>>() {}
    );
    return response.getBody() != null ? response.getBody() : List.of();
  }

  private void createDeletionAttempt(int connectorId, int credentialId) {
    String requestUrl = url(PATH_ADMIN_DELETION_ATTEMPT);
    log.info("Onyx API request: POST {} connector_id={} credential_id={}", requestUrl, connectorId, credentialId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    OnyxDeletionAttemptRequestDto request = new OnyxDeletionAttemptRequestDto(connectorId, credentialId);
    HttpEntity<OnyxDeletionAttemptRequestDto> entity = new HttpEntity<>(request, headers);
    restTemplate.exchange(
        requestUrl,
        HttpMethod.POST,
        entity,
        Void.class
    );
  }

  /**
   * Creates a document set in Onyx with the given name, description, and connector ids.
   * Returns the new document set id.
   */
  public int createDocumentSet(String name, String description, List<Integer> ccPairIds) {
    String url = url(PATH_ADMIN_DOCUMENT_SET);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    OnyxDocumentSetCreateRequestDto request = new OnyxDocumentSetCreateRequestDto(
        name,
        description != null ? description : "",
        ccPairIds,
        true,
        Collections.emptyList(),
        Collections.emptyList()
    );
    HttpEntity<OnyxDocumentSetCreateRequestDto> entity = new HttpEntity<>(request, headers);
    ResponseEntity<Integer> response = restTemplate.exchange(
        url,
        HttpMethod.POST,
        entity,
        Integer.class
    );
    if (response.getBody() == null) {
      throw new RuntimeException("Onyx create document set returned empty response");
    }
    return response.getBody();
  }

  /**
   * Updates a document set in Onyx with the new list of connector ids.
   * Onyx API expects PATCH (not POST) per Swagger: /manage/admin/document-set
   */
  public void updateDocumentSet(OnyxDocumentSetUpdateRequestDto request) {
    String url = url(PATH_ADMIN_DOCUMENT_SET);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    HttpEntity<OnyxDocumentSetUpdateRequestDto> entity = new HttpEntity<>(request, headers);
    restTemplate.exchange(
        url,
        HttpMethod.PATCH,
        entity,
        Void.class
    );
  }

  /**
   * Updates connector (cc_pair) status in Onyx.
   * Onyx API: PUT /manage/admin/cc-pair/{cc_pair_id}/status with {"status": "PAUSED"|"ACTIVE"|...}
   */
  public void updateConnectorStatus(int ccPairId, String onyxStatus) {
    String requestUrl = url(PATH_ADMIN_CC_PAIR + "/" + ccPairId + "/status");
    log.info("Onyx API request: PUT {} status={}", requestUrl, onyxStatus);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    OnyxCcStatusUpdateRequestDto request = new OnyxCcStatusUpdateRequestDto(onyxStatus);
    HttpEntity<OnyxCcStatusUpdateRequestDto> entity = new HttpEntity<>(request, headers);
    restTemplate.exchange(
        requestUrl,
        HttpMethod.PUT,
        entity,
        Void.class
    );
  }

  /** Pauses a connector in Onyx. */
  public void pauseConnector(int ccPairId) {
    updateConnectorStatus(ccPairId, "PAUSED");
  }

  /** Resumes (activates) a connector in Onyx. */
  public void resumeConnector(int ccPairId) {
    updateConnectorStatus(ccPairId, "ACTIVE");
  }

  /**
   * Creates an API key in Onyx for the user (role: limited, basic, admin, etc.).
   * Calls POST /manage/admin/api-key (verify in your Onyx Swagger: http://155.212.162.11:3000/api/docs).
   * If the endpoint does not exist, create API keys manually in Onyx Admin Panel.
   */
  public String createApiKey(String name, String role) {
    String requestUrl = urlApi(PATH_ADMIN_API_KEY);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    OnyxApiKeyCreateRequestDto request = new OnyxApiKeyCreateRequestDto(
        name != null ? name : "Chat Key",
        role != null ? role : "limited"
    );
    HttpEntity<OnyxApiKeyCreateRequestDto> entity = new HttpEntity<>(request, headers);
    ResponseEntity<OnyxApiKeyCreateResponseDto> response = restTemplate.exchange(
        requestUrl,
        HttpMethod.POST,
        entity,
        OnyxApiKeyCreateResponseDto.class
    );
    OnyxApiKeyCreateResponseDto body = response.getBody();
    if (body == null || body.getKeyValue() == null || body.getKeyValue().isBlank()) {
      throw new RuntimeException("Onyx create API key returned empty key");
    }
    return body.getKeyValue();
  }

  /**
   * Creates a persona (agent) in Onyx with the given document set attached.
   * Onyx API: POST /persona (PersonaUpsertRequest).
   * Returns the persona_id.
   */
  public int createPersonaWithDocumentSet(String name, int docSetId) {
    String requestUrl = urlApi(PATH_PERSONA);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    OnyxPersonaUpsertRequestDto request = OnyxPersonaUpsertRequestDto.forDocumentSet(name, docSetId);
    HttpEntity<OnyxPersonaUpsertRequestDto> entity = new HttpEntity<>(request, headers);
    ResponseEntity<OnyxPersonaSnapshotDto> response = restTemplate.exchange(
        requestUrl,
        HttpMethod.POST,
        entity,
        OnyxPersonaSnapshotDto.class
    );
    OnyxPersonaSnapshotDto body = response.getBody();
    if (body == null || body.id() == null) {
      throw new RuntimeException("Onyx create persona returned empty id");
    }
    return body.id();
  }

  /**
   * Creates a chat session in Onyx. Proxies POST /chat/create-chat-session.
   * Forwards the Authorization header from the incoming request as-is.
   */
  public Map<String, Object> createChatSession(String authorizationHeader, @NotNull Map<String, Object> request) {
    String requestUrl = urlApi(PATH_CHAT_CREATE_SESSION);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (authorizationHeader != null && !authorizationHeader.isBlank()) {
      headers.set("Authorization", authorizationHeader);
    }
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        requestUrl,
        HttpMethod.POST,
        entity,
        new ParameterizedTypeReference<Map<String, Object>>() {}
    );
    return response.getBody();
  }

  /**
   * Proxies send-chat-message to Onyx API. Streams response directly to outputStream (InputStream pipe).
   * Flushes after each chunk to ensure end of stream reaches client (avoids truncation).
   */
  public void streamSendChatMessage(String authorizationHeader, @NotNull Map<String, Object> request, OutputStream outputStream) throws IOException {
    String requestUrl = urlApi(PATH_CHAT_SEND_MESSAGE);
    Object message = request.get("message");
    Object chatSessionId = request.get("chat_session_id");
    String msgPreview = message != null ? message.toString() : "";
    if (msgPreview.length() > 80) {
      msgPreview = msgPreview.substring(0, 80) + "...";
    }
    long startTime = System.currentTimeMillis();
    log.info("sendChatMessage Onyx start sessionId={} messagePreview={} url={}", chatSessionId, msgPreview, requestUrl);
    Map<String, Object> requestToSend = new HashMap<>(request);
    @SuppressWarnings("unchecked")
    Map<String, Object> llmOverride = request.get("llm_override") instanceof Map
        ? new HashMap<>((Map<String, Object>) request.get("llm_override"))
        : new HashMap<>();
    llmOverride.put("temperature", 0);
    requestToSend.put("llm_override", llmOverride);

    RequestCallback requestCallback = req -> {
      req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      if (authorizationHeader != null && !authorizationHeader.isBlank()) {
        req.getHeaders().set("Authorization", authorizationHeader);
      }
      objectMapper.writeValue(req.getBody(), requestToSend);
    };
    ResponseExtractor<Void> responseExtractor = response -> {
      int status = response.getStatusCode().value();
      var contentType = response.getHeaders().getContentType();
      log.debug("sendChatMessage Onyx response sessionId={} status={} contentType={}", chatSessionId, status, contentType);
      try (InputStream in = response.getBody()) {
        if (in == null) {
          log.error("sendChatMessage Onyx error sessionId={} error=response body is null", chatSessionId);
          throw new IOException("Onyx response body is null");
        }
        byte[] buffer = new byte[4096];
        int n;
        long totalBytes = 0;
        int chunkCount = 0;
        while ((n = in.read(buffer)) != -1) {
          outputStream.write(buffer, 0, n);
          outputStream.flush();  // Flush each chunk — ensures end of phrase reaches client
          totalBytes += n;
          chunkCount++;
          if (chunkCount > 0 && chunkCount % 50 == 0) {
            log.debug("sendChatMessage streaming sessionId={} chunks={} bytes={}", chatSessionId, chunkCount, totalBytes);
          }
        }
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("sendChatMessage Onyx done sessionId={} bytes={} chunks={} elapsed_ms={}", chatSessionId, totalBytes, chunkCount, elapsed);
      } catch (IOException e) {
        long elapsed = System.currentTimeMillis() - startTime;
        log.error("sendChatMessage Onyx error sessionId={} elapsed_ms={} error={} (possible client disconnect)",
            chatSessionId, elapsed, e.getMessage());
        throw e;
      }
      return null;
    };
    try {
      onyxStreamingRestTemplate.execute(requestUrl, org.springframework.http.HttpMethod.POST, requestCallback, responseExtractor);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - startTime;
      log.error("sendChatMessage Onyx error sessionId={} elapsed_ms={} error={}", chatSessionId, elapsed, e.getMessage());
      throw e;
    }
  }

  /**
   * Fetches a document set by id from Onyx.
   * Returns empty if not found.
   */
  public Optional<OnyxDocumentSetDto> getDocumentSetById(Integer id) {
    if (id == null) {
      return Optional.empty();
    }
    try {
      List<OnyxDocumentSetDto> documentSets = fetchAllDocumentSets();
      return documentSets.stream()
          .filter(ds -> id.equals(ds.id()))
          .findFirst();
    } catch (Exception e) {
      log.warn("Failed to fetch document set id={}: {}", id, e.getMessage());
      return Optional.empty();
    }
  }
}
