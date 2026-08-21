package me.xap3y.space.repository;

import me.xap3y.space.entity.ResourceLimitRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResourceLimitRuleRepository extends JpaRepository<ResourceLimitRule, Long> {
    List<ResourceLimitRule> findByProfileId(Long profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ResourceLimitRule rule WHERE rule.profile.id = :profileId")
    void deleteByProfileId(@Param("profileId") Long profileId);
}
