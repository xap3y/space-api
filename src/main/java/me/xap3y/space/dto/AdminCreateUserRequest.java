package me.xap3y.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminCreateUserRequest {
    private String username;
    private String email;
    private String password;
    private Boolean verified;
}
