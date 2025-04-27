package me.xap3y.space.mapper;

import me.xap3y.space.entity.User;
import me.xap3y.space.model.UserInviter;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserInvitorMapper implements Function<User, UserInviter> {

    @Override
    public UserInviter apply(User user) {
        return new UserInviter(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getAvatar(),
                user.getCreatedAt()
        );
    }
}
