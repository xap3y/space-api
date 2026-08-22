package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.ResourceLimitDtos.PauseRequest;
import me.xap3y.space.dto.ResourceLimitDtos.PolicyUpdate;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.entity.User;
import me.xap3y.space.service.AuditLogService;
import me.xap3y.space.service.ResourceLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/limits")
@RequiredArgsConstructor
public class ResourceLimitController {

    private final ResourceLimitService resourceLimitService;
    private final AuditLogService auditLogService;

    @GetMapping("/roles")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.getRolePolicies()));
    }

    @PutMapping("/roles/{role}")
    @RequiresSpecialApiKey
    public ResponseEntity<?> updateRole(HttpServletRequest request, @PathVariable UserRole role, @RequestBody PolicyUpdate update) {
        Object result = resourceLimitService.updateRolePolicy(role, update);
        auditLogService.saveLog(PortalLogType.RESOURCE_LIMIT_ROLE_CHANGE, actor(request), role.name(), "ADMIN");
        return ResponseEntity.ok(new DefaultResponse(false, result));
    }

    @GetMapping("/users/{uid}")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUser(@PathVariable Long uid) {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.getUserPolicy(uid)));
    }

    @PutMapping("/users/{uid}")
    @RequiresSpecialApiKey
    public ResponseEntity<?> updateUser(HttpServletRequest request, @PathVariable Long uid, @RequestBody PolicyUpdate update) {
        Object result = resourceLimitService.updateUserPolicy(uid, update);
        auditLogService.saveLog(PortalLogType.RESOURCE_LIMIT_USER_OVERRIDE_CHANGE, actor(request), "User #" + uid, "ADMIN");
        return ResponseEntity.ok(new DefaultResponse(false, result));
    }

    @DeleteMapping("/users/{uid}/overrides")
    @RequiresSpecialApiKey
    public ResponseEntity<?> clearUserOverrides(HttpServletRequest request, @PathVariable Long uid) {
        Object result = resourceLimitService.clearUserOverrides(uid);
        auditLogService.saveLog(PortalLogType.RESOURCE_LIMIT_USER_OVERRIDE_CLEAR, actor(request), "User #" + uid, "ADMIN");
        return ResponseEntity.ok(new DefaultResponse(false, result));
    }

    @PostMapping("/users/{uid}/pause")
    @RequiresSpecialApiKey
    public ResponseEntity<?> pauseUser(HttpServletRequest servletRequest, @PathVariable Long uid, @RequestBody PauseRequest request) {
        Object result = resourceLimitService.pauseUser(uid, request);
        String duration = Boolean.TRUE.equals(request.indefinite()) ? "indefinite" : request.durationMinutes() + " minutes";
        auditLogService.saveLog(PortalLogType.USER_PAUSE, actor(servletRequest), "User #" + uid + " for " + duration, "ADMIN");
        return ResponseEntity.ok(new DefaultResponse(false, result));
    }

    @DeleteMapping("/users/{uid}/pause")
    @RequiresSpecialApiKey
    public ResponseEntity<?> unpauseUser(HttpServletRequest request, @PathVariable Long uid) {
        Object result = resourceLimitService.unpauseUser(uid);
        auditLogService.saveLog(PortalLogType.USER_UNPAUSE, actor(request), "User #" + uid, "ADMIN");
        return ResponseEntity.ok(new DefaultResponse(false, result));
    }

    private User actor(HttpServletRequest request) {
        return (User) request.getAttribute("uploader");
    }
}
