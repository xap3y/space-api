package me.xap3y.space.controller.admin;

import lombok.RequiredArgsConstructor;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.ResourceLimitDtos.PauseRequest;
import me.xap3y.space.dto.ResourceLimitDtos.PolicyUpdate;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ResourceLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/limits")
@RequiredArgsConstructor
public class ResourceLimitController {

    private final ResourceLimitService resourceLimitService;

    @GetMapping("/roles")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.getRolePolicies()));
    }

    @PutMapping("/roles/{role}")
    @RequiresSpecialApiKey
    public ResponseEntity<?> updateRole(@PathVariable UserRole role, @RequestBody PolicyUpdate update) {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.updateRolePolicy(role, update)));
    }

    @GetMapping("/users/{uid}")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUser(@PathVariable Long uid) {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.getUserPolicy(uid)));
    }

    @PutMapping("/users/{uid}")
    @RequiresSpecialApiKey
    public ResponseEntity<?> updateUser(@PathVariable Long uid, @RequestBody PolicyUpdate update) {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.updateUserPolicy(uid, update)));
    }

    @DeleteMapping("/users/{uid}/overrides")
    @RequiresSpecialApiKey
    public ResponseEntity<?> clearUserOverrides(@PathVariable Long uid) {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.clearUserOverrides(uid)));
    }

    @PostMapping("/users/{uid}/pause")
    @RequiresSpecialApiKey
    public ResponseEntity<?> pauseUser(@PathVariable Long uid, @RequestBody PauseRequest request) {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.pauseUser(uid, request)));
    }

    @DeleteMapping("/users/{uid}/pause")
    @RequiresSpecialApiKey
    public ResponseEntity<?> unpauseUser(@PathVariable Long uid) {
        return ResponseEntity.ok(new DefaultResponse(false, resourceLimitService.unpauseUser(uid)));
    }
}
