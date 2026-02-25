package ru.cleardocs.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cleardocs.backend.dto.ChatResponseDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.service.ChatService;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

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

  @PostMapping("/chat/create-chat-session")
  public ResponseEntity<?> createChatSession(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    log.info("createChatSession() - proxying to Onyx");
    Object response = chatService.createChatSession(
        request.getHeader("Authorization"),
        body);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/chat/send-chat-message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> sendChatMessage(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    Object msg = body != null ? body.get("message") : null;
    Object sessionId = body != null ? body.get("chat_session_id") : null;
    log.info("sendChatMessage() - request received message={} chat_session_id={}", msg, sessionId);
    StreamingResponseBody streamBody = outputStream -> {
      long start = System.currentTimeMillis();
      log.info("sendChatMessage() - StreamingResponseBody.writeTo started");
      try {
        chatService.streamSendChatMessage(request.getHeader("Authorization"), body, outputStream);
        outputStream.flush();
        log.info("sendChatMessage() - StreamingResponseBody.writeTo completed in {}ms", System.currentTimeMillis() - start);
      } catch (Exception e) {
        log.error("sendChatMessage() - stream error after {}ms: {} (client may have disconnected)", System.currentTimeMillis() - start, e.getMessage());
        throw e;
      }
    };
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(streamBody);
  }
}
