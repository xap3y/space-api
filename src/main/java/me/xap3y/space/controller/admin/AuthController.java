package me.xap3y.space.controller.admin;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.model.AuthLoginRequest;
import me.xap3y.space.model.AuthRegisterRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.InviteCodeService;
import me.xap3y.space.service.UserService;
import me.xap3y.space.util.ConfigDb;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final UserService userService;
    private final InviteCodeService inviteCodeService;
    private final PasswordEncoder passwordEncoder;
    private final ServerInfo serverInfo;

    public AuthController(UserService userService, InviteCodeService inviteCodeService, PasswordEncoder passwordEncoder, ServerInfo serverInfo) {
        this.userService = userService;
        this.inviteCodeService = inviteCodeService;
        this.passwordEncoder = passwordEncoder;
        this.serverInfo = serverInfo;
    }

    @PostMapping(
            value = "/login",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> tryLogin(
            @RequestBody AuthLoginRequest loginRequest
    ) {

        UserDto user = userService.findByEmail(loginRequest.getEmail());

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.password())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid password"), HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.ok(new DefaultResponse(false, user));
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createUser(
            @RequestBody AuthRegisterRequest registerRequest
    ) {

        if (!serverInfo.getEnv().equals("dev")) {
            return new ResponseEntity<>(new DefaultResponse(true, "Registration is currently disabled"), HttpStatus.FORBIDDEN);
        }

        if (!isValidEmailAddress(registerRequest.getEmail())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid email address"), HttpStatus.BAD_REQUEST);
        } else if (!isUsernameValid(registerRequest.getUsername())) {
            return new ResponseEntity<>(new DefaultResponse(true, "You can't have this username!"), HttpStatus.BAD_REQUEST);
        } else if (userService.existsByUsername(registerRequest.getUsername())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Username is taken!"), HttpStatus.BAD_REQUEST);
        } else if (userService.existsByEmail(registerRequest.getEmail())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Email already in use!"), HttpStatus.BAD_REQUEST);
        } else if (!inviteCodeService.isValidInviteCode(registerRequest.getInviteCode())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid invite code"), HttpStatus.BAD_REQUEST);
        }

        try {
            userService.registerUser(registerRequest);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(new DefaultResponse(true, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok(new DefaultResponse(false, "Registration successful"));
    }

    private boolean isValidEmailAddress(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern p = Pattern.compile(emailRegex);
        return email != null && p.matcher(email).matches();
    }

    private boolean isUsernameValid(String username) {
        for (String uname : ConfigDb.BLACKLISTED_USERNAMES) {
            if (username.contains(uname)) {
                return false;
            }
        }
        return true;
    }
}
