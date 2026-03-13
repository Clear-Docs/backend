package ru.cleardocs.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cleardocs.backend.constant.PlanCode;
import ru.cleardocs.backend.entity.Plan;
import ru.cleardocs.backend.entity.User;
import ru.cleardocs.backend.repository.PlanRepository;
import ru.cleardocs.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Раз в сутки переводит на FREE пользователей, у которых истёк оплаченный период:
 * тариф считаем активным, если был успешный платёж за последние periodDays дней (по данным из payments).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanExpiryService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    @Scheduled(cron = "${plan-expiry.cron:0 0 3 * * *}")
    @Transactional
    public void downgradeExpiredPlans() {
        log.info("Plan expiry: job started");
        Plan freePlan = planRepository.findByCode(PlanCode.FREE).orElse(null);
        if (freePlan == null) {
            log.warn("Plan expiry: FREE plan not found, skipping");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Plan> paidPlans = planRepository.findAll().stream()
                .filter(p -> p.getCode() != PlanCode.FREE)
                .toList();
        int totalDowngraded = 0;
        for (Plan plan : paidPlans) {
            LocalDateTime cutoff = now.minusDays(plan.getPeriodDays());
            List<User> expired = userRepository.findUsersWithExpiredPlan(plan, cutoff);
            for (User user : expired) {
                user.setPlan(freePlan);
                user.setTochkaSubscriptionOperationId(null);
                userRepository.save(user);
                log.info("Plan expiry: user {} downgraded to FREE (last SUCCESS payment for {} was before {})",
                        user.getId(), plan.getCode(), cutoff);
                totalDowngraded++;
            }
        }
        log.info("Plan expiry: job finished, downgraded {} user(s) to FREE", totalDowngraded);
    }
}
