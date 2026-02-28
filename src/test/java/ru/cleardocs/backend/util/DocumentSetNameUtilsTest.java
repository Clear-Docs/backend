package ru.cleardocs.backend.util;

import org.junit.jupiter.api.Test;
import ru.cleardocs.backend.entity.User;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSetNameUtilsTest {

  private static final String PREFIX = "Documents";
  private static final UUID FIXED_UUID = UUID.fromString("19e576e3-94f1-45ba-bfd4-984f33c11d81");

  @Test
  void documentSetNameFor_userWithNameAndEmail_returnsNameWithNameAndEmail() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("ivan@mail.ru")
        .name("Иван Петров")
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals("Documents - Иван Петров (ivan@mail.ru) - 19e576e3", result);
  }

  @Test
  void documentSetNameFor_userWithEmailOnly_nameNull_returnsNameWithEmailOnly() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("butov6101@mail.ru")
        .name(null)
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals("Documents (butov6101@mail.ru) - 19e576e3", result);
  }

  @Test
  void documentSetNameFor_userWithEmailOnly_nameBlank_returnsNameWithEmailOnly() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("test@example.com")
        .name("   ")
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals("Documents (test@example.com) - 19e576e3", result);
  }

  @Test
  void documentSetNameFor_userWithNullEmail_usesUserPlaceholder() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email(null)
        .name("John")
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals("Documents - John (user) - 19e576e3", result);
  }

  @Test
  void documentSetNameFor_userWithBlankEmail_usesUserPlaceholder() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("")
        .name(null)
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals("Documents (user) - 19e576e3", result);
  }

  @Test
  void documentSetNameFor_userWithNullId_usesRandomSuffix() {
    User user = User.builder()
        .id(null)
        .email("test@example.com")
        .name(null)
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertTrue(result.startsWith("Documents (test@example.com) - "));
    assertTrue(result.matches("Documents \\(test@example\\.com\\) - [a-f0-9]{8}"));
  }

  @Test
  void documentSetNameFor_resultExceeds255Chars_truncatesTo255() {
    String longName = "A".repeat(300);
    User user = User.builder()
        .id(FIXED_UUID)
        .email("a@b.ru")
        .name(longName)
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals(255, result.length());
    assertTrue(result.startsWith("Documents - A"));
  }

  @Test
  void documentSetNameFor_resultExactly255Chars_noTruncation() {
    int fixedPartLen = "Documents - ".length() + " (a@b.ru) - 19e576e3".length();
    String name = "N".repeat(255 - fixedPartLen);
    User user = User.builder()
        .id(FIXED_UUID)
        .email("a@b.ru")
        .name(name)
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals(255, result.length());
    assertTrue(result.endsWith("19e576e3"));
  }

  @Test
  void documentSetNameFor_customPrefix_usesPrefix() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("test@mail.ru")
        .name(null)
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor("Docs", user);

    assertEquals("Docs (test@mail.ru) - 19e576e3", result);
  }

  @Test
  void documentSetNameFor_userWithNameContainingSpecialChars_includesInResult() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("user@test.com")
        .name("O'Brien-Smith")
        .build();

    String result = DocumentSetNameUtils.documentSetNameFor(PREFIX, user);

    assertEquals("Documents - O'Brien-Smith (user@test.com) - 19e576e3", result);
  }
}
