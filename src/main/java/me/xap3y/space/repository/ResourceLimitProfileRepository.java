package me.xap3y.space.repository;

import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.entity.ResourceLimitProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceLimitProfileRepository extends JpaRepository<ResourceLimitProfile, Long> {
    Optional<ResourceLimitProfile> findByRole(UserRole role);
    Optional<ResourceLimitProfile> findByUserId(Long userId);
    List<ResourceLimitProfile> findAllByUserIsNotNull();
}
