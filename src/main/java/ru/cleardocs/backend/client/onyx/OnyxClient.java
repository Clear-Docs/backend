package ru.cleardocs.backend.client.onyx;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.cleardocs.backend.dto.EntityConnectorDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class OnyxClient {

  private static final String PATH_DOCUMENT_SET = "/document-set";
  private static final String PATH_ADMIN_DOCUMENT_SET = "/admin/document-set";
  private static final String PATH_ADMIN_CONNECTOR_UPLOAD = "/admin/connector/file/upload";
  private static final String PATH_ADMIN_CONNECTOR_CREATE = "/admin/connector-with-mock-credential";
  private static final String PATH_ADMIN_CONNECTOR_DELETE = "/admin/connector";

  private final RestTemplate restTemplate =
      new RestTemplate(new HttpComponentsClientHttpRequestFactory());
  private final String baseUrl;
  private final String managePath;
  private final String apiKey;

  public OnyxClient(
      @Value("${onyx.base-url:http://155.212.162.11:3000/api}") String baseUrl,
      @Value("${onyx.manage-path:/manage}") String managePath,
      @Value("${onyx.api-key:}") String apiKey
  ) {
    this.baseUrl = baseUrl.replaceAll("/$", "");
    this.managePath = managePath.replaceAll("/$", "");
    this.apiKey = apiKey;
  }

  private String url(String path) {
    return baseUrl + managePath + path;
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
      return mapToEntityConnectors(ourDocSet.get());
    } catch (Exception e) {
      log.warn("Failed to fetch connectors from Onyx for docSetId={}: {}", docSetId, e.getMessage());
      return List.of();
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

  private List<EntityConnectorDto> mapToEntityConnectors(OnyxDocumentSetDto docSet) {
    List<EntityConnectorDto> result = new ArrayList<>();
    for (OnyxConnectorSummaryDto cc : docSet.ccPairSummaries()) {
      result.add(new EntityConnectorDto(cc.id(), cc.name(), cc.source()));
    }
    for (OnyxConnectorSummaryDto fc : docSet.federatedConnectorSummaries()) {
      result.add(new EntityConnectorDto(fc.id(), fc.name(), fc.source()));
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
   * Deletes a connector in Onyx.
   * Onyx API: DELETE /manage/admin/connector/{connector_id}
   */
  public void deleteConnector(int connectorId) {
    String deleteUrl = url(PATH_ADMIN_CONNECTOR_DELETE + "/" + connectorId);
    HttpHeaders headers = new HttpHeaders();
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    restTemplate.exchange(
        deleteUrl,
        HttpMethod.DELETE,
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
