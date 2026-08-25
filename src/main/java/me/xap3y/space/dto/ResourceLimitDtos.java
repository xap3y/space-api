package me.xap3y.space.dto;

import me.xap3y.space.api.enums.ResourceLimitType;
import me.xap3y.space.api.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Map;

public final class ResourceLimitDtos {
    private ResourceLimitDtos() {}

    public record RuleValues(Long dailyCount, Long weeklyCount, Long dailyBytes, Long weeklyBytes) {}

    public record FilePackLimits(Integer maxFiles, Long maxBytes) {}

    public record PolicyUpdate(Map<ResourceLimitType, RuleValues> limits, FilePackLimits filePackLimits) {}

    public record RolePolicy(UserRole role, Map<ResourceLimitType, RuleValues> limits, FilePackLimits filePackLimits) {}

    public record PauseRequest(Long durationMinutes, Boolean indefinite) {}

    public record UsageValues(long dailyCount, long weeklyCount, long dailyBytes, long weeklyBytes) {}

    public record UserPolicy(
            Long uid,
            String username,
            UserRole role,
            Map<ResourceLimitType, RuleValues> overrides,
            Map<ResourceLimitType, RuleValues> effective,
            Map<ResourceLimitType, UsageValues> usage,
            FilePackLimits filePackOverrides,
            FilePackLimits effectiveFilePackLimits,
            boolean paused,
            boolean pausedIndefinitely,
            LocalDateTime pausedUntil
    ) {}
}
