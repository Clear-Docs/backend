package ru.cleardocs.backend.service;

import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Service
public class UserService {

  private final PlanService planService;
  private final UserRepository userRepository;

  public UserService(PlanService planService, UserRepository userRepository) {
    this.planService = planService;
    this.userRepository = userRepository;
  }

  @Transactional
  public User getByToken(FirebaseToken token) {
    log.info("getUser() - starts");
    Optional<User> userOptional = userRepository.findByFirebaseUid(token.getUid());
    User user;
    if (userOptional.isEmpty()) {
      log.info("getUser() - user is not found");
      user = register(token);
    } else {
      user = userOptional.get();
    }
    log.info("getUser() - ends with user = {}", user);
    return user;
  }

  private User register(FirebaseToken token) {
    log.info("registerUser() - starts");
    Plan plan = planService.getByCode(PlanCode.FREE);
    User newUser = userRepository.save(User.builder()
        .firebaseUid(token.getUid())
        .email(token.getEmail())
        .name(token.getName())
        .plan(plan)
        .build());
    log.info("registerUser() - ends with user id = {}", newUser.getId());
    return newUser;
  }
}
