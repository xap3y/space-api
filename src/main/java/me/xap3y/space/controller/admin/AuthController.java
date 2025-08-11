package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.api.enums.Environment;
import me.xap3y.space.api.enums.UserAccountStatus;
import me.xap3y.space.api.exception.EmailVerifyCodeExpired;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.entity.EmailVerifyCodes;
import me.xap3y.space.entity.Sessions;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.model.AuthLoginRequest;
import me.xap3y.space.model.AuthRegisterRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.*;
import me.xap3y.space.util.ConfigDb;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
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
    private final ApiKeyService apiKeyService;
    private final EmailService emailService;
    private final EmailVerifyCodeService emailVerifyCodeService;

    public AuthController(UserService userService, InviteCodeService inviteCodeService, PasswordEncoder passwordEncoder, ServerInfo serverInfo, SessionService sessionService, UserMapper userMapper, ApiKeyService apiKeyService, EmailService emailService, EmailVerifyCodeService emailVerifyCodeService) {
        this.userService = userService;
        this.inviteCodeService = inviteCodeService;
        this.passwordEncoder = passwordEncoder;
        this.serverInfo = serverInfo;
        this.sessionService = sessionService;
        this.userMapper = userMapper;
        this.apiKeyService = apiKeyService;
        this.emailService = emailService;
        this.emailVerifyCodeService = emailVerifyCodeService;
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

    @GetMapping("/validate")
    @RequiresApiKey
    public ResponseEntity<?> validateApiKey(
            HttpServletRequest request
            //@RequestHeader(required = false, value = "key") String apiKey
    ) {

        /*if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        apiKeyService.validateApiKey(apiKey);*/

        return ResponseEntity.ok(new DefaultResponse(false, "OK"));
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

        if (user.getStatus() != UserAccountStatus.ACTIVE) {
            return new ResponseEntity<>(new DefaultResponse(true, "Your account is not active! Check your email and activate it."), HttpStatus.FORBIDDEN);
        }

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        if (userAgent.contains("Postman") || userAgent.contains("curl") || userAgent.contains("Insomnia")) {
            return ResponseEntity.ok()
                    .body(new DefaultResponse(false, user));
        }

        String sessionToken = sessionService.createSession(user, userAgent, ipAddress);

        //String code = String.valueOf((int)(Math.random() * 900000) + 100000);
        //emailService.sendVerificationCode(user.getEmail(), code, );

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
            value = "/verify/telegram",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> verifyEmail(
            HttpServletRequest request,
            @CookieValue(value = "verify_token", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No verification token provided"), HttpStatus.BAD_REQUEST);
        }
        User uploader = apiKeyService.validateApiKey(token);

        EmailVerifyCodes verifyCode = emailVerifyCodeService.findTopByUserStrict(uploader);

        if (verifyCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new EmailVerifyCodeExpired();
        } else if (verifyCode.isUsed()) {
            return new ResponseEntity<>(new DefaultResponse(true, "This code has already been used"), HttpStatus.BAD_REQUEST);
        }

        Map<String, String> res = Map.of(
                "botname", "xapspace_auth_dev_bot", //XapSpaceAuth_bot
                "token", verifyCode.getTelCode()
        );


        return new ResponseEntity<>(new DefaultResponse(false, res), HttpStatus.OK);
    }

    @PostMapping(
            value = "/verify/validate",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> validateVerifyToken(
            HttpServletRequest request,
            @CookieValue(value = "verify_token", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No verification token provided"), HttpStatus.BAD_REQUEST);
        }

        User uploader = apiKeyService.validateApiKey(token);

        if (uploader.getStatus() != UserAccountStatus.WAITING_VERIFICATION) {
            return new ResponseEntity<>(new DefaultResponse(true, "User is not pending verification"), HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(new DefaultResponse(false, "Verification token is valid for user: " + uploader.getUsername()));
    }

    @PostMapping(
            value = "/verify/sendemail",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> sendVerifyEmail(
            HttpServletRequest request,
            @CookieValue(value = "verify_token", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No verification token provided"), HttpStatus.BAD_REQUEST);
        }

        User uploader = apiKeyService.validateApiKey(token);

        EmailVerifyCodes verifyCode = emailVerifyCodeService.findTopByUserStrict(uploader);

        emailService.sendVerificationCode(verifyCode.getEmail(), verifyCode.getCode(), verifyCode.getUrlCode());

        return ResponseEntity.ok(new DefaultResponse(false, "Verification email sent successfully"));
    }

    @PostMapping(
            value = "/verify/email",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> verifyEmail(
            HttpServletRequest request,
            @RequestParam("code") String code,
            @CookieValue(value = "verify_token", required = false) String token
    ) {

        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No verification token provided"), HttpStatus.BAD_REQUEST);
        }

        User uploader = apiKeyService.validateApiKey(token);

        EmailVerifyCodes verifyCode = emailVerifyCodeService.findByCodeStrict(code);

        if (!Objects.equals(verifyCode.getUser().getId(), uploader.getId())) {
            throw new InvalidApiKeyException();
        }

        if (verifyCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new EmailVerifyCodeExpired();
        } else if (verifyCode.isUsed()) {
            return new ResponseEntity<>(new DefaultResponse(true, "This code has already been used"), HttpStatus.BAD_REQUEST);
        }

        userService.updateUserStatus(uploader, UserAccountStatus.ACTIVE);
        emailVerifyCodeService.setCodeUsed(verifyCode);

        DefaultResponse response = new DefaultResponse(false, "Email verified successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/verify/token",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> verifyEmailToken(
            HttpServletRequest request,
            @RequestParam("code") String code
    ) {
        EmailVerifyCodes verifyCode = emailVerifyCodeService.findByUrlCodeStrict(code);

        if (verifyCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new EmailVerifyCodeExpired();
        } else if (verifyCode.isUsed()) {
            return new ResponseEntity<>(new DefaultResponse(true, "This code has already been used"), HttpStatus.BAD_REQUEST);
        }

        userService.updateUserStatus(verifyCode.getUser(), UserAccountStatus.ACTIVE);
        emailVerifyCodeService.setCodeUsed(verifyCode);

        // return text/html response for email verification
        String htmlResponse = "<html><body><h1>Email verified successfully!</h1></body></html>";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(htmlResponse, headers, HttpStatus.OK);
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

        User regiseredUser;

        try {
            regiseredUser = userService.registerUser(registerRequest);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(new DefaultResponse(true, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ResponseCookie cookie = ResponseCookie.from("verify_token", regiseredUser.getApiKey().getKeyCode())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(15 * 60)
                .sameSite("None")
                .secure(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new DefaultResponse(false, "Registration successful"));
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
