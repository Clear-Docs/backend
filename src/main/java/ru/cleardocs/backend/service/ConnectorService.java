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
import ru.cleardocs.backend.entity.Limit;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.BadRequestException;
import ru.cleardocs.backend.repository.UserRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConnectorService {

  private static final String DEFAULT_DOCUMENT_SET_NAME = "Documents";

  private final OnyxClient onyxClient;
  private final UserRepository userRepository;

  public ConnectorService(OnyxClient onyxClient, UserRepository userRepository) {
    this.onyxClient = onyxClient;
    this.userRepository = userRepository;
  }

  public GetConnectorsDto getConnectors(User user) {
    log.info("getConnectors() - starts with user id = {}, docSetId = {}", user.getId(), user.getDocSetId());
    if (user.getDocSetId() == null) {
      log.info("getConnectors() - user has no docSetId, returning empty list");
      return new GetConnectorsDto(Collections.emptyList());
    }
    List<EntityConnectorDto> connectors = onyxClient.getConnectorsByDocSetId(user.getDocSetId());
    log.info("getConnectors() - ends with connectors count = {}", connectors.size());
    return new GetConnectorsDto(connectors);
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

  private void createAndLinkDocumentSet(User user, int ccPairId) {
    int newDocSetId = onyxClient.createDocumentSet(DEFAULT_DOCUMENT_SET_NAME, "", List.of(ccPairId));
    user.setDocSetId(newDocSetId);
    userRepository.save(user);
    log.info("createFileConnector() - created document set id = {} for user id = {}", newDocSetId, user.getId());
  }

  private void addConnectorToExistingDocumentSet(User user, List<EntityConnectorDto> existingConnectors, int ccPairId) {
    List<Integer> existingCcPairIds = existingConnectors.stream()
        .map(EntityConnectorDto::id)
        .collect(Collectors.toList());
    List<Integer> allCcPairIds = new ArrayList<>(existingCcPairIds);
    allCcPairIds.add(ccPairId);

    onyxClient.getDocumentSetById(user.getDocSetId())
        .ifPresent(docSet -> {
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
        });
  }
}
