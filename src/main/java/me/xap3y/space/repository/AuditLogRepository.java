package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.AuditLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    long count();

    @Transactional
    void deleteById(@NotNull Long id);

    List<AuditLog> findAllByUserId_Id(@NotNull Long userId);

    @NotNull List<AuditLog> findAll();

    List<AuditLog> findByTimeIsBetween(LocalDateTime timeAfter, LocalDateTime timeBefore);

    List<AuditLog> findByTimeIsBetweenAndUserId_Id(LocalDateTime timeAfter, LocalDateTime timeBefore, Long userIdId);
}
