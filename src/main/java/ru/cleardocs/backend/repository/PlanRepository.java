package ru.cleardocs.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.cleardocs.backend.entity.Plan;

import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
}
