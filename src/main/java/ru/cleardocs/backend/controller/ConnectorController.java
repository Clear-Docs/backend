package ru.cleardocs.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.cleardocs.backend.dto.CreateConnectorResponseDto;
import ru.cleardocs.backend.dto.CreateUrlConnectorRequestDto;
import ru.cleardocs.backend.dto.GetConnectorsDto;
import ru.cleardocs.backend.dto.UpdateConnectorRequestDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.BadRequestException;
import ru.cleardocs.backend.service.ConnectorService;

import java.io.IOException;

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

  @PostMapping("/url")
  public ResponseEntity<CreateConnectorResponseDto> createUrlConnector(
      @AuthenticationPrincipal User user,
      @RequestBody CreateUrlConnectorRequestDto body
  ) {
    log.info("createUrlConnector() - starts with user id = {}, name = {}, url = {}",
        user.getId(), body != null ? body.name() : null, body != null ? body.url() : null);
    if (body == null || body.name() == null || body.url() == null) {
      throw new BadRequestException("name and url are required");
    }
    CreateConnectorResponseDto response = connectorService.createUrlConnector(user, body.name(), body.url());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping
  public ResponseEntity<CreateConnectorResponseDto> createFileConnector(
      @AuthenticationPrincipal User user,
      @RequestParam("name") String name,
      @RequestParam("files") MultipartFile[] files
  ) throws IOException {
    log.info("createFileConnector() - starts with user id = {}, name = {}", user.getId(), name);
    CreateConnectorResponseDto response = connectorService.createFileConnector(user, name, files);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{connectorId}")
  public ResponseEntity<Void> updateConnector(
      @AuthenticationPrincipal User user,
      @PathVariable int connectorId,
      @RequestBody UpdateConnectorRequestDto body
  ) {
    log.info("updateConnector() - starts with user id = {}, connectorId = {}, body = {}", user.getId(), connectorId, body);
    connectorService.updateConnector(user, connectorId, body);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{connectorId}")
  public ResponseEntity<Void> deleteConnector(
      @AuthenticationPrincipal User user,
      @PathVariable int connectorId
  ) {
    log.info("deleteConnector() - starts with user id = {}, connectorId = {}", user.getId(), connectorId);
    connectorService.deleteConnector(user, connectorId);
    return ResponseEntity.noContent().build();
  }
}
