package me.xap3y.space.mapper;

import me.xap3y.space.dto.AuditLogDto;
import me.xap3y.space.entity.AuditLog;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class AuditLogMapper implements Function<AuditLog, AuditLogDto> {

    private final ShortUserMapper shortUserMapper;

    public AuditLogMapper(ShortUserMapper shortUserMapper) {
        this.shortUserMapper = shortUserMapper;
    }

    @Override
    public AuditLogDto apply(AuditLog auditLog) {
        return new AuditLogDto(
                auditLog.getId(),
                auditLog.getUserId() != null ? shortUserMapper.apply(auditLog.getUserId(), false) : null,
                auditLog.getType(),
                auditLog.getDescription(),
                auditLog.getSource(),
                auditLog.getTime()
        );
    }
}
