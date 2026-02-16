package ru.cleardocs.backend.mapper;

import org.springframework.stereotype.Component;
import ru.cleardocs.backend.dto.UserDto;
import ru.cleardocs.backend.entity.User;

@Component
public class UserMapper {

  private final PlanMapper planMapper;

  public UserMapper(PlanMapper planMapper) {
    this.planMapper = planMapper;
  }

  public UserDto toDto(User user) {
    return new UserDto(user.getEmail(), user.getName(), planMapper.toDto(user.getPlan()));
  }
}
