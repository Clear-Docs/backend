package ru.cleardocs.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.cleardocs.backend.client.onyx.OnyxClient;
import ru.cleardocs.backend.client.onyx.OnyxCreateConnectorResponseDto;
import ru.cleardocs.backend.client.onyx.OnyxDocumentSetUpdateRequestDto;
import ru.cleardocs.backend.client.onyx.OnyxFileUploadResponseDto;
import ru.cleardocs.backend.dto.CreateConnectorResponseDto;
import ru.cleardocs.backend.dto.EntityConnectorDto;
import ru.cleardocs.backend.dto.GetConnectorsDto;
import ru.cleardocs.backend.dto.UpdateConnectorRequestDto;
import ru.cleardocs.backend.entity.Limit;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.BadRequestException;
import ru.cleardocs.backend.exception.NotFoundException;
import ru.cleardocs.backend.repository.UserRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConnectorService {

  private static final String DEFAULT_DOCUMENT_SET_NAME = "Docs";

  private final OnyxClient onyxClient;
  private final UserRepository userRepository;

  public ConnectorService(OnyxClient onyxClient, UserRepository userRepository) {
    this.onyxClient = onyxClient;
    this.userRepository = userRepository;
  }

  public GetConnectorsDto getConnectors(User user) {
    log.info("getConnectors() - starts with user id = {}, docSetId = {}", user.getId(), user.getDocSetId());
    List<EntityConnectorDto> connectors;
    if (user.getDocSetId() == null) {
      log.info("getConnectors() - user has no docSetId, returning empty list");
      connectors = Collections.emptyList();
    } else {
      connectors = onyxClient.getConnectorsByDocSetId(user.getDocSetId());
    }

    int maxConnectors = 0;
    Plan plan = user.getPlan();
    if (plan != null && plan.getLimit() != null) {
      maxConnectors = plan.getLimit().getMaxConnectors();
    }
    boolean canAdd = connectors.size() < maxConnectors;

    log.info("getConnectors() - ends with connectors count = {}, maxConnectors = {}, canAdd = {}",
        connectors.size(), maxConnectors, canAdd);
    return new GetConnectorsDto(connectors, canAdd);
  }

  @Transactional
  public CreateConnectorResponseDto createFileConnector(User user, String name, MultipartFile[] files) throws IOException {
    log.info("createFileConnector() - starts with user id = {}, docSetId = {}, name = {}",
        user.getId(), user.getDocSetId(), name);

    if (name == null || name.isBlank()) {
      throw new BadRequestException("Connector name is required.");
    }

    if (files == null || files.length == 0) {
      throw new BadRequestException("At least one file is required.");
    }

    int maxConnectors = 0;
    Plan plan = user.getPlan();
    if (plan != null && plan.getLimit() != null) {
      Limit limit = plan.getLimit();
      maxConnectors = limit.getMaxConnectors();
    }

    List<EntityConnectorDto> existingConnectors = user.getDocSetId() == null
        ? Collections.emptyList()
        : onyxClient.getConnectorsByDocSetId(user.getDocSetId());
    if (existingConnectors.size() >= maxConnectors) {
      log.warn("createFileConnector() - connector limit reached for user id = {}, current = {}, max = {}",
          user.getId(), existingConnectors.size(), maxConnectors);
      throw new BadRequestException(String.format(
          "Connector limit reached. Current: %d, Maximum allowed: %d",
          existingConnectors.size(), maxConnectors));
    }

    OnyxFileUploadResponseDto uploadResponse = onyxClient.uploadFiles(files);
    List<String> fileLocations = uploadResponse.filePaths();
    List<String> fileNames = uploadResponse.fileNames();

    if (fileLocations.isEmpty()) {
      throw new BadRequestException("No valid files were uploaded.");
    }

    OnyxCreateConnectorResponseDto createResponse = onyxClient.createFileConnector(name, fileLocations, fileNames);

    if (!Boolean.TRUE.equals(createResponse.success()) || createResponse.data() == null) {
      throw new RuntimeException("Failed to create connector in Onyx: " + createResponse.message());
    }

    int ccPairId = createResponse.data();

    if (user.getDocSetId() == null) {
      createAndLinkDocumentSet(user, ccPairId);
    } else {
      addConnectorToExistingDocumentSet(user, existingConnectors, ccPairId);
    }

    log.info("createFileConnector() - ends with cc_pair_id = {}", ccPairId);
    return new CreateConnectorResponseDto(ccPairId, name, "file");
  }

  @Transactional
  public CreateConnectorResponseDto createUrlConnector(User user, String name, String url) {
    log.info("createUrlConnector() - starts with user id = {}, docSetId = {}, name = {}, url = {}",
        user.getId(), user.getDocSetId(), name, url);

    if (name == null || name.isBlank()) {
      throw new BadRequestException("Connector name is required.");
    }

    if (url == null || url.isBlank()) {
      throw new BadRequestException("URL is required.");
    }

    int maxConnectors = 0;
    Plan plan = user.getPlan();
    if (plan != null && plan.getLimit() != null) {
      maxConnectors = plan.getLimit().getMaxConnectors();
    }

    List<EntityConnectorDto> existingConnectors = user.getDocSetId() == null
        ? Collections.emptyList()
        : onyxClient.getConnectorsByDocSetId(user.getDocSetId());
    if (existingConnectors.size() >= maxConnectors) {
      log.warn("createUrlConnector() - connector limit reached for user id = {}, current = {}, max = {}",
          user.getId(), existingConnectors.size(), maxConnectors);
      throw new BadRequestException(String.format(
          "Connector limit reached. Current: %d, Maximum allowed: %d",
          existingConnectors.size(), maxConnectors));
    }

    OnyxCreateConnectorResponseDto createResponse = onyxClient.createUrlConnector(name, url);

    if (!Boolean.TRUE.equals(createResponse.success()) || createResponse.data() == null) {
      throw new RuntimeException("Failed to create URL connector in Onyx: " + createResponse.message());
    }

    int ccPairId = createResponse.data();

    if (user.getDocSetId() == null) {
      createAndLinkDocumentSet(user, ccPairId);
    } else {
      addConnectorToExistingDocumentSet(user, existingConnectors, ccPairId);
    }

    log.info("createUrlConnector() - ends with cc_pair_id = {}", ccPairId);
    return new CreateConnectorResponseDto(ccPairId, name, "web");
  }

  public void deleteConnector(User user, int connectorId) {
    log.info("deleteConnector() - starts with user id = {}, docSetId = {}, connectorId = {}",
        user.getId(), user.getDocSetId(), connectorId);

    if (user.getDocSetId() == null) {
      throw new NotFoundException("User has no document set");
    }

    List<EntityConnectorDto> connectors = onyxClient.getConnectorsByDocSetId(user.getDocSetId());
    boolean connectorBelongsToUser = connectors.stream()
        .anyMatch(c -> c.id() != null && c.id() == connectorId);
    if (!connectorBelongsToUser) {
      throw new NotFoundException("Connector not found");
    }

    onyxClient.deleteConnector(connectorId);

    log.info("deleteConnector() - ends, connector {} deleted", connectorId);
  }

  public void updateConnector(User user, int connectorId, UpdateConnectorRequestDto request) {
    log.info("updateConnector() - starts with user id = {}, docSetId = {}, connectorId = {}, status = {}",
        user.getId(), user.getDocSetId(), connectorId, request != null ? request.status() : null);

    if (user.getDocSetId() == null) {
      throw new NotFoundException("User has no document set");
    }

    List<EntityConnectorDto> connectors = onyxClient.getConnectorsByDocSetId(user.getDocSetId());
    boolean connectorBelongsToUser = connectors.stream()
        .anyMatch(c -> c.id() != null && c.id() == connectorId);
    if (!connectorBelongsToUser) {
      throw new NotFoundException("Connector not found");
    }

    if (request == null || request.status() == null || request.status().isBlank()) {
      throw new BadRequestException("status is required (paused or active)");
    }
    if (request.isPaused()) {
      onyxClient.pauseConnector(connectorId);
      log.info("updateConnector() - connector {} paused", connectorId);
    } else if (request.isActive()) {
      onyxClient.resumeConnector(connectorId);
      log.info("updateConnector() - connector {} resumed", connectorId);
    } else {
      throw new BadRequestException("status must be 'paused' or 'active', got: " + request.status());
    }
  }

  private String documentSetNameFor(User user) {
    String email = (user.getEmail() != null && !user.getEmail().isBlank())
        ? user.getEmail()
        : "user";
    String suffix = user.getId() != null
        ? user.getId().toString().substring(0, 8)
        : UUID.randomUUID().toString().substring(0, 8);
    String name;
    if (user.getName() != null && !user.getName().isBlank()) {
      name = DEFAULT_DOCUMENT_SET_NAME + " - " + user.getName() + " (" + email + ") - " + suffix;
    } else {
      name = DEFAULT_DOCUMENT_SET_NAME + " (" + email + ") - " + suffix;
    }
    return name.length() > 255 ? name.substring(0, 255) : name;
  }

  private void createAndLinkDocumentSet(User user, int ccPairId) {
    String docSetName = documentSetNameFor(user);
    int newDocSetId = onyxClient.createDocumentSet(docSetName, "", List.of(ccPairId));
    user.setDocSetId(newDocSetId);
    userRepository.save(user);
    log.info("createAndLinkDocumentSet() - created document set id = {} for user id = {}", newDocSetId, user.getId());
  }

  private void addConnectorToExistingDocumentSet(User user, List<EntityConnectorDto> existingConnectors, int ccPairId) {
    List<Integer> existingCcPairIds = existingConnectors.stream()
        .map(EntityConnectorDto::id)
        .collect(Collectors.toList());
    List<Integer> allCcPairIds = new ArrayList<>(existingCcPairIds);
    allCcPairIds.add(ccPairId);

    var docSetOpt = onyxClient.getDocumentSetById(user.getDocSetId());
    if (docSetOpt.isEmpty()) {
      // Document set was deleted in Onyx or data inconsistency - auto-heal by creating a new one
      log.warn("createFileConnector() - document set id = {} not found in Onyx, creating new document set for user id = {}",
          user.getDocSetId(), user.getId());
      createAndLinkDocumentSet(user, ccPairId);
      return;
    }

    var docSet = docSetOpt.get();
    OnyxDocumentSetUpdateRequestDto updateRequest = new OnyxDocumentSetUpdateRequestDto(
        docSet.id(),
        docSet.description() != null ? docSet.description() : "",
        allCcPairIds,
        docSet.isPublic(),
        docSet.users(),
        docSet.groups()
    );
    onyxClient.updateDocumentSet(updateRequest);
    log.info("createFileConnector() - updated document set id = {} with new connector", docSet.id());
  }
}
