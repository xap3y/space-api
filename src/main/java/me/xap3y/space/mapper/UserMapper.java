package me.xap3y.space.mapper;

import me.xap3y.space.dto.UserDto;
import me.xap3y.space.entity.User;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserMapper implements Function<User, UserDto> {

    @Override
    public UserDto apply(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getAvatar(),
                user.getInvitedBy() != null ? user.getInvitedBy().getId() : null,
                user.getCreatedAt(),
                user.getStats(),
                user.getSocials()
        );
    }
}
