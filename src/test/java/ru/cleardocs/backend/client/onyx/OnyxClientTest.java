package ru.cleardocs.backend.client.onyx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ActiveProfiles("test")
class OnyxClientTest {

  @Autowired
  OnyxClient onyxClient;

  @Autowired
  RestTemplate restTemplate;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void getCcPairStatus_whenOnyxReturnsNullStatus_throwsIllegalStateException() {
    String responseBody = """
        [{"indexing_statuses":[{"cc_pair_id":123,"cc_pair_status":null}]}]
        """;

    mockServer.expect(requestTo(containsString("indexing-status")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
        onyxClient.getCcPairStatus(123)
    );

    assertTrue(thrown.getMessage().contains("Onyx returned null cc_pair_status for cc_pair_id=123"));
  }

  @Test
  void streamSendChatMessage_writesFullStreamWithFlush() throws Exception {
    String ssePayload = "data: {\"type\":\"message_delta\",\"delta\":\"Hello\"}\n\n"
        + "data: {\"type\":\"message_delta\",\"delta\":\" world\"}\n\n"
        + "data: {\"type\":\"message_end\"}\n\n";

    mockServer.expect(requestTo(containsString("send-chat-message")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(ssePayload, MediaType.TEXT_EVENT_STREAM));

    var out = new ByteArrayOutputStream();
    var request = Map.<String, Object>of("message", "hi", "chat_session_id", "sess-1");
    onyxClient.streamSendChatMessage("Bearer key", request, out);

    String result = out.toString();
    assertTrue(result.contains("\"delta\":\"Hello\""), "Should contain first delta");
    assertTrue(result.contains("\"delta\":\" world\""), "Should contain second delta");
    assertTrue(result.contains("\"type\":\"message_end\""), "Should contain end — full stream delivered");

    mockServer.verify();
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    RestTemplate restTemplate() {
      return new RestTemplate();
    }

    @Bean("onyxStreamingRestTemplate")
    RestTemplate onyxStreamingRestTemplate(RestTemplate restTemplate) {
      return restTemplate;  // same instance — MockRestServiceServer intercepts streamSendChatMessage
    }
  }
}
