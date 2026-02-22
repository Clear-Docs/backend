package ru.cleardocs.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Profile("!test & !local")
public class FirebaseConfig {

  @Value("${firebase.config.path}")
  private String firebaseConfigPath;

  @Bean
  public FirebaseApp firebaseApp() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      try (InputStream inputStream = new FileInputStream(firebaseConfigPath)) {
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(inputStream))
            .build();
        return FirebaseApp.initializeApp(options);
      }
    }
    return FirebaseApp.getInstance();
  }
}
