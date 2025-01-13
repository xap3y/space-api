package me.xap3y.space.dto;

import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.model.UserInviter;
import me.xap3y.space.model.UserSocials;
import me.xap3y.space.model.UserStats;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

public record UserDto(
        long uid,
        String email,
        String username,
        String password,
        UserRole role,
        String avatar,
        LocalDateTime createdAt,

        @Nullable
        UserInviter invitor,

        @NonNull
        UserStats stats,

        @Nullable
        UserSocials socials,

        @Nullable
        String apiKey
) {
}
