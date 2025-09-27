package me.xap3y.space.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthRegisterRequest {
    private String username;
    private String email;
    private String password;
    private String inviteCode;
}
