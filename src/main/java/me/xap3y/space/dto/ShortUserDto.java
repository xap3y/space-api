package me.xap3y.space.dto;

import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.model.UserInviter;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

public record ShortUserDto(
        long uid,
        String username,
        UserRole role,
        String avatar,
        LocalDateTime createdAt,

        @Nullable
        UserInviter invitor
) {
}
