package ru.cleardocs.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.dto.GetConnectorsDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.service.ConnectorService;

@Slf4j
@RestController
@RequestMapping("/api/v1/connectors")
public class ConnectorController {

  private final ConnectorService connectorService;

  public ConnectorController(ConnectorService connectorService) {
    this.connectorService = connectorService;
  }

  @GetMapping
  public ResponseEntity<GetConnectorsDto> getConnectors(@AuthenticationPrincipal User user) {
    log.info("getConnectors() - starts with user id = {}", user.getId());
    GetConnectorsDto response = connectorService.getConnectors(user);
    return ResponseEntity.ok(response);
  }
}
