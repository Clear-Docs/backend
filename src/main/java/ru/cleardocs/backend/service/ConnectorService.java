package ru.cleardocs.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cleardocs.backend.client.onyx.OnyxClient;
import ru.cleardocs.backend.dto.EntityConnectorDto;
import ru.cleardocs.backend.dto.GetConnectorsDto;
import ru.cleardocs.backend.entity.User;

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
}
