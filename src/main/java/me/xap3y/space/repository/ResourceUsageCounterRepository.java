package me.xap3y.space.repository;

import me.xap3y.space.api.enums.ResourceLimitType;
import me.xap3y.space.entity.ResourceUsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ResourceUsageCounterRepository extends JpaRepository<ResourceUsageCounter, Long> {
    Optional<ResourceUsageCounter> findByUserIdAndResourceTypeAndUsageDate(Long userId,
                                                                           ResourceLimitType resourceType,
                                                                           LocalDate usageDate);
}
