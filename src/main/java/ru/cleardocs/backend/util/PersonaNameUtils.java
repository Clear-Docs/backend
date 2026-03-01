package ru.cleardocs.backend.util;

import ru.cleardocs.backend.entity.User;

/**
 * Utility for generating persona (assistant) names for Onyx.
 */
public final class PersonaNameUtils {

  private static final String PREFIX = "Assistant-";

  private PersonaNameUtils() {
  }

  /**
   * Generates a persona name for the given user.
   * Format: "Assistant-{identifier}", where identifier is (in order of preference):
   * user's name, email, or id.
   *
   * @param user the user
   * @return persona name
   */
  public static String personaNameFor(User user) {
    String identifier = user.getName() != null && !user.getName().isBlank()
        ? user.getName()
        : user.getEmail() != null && !user.getEmail().isBlank()
            ? user.getEmail()
            : user.getId() != null
                ? user.getId().toString()
                : "Unknown";
    return PREFIX + identifier;
  }
}
