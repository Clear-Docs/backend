package ru.cleardocs.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByFirebaseUid(String firebaseUid);

  /**
   * Пользователи с данным планом, у которых последний успешный платёж по этому плану был раньше cutoff.
   * Тариф активен, если был платёж за последние periodDays дней; иначе считаем период истёкшим.
   */
  @Query("SELECT u FROM User u WHERE u.plan = :plan AND (SELECT MAX(p.createdAt) FROM Payment p WHERE p.user = u AND p.paymentStatus = ru.cleardocs.backend.constant.PaymentStatus.SUCCESS AND p.plan = :plan) < :cutoff")
  List<User> findUsersWithExpiredPlan(@Param("plan") Plan plan, @Param("cutoff") LocalDateTime cutoff);
}
