package ru.cleardocs.backend.service;

import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.dto.GetMeDto;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.mapper.UserMapper;
import ru.cleardocs.backend.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Service
public class UserService {

  private final UserMapper userMapper;
  private final PlanService planService;
  private final UserRepository userRepository;

  public UserService(UserMapper userMapper, PlanService planService, UserRepository userRepository) {
    this.userMapper = userMapper;
    this.planService = planService;
    this.userRepository = userRepository;
  }

  @Transactional
  public GetMeDto getMe(User user) {
    log.info("getMe() - starts with user id = {}", user.getId());
    // Document set is created lazily when user uploads first file via ConnectorService.createFileConnector()
    // Onyx API does not allow creating document sets with no connectors
    GetMeDto response = new GetMeDto(userMapper.toDto(user));
    log.info("getMe() - ends");
    return response;
  }

  @Transactional
  public User getByToken(FirebaseToken token) {
    log.info("getUser() - starts");
    Optional<User> userOptional = userRepository.findByFirebaseUid(token.getUid());
    User user;
    if (userOptional.isEmpty()) {
      log.info("getUser() - user is not found");
      try {
        user = register(token);
      } catch (DataIntegrityViolationException e) {
        log.warn("getUser() - duplicate key on register, fetching existing user firebase_uid={}", token.getUid());
        user = userRepository.findByFirebaseUid(token.getUid())
            .orElseThrow(() -> new IllegalStateException("User creation failed due to race condition", e));
      }
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
