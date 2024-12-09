package me.xap3y.space.model;

import lombok.Data;

@Data
public class AuthRegisterRequest {
    private String username;
    private String email;
    private String password;
    private String inviteCode;
}
