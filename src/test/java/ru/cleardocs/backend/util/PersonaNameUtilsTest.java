package ru.cleardocs.backend.util;

import org.junit.jupiter.api.Test;
import ru.cleardocs.backend.entity.User;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonaNameUtilsTest {

  private static final UUID FIXED_UUID = UUID.fromString("19e576e3-94f1-45ba-bfd4-984f33c11d81");

  @Test
  void personaNameFor_userWithName_usesName() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("ivan@mail.ru")
        .name("Иван Петров")
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-Иван Петров", result);
  }

  @Test
  void personaNameFor_userWithNameAndEmail_usesName() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("test@example.com")
        .name("John Doe")
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-John Doe", result);
  }

  @Test
  void personaNameFor_userWithEmailOnly_usesEmail() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("butov6101@mail.ru")
        .name(null)
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-butov6101@mail.ru", result);
  }

  @Test
  void personaNameFor_userWithBlankName_usesEmail() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("test@example.com")
        .name("   ")
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-test@example.com", result);
  }

  @Test
  void personaNameFor_userWithEmptyName_usesEmail() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("user@test.com")
        .name("")
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-user@test.com", result);
  }

  @Test
  void personaNameFor_userWithIdOnly_usesId() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email(null)
        .name(null)
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-19e576e3-94f1-45ba-bfd4-984f33c11d81", result);
  }

  @Test
  void personaNameFor_userWithBlankEmail_usesId() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("")
        .name(null)
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-19e576e3-94f1-45ba-bfd4-984f33c11d81", result);
  }

  @Test
  void personaNameFor_userWithNullEmail_usesId() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email(null)
        .name(null)
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-19e576e3-94f1-45ba-bfd4-984f33c11d81", result);
  }

  @Test
  void personaNameFor_userWithNameContainingSpecialChars_includesAsIs() {
    User user = User.builder()
        .id(FIXED_UUID)
        .email("user@test.com")
        .name("O'Brien-Smith")
        .build();

    String result = PersonaNameUtils.personaNameFor(user);

    assertEquals("Assistant-O'Brien-Smith", result);
  }
}
