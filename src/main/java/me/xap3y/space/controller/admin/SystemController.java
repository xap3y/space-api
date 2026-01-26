package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.AuditLogDto;
import me.xap3y.space.mapper.AuditLogMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.AuditLogService;
import me.xap3y.space.service.SystemMetricsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/admin/system")
public class SystemController {

    private final SystemMetricsService systemMetricsService;
    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

    public SystemController(SystemMetricsService systemMetricsService, AuditLogService auditLogService, AuditLogMapper auditLogMapper) {
        this.systemMetricsService = systemMetricsService;
        this.auditLogService = auditLogService;
        this.auditLogMapper = auditLogMapper;
    }

    @GetMapping(
            value = "/metrics",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getSystemMetrics(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(new DefaultResponse(false, systemMetricsService.getAllMetrics()));
    }

    @GetMapping(
            value = "/auditlog",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getAuditLog(
            HttpServletRequest request
    ) {
        List<AuditLogDto> logs = auditLogService.getAllLogs()
                .stream()
                .map(auditLogMapper)
                .toList();

        return ResponseEntity.ok(new DefaultResponse(false, logs, logs.size()));
    }
}
