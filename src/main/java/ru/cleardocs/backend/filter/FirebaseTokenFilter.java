package ru.cleardocs.backend.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.service.UserService;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

  private final UserService userService;

  public FirebaseTokenFilter(UserService userService) {
    this.userService = userService;
  }

  private static final String PATH_CHAT_CREATE_SESSION = "/api/v1/chat/create-chat-session";
  private static final String PATH_CHAT_SEND_MESSAGE = "/api/v1/chat/send-chat-message";

  @Override
  protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
    String path = request.getRequestURI();
    if (PATH_CHAT_CREATE_SESSION.equals(path) || PATH_CHAT_SEND_MESSAGE.equals(path)) {
      filterChain.doFilter(request, response);
      return;
    }
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        User user = userService.getByToken(decodedToken);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (FirebaseAuthException e) {
        log.error("doFilterInternal() - exception with message = {}", e.getMessage());
      }
    }
    filterChain.doFilter(request, response);
  }
}
