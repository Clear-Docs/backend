package ru.cleardocs.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
  }

  /**
   * RestTemplate for Onyx streaming (send-chat-message). Extended response timeout
   * (10 min) prevents truncation when LLM pauses during generation.
   */
  @Bean("onyxStreamingRestTemplate")
  public RestTemplate onyxStreamingRestTemplate() {
    RequestConfig config = RequestConfig.custom()
        .setResponseTimeout(Timeout.ofMinutes(10))
        .build();
    var httpClient = HttpClientBuilder.create()
        .setDefaultRequestConfig(config)
        .build();
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
  }

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
