package ru.cleardocs.backend.util;

import ru.cleardocs.backend.entity.User;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility for generating unique, human-readable names (document sets, connectors).
 */
public final class DocumentSetNameUtils {

  private static final int MAX_NAME_LENGTH = 255;

  private DocumentSetNameUtils() {
  }

  /**
   * Returns a unique name. If baseName is not in existingNames, returns it as-is.
   * Otherwise adds a postfix " - {suffix}" and iterates with "-2", "-3" etc. until unique.
   *
   * @param baseName      requested name
   * @param existingNames names already in use
   * @param suffix        suffix to add when conflict (e.g. first 8 chars of userId)
   * @return unique name
   */
  public static String ensureUniqueName(String baseName, Collection<String> existingNames, String suffix) {
    Set<String> taken = existingNames.stream()
        .filter(n -> n != null && !n.isBlank())
        .collect(Collectors.toSet());
    if (!taken.contains(baseName)) {
      return baseName;
    }
    String candidate = baseName + " - " + suffix;
    if (!taken.contains(candidate)) {
      return candidate;
    }
    int n = 2;
    while (taken.contains(candidate + "-" + n)) {
      n++;
    }
    return candidate + "-" + n;
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
