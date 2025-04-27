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
    private final UserInvitorMapper userInvitorMapper;

    public UserMapper(HelperService helperService, UserInvitorMapper userInvitorMapper) {
        this.helperService = helperService;
        this.userInvitorMapper = userInvitorMapper;
    }

    @Override
    public UserDto apply(User user) {

        UserInviter inviter = null;
        if (user.getInvitedBy() != null) {
            inviter = userInvitorMapper.apply(user.getInvitedBy());
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
                user.getApiKey().getKeyCode()
        );
    }
}
