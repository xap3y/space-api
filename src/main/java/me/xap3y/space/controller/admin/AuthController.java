package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.api.enums.Environment;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.entity.Sessions;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.model.AuthLoginRequest;
import me.xap3y.space.model.AuthRegisterRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.InviteCodeService;
import me.xap3y.space.service.SessionService;
import me.xap3y.space.service.UserService;
import me.xap3y.space.util.ConfigDb;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private static final Boolean secure = false;

    private final UserService userService;
    private final InviteCodeService inviteCodeService;
    private final PasswordEncoder passwordEncoder;
    private final ServerInfo serverInfo;
    private final SessionService sessionService;
    private final UserMapper userMapper;

    public AuthController(UserService userService, InviteCodeService inviteCodeService, PasswordEncoder passwordEncoder, ServerInfo serverInfo, SessionService sessionService, UserMapper userMapper) {
        this.userService = userService;
        this.inviteCodeService = inviteCodeService;
        this.passwordEncoder = passwordEncoder;
        this.serverInfo = serverInfo;
        this.sessionService = sessionService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CookieValue(value = "session_token", required = false) String token) {
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Sessions session = sessionService.getSession(token);
        if (session == null || !session.getIsValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto user = userMapper.apply(session.getUserId());
        return ResponseEntity.ok(new DefaultResponse(false, user));
    }

    @PostMapping(
            value = "/login",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> tryLogin(
            HttpServletRequest request,
            @RequestBody AuthLoginRequest loginRequest
    ) {

        User user = userService.findByEmailRaw(loginRequest.getEmail());

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid password"), HttpStatus.FORBIDDEN);
        }

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        if (userAgent.contains("Postman") || userAgent.contains("curl") || userAgent.contains("Insomnia")) {
            return ResponseEntity.ok()
                    .body(new DefaultResponse(false, user));
        }

        String sessionToken = sessionService.createSession(user, userAgent, ipAddress);

        ResponseCookie cookie = ResponseCookie.from("session_token", sessionToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .secure(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new DefaultResponse(false, user));
    }

    @PostMapping(
            value = "/logout",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> tryLogout(
            HttpServletRequest request,
            @CookieValue("session_token") String token
    ) {
        if (token == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "No session token provided"), HttpStatus.BAD_REQUEST);
        }

        try {
            sessionService.invalidateSession(token);
        } catch (Exception e) {
            return new ResponseEntity<>(new DefaultResponse(true, "Failed to delete session"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String userAgent = request.getHeader("User-Agent");

        if (userAgent.contains("Postman") || userAgent.contains("curl")) {
            return ResponseEntity.ok(new DefaultResponse(false, "Logged out"));
        }

        ResponseCookie expiredCookie = ResponseCookie.from("session_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body("Logged out");

    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createUser(
            @RequestBody AuthRegisterRequest registerRequest
    ) {

        if (!serverInfo.getEnv().equals("dev") || SpaceApplication.env == Environment.PRODUCTION) {
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
        } else if (registerRequest.getUsername().contains(registerRequest.getEmail())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Username can't contain your email!"), HttpStatus.BAD_REQUEST);
        } else if (registerRequest.getUsername().contains(registerRequest.getPassword())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Username can't contain your password!"), HttpStatus.BAD_REQUEST);
        } else if (registerRequest.getEmail().equals(registerRequest.getPassword())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Email and password can't be the same!"), HttpStatus.BAD_REQUEST);
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
