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
@Table(name = "plans")
public class Plan {

  @Id
  @GeneratedValue
  private UUID id;

  private String code;
  private String title;
  private int priceRub;
  private int periodDays;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @OneToOne
  @JoinColumn(name = "limit_id")
  private Limit limit;

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
