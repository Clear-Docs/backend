package ru.cleardocs.backend.service;

import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cleardocs.backend.client.onyx.OnyxClient;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.dto.GetMeDto;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.mapper.UserMapper;
import ru.cleardocs.backend.repository.UserRepository;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

  private static final String DEFAULT_DOCUMENT_SET_NAME = "Documents";

  private final UserMapper userMapper;
  private final PlanService planService;
  private final UserRepository userRepository;
  private final OnyxClient onyxClient;

  public UserService(UserMapper userMapper, PlanService planService, UserRepository userRepository, OnyxClient onyxClient) {
    this.userMapper = userMapper;
    this.planService = planService;
    this.userRepository = userRepository;
    this.onyxClient = onyxClient;
  }

  @Transactional
  public GetMeDto getMe(User user) {
    log.info("getMe() - starts with user id = {}", user.getId());
    if (user.getDocSetId() == null) {
      ensureDocumentSetExists(user);
    }
    GetMeDto response = new GetMeDto(userMapper.toDto(user));
    log.info("getMe() - ends");
    return response;
  }

  private void ensureDocumentSetExists(User user) {
    int newDocSetId = onyxClient.createDocumentSet(DEFAULT_DOCUMENT_SET_NAME, "", Collections.emptyList());
    user.setDocSetId(newDocSetId);
    userRepository.save(user);
    log.info("getMe() - created document set id = {} for user id = {}", newDocSetId, user.getId());
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
