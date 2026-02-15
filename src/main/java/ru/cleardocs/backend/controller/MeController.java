package ru.cleardocs.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class MeController {

  @GetMapping("/me")
  public ResponseEntity<String> me() {
    log.info("me() - unauthenticated request, returning 401");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Auth required");
  }
}
