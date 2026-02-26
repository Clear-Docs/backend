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
    int messageLength = msg != null ? msg.toString().length() : 0;
    log.info("sendChatMessage request sessionId={} messageLength={}", sessionId, messageLength);
    StreamingResponseBody streamBody = outputStream -> {
      long start = System.currentTimeMillis();
      try {
        chatService.streamSendChatMessage(request.getHeader("Authorization"), body, outputStream);
      } catch (Exception e) {
        long elapsed = System.currentTimeMillis() - start;
        log.warn("sendChatMessage failed sessionId={} elapsed_ms={} error={}", sessionId, elapsed, e.getMessage());
        throw e;
      } finally {
        outputStream.flush();  // Always flush, even on exception — ensures last chunk reaches client
      }
      long elapsed = System.currentTimeMillis() - start;
      log.info("sendChatMessage completed sessionId={} elapsed_ms={}", sessionId, elapsed);
    };
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(streamBody);
  }
}
