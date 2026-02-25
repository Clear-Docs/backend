package ru.cleardocs.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.dto.ChatResponseDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.service.ChatService;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ChatController {

  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @GetMapping("/chat")
  public ResponseEntity<ChatResponseDto> chat(@AuthenticationPrincipal User user) {
    log.info("chat() - starts with user id = {}", user.getId());
    ChatResponseDto response = chatService.getChatCredentials(user);
    return ResponseEntity.ok(response);
  }
}
