package ru.cleardocs.backend.service;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cleardocs.backend.client.onyx.OnyxClient;
import ru.cleardocs.backend.dto.ChatResponseDto;
import ru.cleardocs.backend.dto.EntityConnectorDto;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.exception.BadRequestException;
import ru.cleardocs.backend.repository.UserRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChatService {

  private static final String API_KEY_TYPE_BASIC = "basic";

  private final OnyxClient onyxClient;
  private final UserRepository userRepository;

  public ChatService(OnyxClient onyxClient, UserRepository userRepository) {
    this.onyxClient = onyxClient;
    this.userRepository = userRepository;
  }

  @Transactional
  public ChatResponseDto getChatCredentials(User user) {
    log.info("getChatCredentials() - starts with user id = {}, docSetId = {}", user.getId(), user.getDocSetId());

    // 1. Check connectors exist
    if (user.getDocSetId() == null) {
      throw new BadRequestException("No connectors. Add connectors before using chat.");
    }
    List<EntityConnectorDto> connectors = onyxClient.getConnectorsByDocSetId(user.getDocSetId());
    if (connectors.isEmpty()) {
      throw new BadRequestException("No connectors. Add connectors before using chat.");
    }

    // 2. Create API key if missing
    if (user.getApiKey() == null || user.getApiKey().isBlank()) {
      String keyName = "clear-docs-" + (user.getEmail() != null ? user.getEmail().replaceAll("[^a-zA-Z0-9]", "-") : user.getId().toString());
      String newApiKey = onyxClient.createApiKey(keyName, API_KEY_TYPE_BASIC);
      user.setApiKey(newApiKey);
      userRepository.save(user);
      log.info("getChatCredentials() - created API key for user id = {}", user.getId());
    }

    // 3. Create persona if missing
    if (user.getPersonaId() == null) {
      String personaName = "Assistant-" + (user.getName() != null ? user.getName() : user.getId().toString());
      int personaId = onyxClient.createPersonaWithDocumentSet(personaName, user.getDocSetId());
      user.setPersonaId(personaId);
      userRepository.save(user);
      log.info("getChatCredentials() - created persona id = {} for user id = {}", personaId, user.getId());
    }

    log.info("getChatCredentials() - ends with apiKey set, personaId = {}", user.getPersonaId());
    return new ChatResponseDto(user.getApiKey(), user.getPersonaId());
  }

  /**
   * Proxies create-chat-session to Onyx API. Forward the Authorization header as-is.
   * Client sends user's Onyx API key in Authorization header.
   */
  public Map<String, Object> createChatSession(String authorizationHeader, @NotNull Map<String, Object> request) {
    return onyxClient.createChatSession(authorizationHeader, request);
  }

  /**
   * Proxies send-chat-message to Onyx API. Streams response directly to outputStream.
   */
  public void streamSendChatMessage(String authorizationHeader, @NotNull Map<String, Object> request, OutputStream outputStream) throws IOException {
    Object sessionId = request != null ? request.get("chat_session_id") : null;
    long start = System.currentTimeMillis();
    log.debug("streamSendChatMessage sessionId={} start", sessionId);
    try {
      onyxClient.streamSendChatMessage(authorizationHeader, request, outputStream);
      long elapsed = System.currentTimeMillis() - start;
      log.debug("streamSendChatMessage sessionId={} done elapsed_ms={}", sessionId, elapsed);
    } catch (IOException e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error("streamSendChatMessage sessionId={} failed elapsed_ms={} error={}", sessionId, elapsed, e.getMessage());
      throw e;
    }
  }
}
