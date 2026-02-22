package ru.cleardocs.backend.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockFirebaseUserSecurityContextFactory.class)
public @interface WithMockFirebaseUser {

  String email() default "test@example.com";

  String name() default "Test User";

  String planCode() default "FREE";
}
