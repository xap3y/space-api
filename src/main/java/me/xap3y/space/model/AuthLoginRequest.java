package me.xap3y.space.model;

import lombok.Data;

@Data
public class AuthLoginRequest {
    private String email;
    private String password;
}
