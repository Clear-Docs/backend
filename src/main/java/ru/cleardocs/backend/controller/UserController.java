package ru.cleardocs.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.dto.GetMeDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<GetMeDto> getMe(@AuthenticationPrincipal User user) {
    log.info("getMe() - starts with user id = {}", user.getId());
    GetMeDto response = userService.getMe(user);
    log.info("getMe() - ends with response = {}", response);
    return ResponseEntity.ok(response);
  }
}
