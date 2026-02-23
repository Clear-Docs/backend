package ru.cleardocs.backend.client.onyx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.cleardocs.backend.dto.EntityConnectorDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class OnyxClient {

  private final RestTemplate restTemplate = new RestTemplate();
  private final String documentSetsUrl;
  private final String apiKey;

  public OnyxClient(
      @Value("${onyx.base-url:http://155.212.162.11:3000/api}") String baseUrl,
      @Value("${onyx.document-sets-path:/document-sets}") String documentSetsPath,
      @Value("${onyx.api-key:}") String apiKey
  ) {
    this.documentSetsUrl = baseUrl.replaceAll("/$", "") + documentSetsPath;
    this.apiKey = apiKey;
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
        documentSetsUrl,
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
}
