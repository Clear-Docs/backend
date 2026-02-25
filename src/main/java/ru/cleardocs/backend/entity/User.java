package ru.cleardocs.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue
  private UUID id;

  private String firebaseUid;
  private String email;
  private String name;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @ManyToOne
  @JoinColumn(name = "plan_id")
  private Plan plan;

  @Column(name = "doc_set_id")
  private Integer docSetId;

  @Column(name = "api_key")
  private String apiKey;

  @Column(name = "persona_id")
  private Integer personaId;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
