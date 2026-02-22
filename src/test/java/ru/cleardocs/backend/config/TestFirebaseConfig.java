package ru.cleardocs.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.cleardocs.backend.filter.FirebaseTokenFilter;
import ru.cleardocs.backend.service.UserService;

import java.io.IOException;

/**
 * Test replacement for FirebaseTokenFilter that passes all requests through without Firebase verification.
 */
@TestConfiguration
public class TestFirebaseConfig {

  @Bean
  @Primary
  public FirebaseTokenFilter firebaseTokenFilter(UserService userService) {
    return new PassThroughFirebaseTokenFilter(userService);
  }

  static class PassThroughFirebaseTokenFilter extends FirebaseTokenFilter {

    public PassThroughFirebaseTokenFilter(UserService userService) {
      super(userService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      filterChain.doFilter(request, response);
    }
  }
}
