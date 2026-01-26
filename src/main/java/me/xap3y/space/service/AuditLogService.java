package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.entity.AuditLog;
import me.xap3y.space.entity.User;
import me.xap3y.space.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public long countAllLogs() {
        return auditLogRepository.count();
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> getLogsByUserId(Long userId) {
        return auditLogRepository.findAllByUserId_Id(userId);
    }

    public List<AuditLog> getLogsInBetween(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimeIsBetween(start, end);
    }

    public List<AuditLog> getLogsInBetweenAndUserId(LocalDateTime start, LocalDateTime end, Long id) {
        return auditLogRepository.findByTimeIsBetweenAndUserId_Id(start, end, id);
    }

    public void deleteLogById(Long id) {
        auditLogRepository.deleteById(id);
    }

    public AuditLog saveLog(AuditLog log) {
        return auditLogRepository.save(log);
    }

    public AuditLog saveLog(PortalLogType type) {
        return this.saveLog(type, null, null, null);
    }

    public AuditLog saveLog(PortalLogType type, User user) {
        return this.saveLog(type, user, null, null);
    }

    public AuditLog saveLog(PortalLogType type, User user, String description, String source) {
        AuditLog log = new AuditLog();
        log.setType(type);
        log.setUserId(user);
        log.setDescription(description);
        log.setSource(source);
        log.setTime(LocalDateTime.now());
        return auditLogRepository.save(log);
    }
}
