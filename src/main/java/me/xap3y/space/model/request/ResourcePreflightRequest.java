package me.xap3y.space.model.request;

import me.xap3y.space.api.enums.ResourceLimitType;

public record ResourcePreflightRequest(ResourceLimitType type, Long count, Long bytes) {
}
