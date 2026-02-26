package ru.cleardocs.backend.controller;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import ru.cleardocs.backend.config.TestFirebaseConfig;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestFirebaseConfig.class)
@ActiveProfiles("test")
class ChatControllerTest {

  private static final String SSE_PAYLOAD = "data: {\"type\":\"message_delta\",\"delta\":\"Hello\"}\n\n";
  private static final String SSE_PAYLOAD_WITH_END = "data: {\"type\":\"message_delta\",\"delta\":\"Hello\"}\n\n"
      + "data: {\"type\":\"message_delta\",\"delta\":\" world\"}\n\n"
      + "data: {\"type\":\"message_end\"}\n\n";

  @Autowired
  MockMvc mockMvc;

  @Autowired
  RestTemplate restTemplate;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void sendChatMessage_proxiesSseStreamWithoutSerializingDataBuffer() throws Exception {
    mockServer.expect(requestTo(containsString("send-chat-message")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(SSE_PAYLOAD, MediaType.TEXT_EVENT_STREAM));

    var mvcResult = mockMvc.perform(post("/api/v1/chat/send-chat-message")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer onyx-api-key")
            .content("{\"message\":\"hi\",\"chat_session_id\":\"test-session\"}"))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM))
        .andExpect(content().string(containsString("data:")))
        .andExpect(content().string(containsString("\"type\":\"message_delta\"")))
        .andExpect(content().string(containsString("\"delta\":\"Hello\"")))
        .andExpect(content().string(not(containsString("allocated"))))
        .andExpect(content().string(not(containsString("nativeBuffer"))));

    mockServer.verify();
  }

  @Test
  void sendChatMessage_deliversFullStreamIncludingEnd() throws Exception {
    mockServer.expect(requestTo(containsString("send-chat-message")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(SSE_PAYLOAD_WITH_END, MediaType.TEXT_EVENT_STREAM));

    var mvcResult = mockMvc.perform(post("/api/v1/chat/send-chat-message")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer onyx-api-key")
            .content("{\"message\":\"hi\",\"chat_session_id\":\"sess-123\"}"))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM))
        .andExpect(content().string(containsString("\"delta\":\"Hello\"")))
        .andExpect(content().string(containsString("\"delta\":\" world\"")))
        .andExpect(content().string(containsString("\"type\":\"message_end\"")));

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
      return restTemplate;  // same instance — MockRestServiceServer intercepts send-chat-message
    }
  }
}
