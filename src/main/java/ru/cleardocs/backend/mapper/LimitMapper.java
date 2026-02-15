package ru.cleardocs.backend.mapper;

import org.springframework.stereotype.Component;
import ru.cleardocs.backend.dto.LimitDto;
import ru.cleardocs.backend.entity.Limit;

@Component
public class LimitMapper {

  public LimitDto toDto(Limit limit) {
    return new LimitDto(limit.getMaxConnectors());
  }
}
