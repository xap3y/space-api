package me.xap3y.space.mapper;

import me.xap3y.space.dto.UserDto;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.UserInviter;
import me.xap3y.space.service.HelperService;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserMapper implements Function<User, UserDto> {

    private final HelperService helperService;

    public UserMapper(HelperService helperService) {
        this.helperService = helperService;
    }

    @Override
    public UserDto apply(User user) {

        UserInviter inviter = null;
        if (user.getInvitedBy() != null) {
            inviter = new UserInviter(
                    user.getInvitedBy().getId(),
                    user.getInvitedBy().getUsername(),
                    user.getInvitedBy().getRole()
            );
        }
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getAvatar(),
                user.getCreatedAt(),
                inviter,
                helperService.getUserStats(user.getId()),
                user.getSocials(),
                user.getApiKey().getKeyHash()
        );
    }
}
