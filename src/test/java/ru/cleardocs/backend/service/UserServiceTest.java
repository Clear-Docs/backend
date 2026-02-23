package ru.cleardocs.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.dto.GetMeDto;
import ru.cleardocs.backend.dto.LimitDto;
import ru.cleardocs.backend.dto.PlanDto;
import ru.cleardocs.backend.dto.UserDto;
import ru.cleardocs.backend.entity.Limit;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.mapper.UserMapper;
import ru.cleardocs.backend.repository.UserRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  UserMapper userMapper;

  @Mock
  PlanService planService;

  @Mock
  UserRepository userRepository;

  @InjectMocks
  UserService userService;

  @Test
  void getMe_userWithoutDocSet_returnsWithNullDocSetId() {
    UUID userId = UUID.randomUUID();
    Plan plan = planWithLimit();
    User user = User.builder()
        .id(userId)
        .email("test@example.com")
        .name("Test User")
        .plan(plan)
        .docSetId(null)
        .build();

    when(userMapper.toDto(any())).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      return new UserDto(u.getEmail(), u.getName(), planToDto(u.getPlan()), u.getDocSetId());
    });

    GetMeDto result = userService.getMe(user);

    assertNotNull(result);
    assertNull(result.user().docSetId());
    verify(userRepository, never()).save(any());
  }

  @Test
  void getMe_userWithDocSet_returnsDocSetId() {
    Plan plan = planWithLimit();
    User user = User.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .name("Test User")
        .plan(plan)
        .docSetId(42)
        .build();

    when(userMapper.toDto(any())).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      return new UserDto(u.getEmail(), u.getName(), planToDto(u.getPlan()), u.getDocSetId());
    });

    GetMeDto result = userService.getMe(user);

    assertNotNull(result);
    assertEquals(42, result.user().docSetId());
    verify(userRepository, never()).save(any());
  }

  private Plan planWithLimit() {
    Limit limit = Limit.builder().id(UUID.randomUUID()).maxConnectors(1).build();
    return Plan.builder()
        .id(UUID.randomUUID())
        .code(PlanCode.FREE)
        .title("Free")
        .priceRub(0)
        .periodDays(30)
        .limit(limit)
        .build();
  }

  private PlanDto planToDto(Plan plan) {
    if (plan == null) return null;
    LimitDto limitDto = plan.getLimit() != null ? new LimitDto(plan.getLimit().getMaxConnectors()) : new LimitDto(0);
    return new PlanDto(plan.getCode(), plan.getTitle(), plan.getPriceRub(), plan.getPeriodDays(), limitDto);
  }
}
