package ru.cleardocs.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.cleardocs.backend.filter.FirebaseTokenFilter;
import ru.cleardocs.backend.service.UserService;

import java.io.IOException;

/**
 * Local profile: заменяет FirebaseTokenFilter на pass-through фильтр,
 * чтобы запускать приложение без настроенного Firebase.
 */
@Configuration
@Profile("local")
public class LocalFirebaseConfig {

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
