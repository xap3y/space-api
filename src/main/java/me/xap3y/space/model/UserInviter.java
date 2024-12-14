package me.xap3y.space.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.api.enums.UserRole;

@Getter
@Setter
@AllArgsConstructor
public class UserInviter {

    private long uid;
    private String username;
    private UserRole role;
}
