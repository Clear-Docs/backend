package ru.cleardocs.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.cleardocs.backend.client.onyx.OnyxClient;
import ru.cleardocs.backend.client.onyx.OnyxCreateConnectorResponseDto;
import ru.cleardocs.backend.client.onyx.OnyxFileUploadResponseDto;
import ru.cleardocs.backend.dto.CreateConnectorResponseDto;
import ru.cleardocs.backend.dto.EntityConnectorDto;
import ru.cleardocs.backend.dto.GetConnectorsDto;
import ru.cleardocs.backend.entity.Limit;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.BadRequestException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ConnectorService {

  private final OnyxClient onyxClient;

  public ConnectorService(OnyxClient onyxClient) {
    this.onyxClient = onyxClient;
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

  public CreateConnectorResponseDto createFileConnector(User user, String name, MultipartFile[] files) throws IOException {
    log.info("createFileConnector() - starts with user id = {}, docSetId = {}, name = {}",
        user.getId(), user.getDocSetId(), name);

    if (user.getDocSetId() == null) {
      throw new BadRequestException("User has no document set. Cannot create connector.");
    }

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

    List<EntityConnectorDto> existingConnectors = onyxClient.getConnectorsByDocSetId(user.getDocSetId());
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

    log.info("createFileConnector() - ends with cc_pair_id = {}", createResponse.data());
    return new CreateConnectorResponseDto(createResponse.data(), name, "file");
  }
}
