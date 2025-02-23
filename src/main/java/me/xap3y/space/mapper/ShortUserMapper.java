package me.xap3y.space.mapper;

import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.UserInviter;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class ShortUserMapper implements Function<User, ShortUserDto> {

    @Override
    public ShortUserDto apply(User user) {
        return new ShortUserDto(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getAvatar(),
                user.getCreatedAt(),
                (user.getInvitedBy() != null) ? new UserInviter(
                        user.getInvitedBy().getId(),
                        user.getInvitedBy().getUsername(),
                        user.getInvitedBy().getRole()
                ) : null
        );
    }
}
