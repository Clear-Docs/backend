package ru.cleardocs.backend.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.entity.Limit;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;

import java.util.UUID;

public class WithMockFirebaseUserSecurityContextFactory implements WithSecurityContextFactory<WithMockFirebaseUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockFirebaseUser annotation) {
    PlanCode planCode;
    try {
      planCode = PlanCode.valueOf(annotation.planCode());
    } catch (IllegalArgumentException e) {
      planCode = PlanCode.FREE;
    }

    Limit limit = Limit.builder()
        .id(UUID.randomUUID())
        .maxConnectors(planCode == PlanCode.FREE ? 1 : 100)
        .build();

    Plan plan = Plan.builder()
        .id(UUID.randomUUID())
        .code(planCode)
        .title(planCode.name())
        .priceRub(planCode == PlanCode.FREE ? 0 : 990)
        .periodDays(planCode == PlanCode.FREE ? 0 : 30)
        .limit(limit)
        .build();

    User user = User.builder()
        .id(UUID.randomUUID())
        .firebaseUid("test-uid")
        .email(annotation.email())
        .name(annotation.name())
        .plan(plan)
        .docSetId(annotation.docSetId() > 0 ? annotation.docSetId() : null)
        .build();

    var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
        user, null, java.util.Collections.emptyList());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }
}
