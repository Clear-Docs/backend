package ru.cleardocs.backend.util;

import ru.cleardocs.backend.entity.User;

import java.util.UUID;

/**
 * Utility for generating unique, human-readable document set names.
 */
public final class DocumentSetNameUtils {

  private static final int MAX_NAME_LENGTH = 255;

  private DocumentSetNameUtils() {
  }

  /**
   * Generates a unique document set name for the given user.
   * Format: "prefix - name (email) - suffix" or "prefix (email) - suffix" when name is blank.
   *
   * @param prefix default prefix (e.g. "Documents")
   * @param user   the user
   * @return unique name, truncated to 255 chars if needed
   */
  public static String documentSetNameFor(String prefix, User user) {
    String email = (user.getEmail() != null && !user.getEmail().isBlank())
        ? user.getEmail()
        : "user";
    String suffix = user.getId() != null
        ? user.getId().toString().substring(0, 8)
        : UUID.randomUUID().toString().substring(0, 8);
    String name;
    if (user.getName() != null && !user.getName().isBlank()) {
      name = prefix + " - " + user.getName() + " (" + email + ") - " + suffix;
    } else {
      name = prefix + " (" + email + ") - " + suffix;
    }
    return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
  }
}
