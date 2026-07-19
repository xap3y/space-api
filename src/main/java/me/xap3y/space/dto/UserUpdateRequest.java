package me.xap3y.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.xap3y.space.api.enums.UserRole;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private String mail;
    private String email;
    private String profilePicUrl;
    private String avatar;
    private UserRole role;
    private String password;
}
