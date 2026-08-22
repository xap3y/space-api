package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.api.enums.Environment;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.enums.UserAccountStatus;
import me.xap3y.space.api.exception.*;
import me.xap3y.space.api.iface.OptionalApiKey;
import me.xap3y.space.api.iface.OptionalCookieAuth;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.SessionDto;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.entity.*;
import me.xap3y.space.mapper.SessionMapper;
import me.xap3y.space.mapper.ShortUserMapper;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.model.request.AuthLoginRequest;
import me.xap3y.space.model.request.AuthRegisterRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.*;
import me.xap3y.space.util.ConfigDb;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;
    private final InviteCodeService inviteCodeService;
    private final PasswordEncoder passwordEncoder;
    private final ServerInfo serverInfo;
    private final SessionService sessionService;
    private final UserMapper userMapper;
    private final ApiKeyService apiKeyService;
    private final EmailService emailService;
    private final EmailVerifyCodeService emailVerifyCodeService;
    private final LogsService logsService;
    private final PrometheusMetricService prometheusMetricService;
    private final SessionMapper sessionMapper;
    private final AuditLogService auditLogService;
    private final ShortUserMapper shortUserMapper;
    private final TurnStileService turnStileService;
    private final MinecraftServerReportsService minecraftServerReportsService;
    private final TrSessionService trSessionService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CookieValue(value = "session_token", required = false) String token) {
        if (token == null) {
            throw new MissingCredentialsException();
        }
        Session session = sessionService.getValidSession(token);
        if (session == null || !session.getIsValid()) {
            throw new InvalidApiKeyException();
        }
        UserDto user = userMapper.apply(session.getUser());
        return ResponseEntity.ok(new DefaultResponse(false, user));
    }

    @GetMapping("/tr/me")
    public ResponseEntity<?> getCurrentTrUser(@CookieValue(value = "tr_token", required = false) String token) {
        if (token == null) {
            throw new MissingCredentialsException();
        }
        TrSession session = trSessionService.getValidSession(token);
        if (session == null || !session.getIsValid()) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(new DefaultResponse(false, session.getUser()));
    }

    @GetMapping("/me/sessions")
    @OptionalApiKey
    public ResponseEntity<?> getCurrentUserSessions(
            HttpServletRequest request,
            @CookieValue(value = "session_token", required = false) String token
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null && token == null) throw new InvalidApiKeyException();


        if (token != null && uploader == null) {
            Session session = sessionService.getValidSession(token);
            if (session == null || !session.getIsValid()) {
                throw new UnauthorizedException();
            }
            uploader = session.getUser();
        }

        final String activeToken = token;
        List<SessionDto> sessionDtoList = sessionService.getSessions(uploader.getId())
                .stream()
                .map(s -> sessionMapper.apply(s, false, activeToken))
                .toList();

        return ResponseEntity.ok(new DefaultResponse(false, sessionDtoList));
    }

    @DeleteMapping("/me/sessions/{id}")
    @OptionalApiKey
    public ResponseEntity<?> revokeSession(
            HttpServletRequest request,
            @CookieValue(value = "session_token", required = false) String token,
            @PathVariable Long id
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null && token == null) throw new InvalidApiKeyException();

        if (token != null && uploader == null) {
            Session session = sessionService.getValidSession(token);
            if (session == null || !session.getIsValid()) {
                throw new UnauthorizedException();
            }
            uploader = session.getUser();
        }

        Session sessionToRevoke = sessionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!sessionToRevoke.getUser().getId().equals(uploader.getId())) {
            throw new UnauthorizedException("You do not own this session");
        }

        if (token != null && token.equals(sessionToRevoke.getToken())) {
            throw new BadRequestException("Your current session cannot be revoked from this page");
        }

        sessionService.invalidateSessionById(id);

        return ResponseEntity.ok(new DefaultResponse(false, "Session revoked successfully"));
    }

    @GetMapping("/validate")
    @RequiresApiKey
    @OptionalCookieAuth
    public ResponseEntity<?> validateApiKey(
            HttpServletRequest request
    ) {
        User user = (User) request.getAttribute("uploader");
        ShortUserDto userDto = shortUserMapper.apply(user, false);

        return new ResponseEntity<>(new DefaultResponse(false, userDto), HttpStatus.OK);
    }

    @GetMapping("/csrf")
    public void setupCsrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (token != null) {
            response.setHeader("X-CSRF-TOKEN", token.getToken());
        }
    }

    @PostMapping(
            value = "/tr/login",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> tryTrLogin(
            HttpServletRequest request,
            @RequestBody AuthLoginRequest body
    ) {
        if (body.getEmail() == null || body.getPassword() == null) {
            throw new BadRequestException("Username/Email and password are required");
        } else if (body.getToken() == null) {
            throw new BadRequestException("Captcha token is required!");
        }

        boolean isCaTokenValid = turnStileService.validate(body.getToken());
        if (!isCaTokenValid) throw new ResourceAccessForbiddenException();

        MinecraftServerReports user = minecraftServerReportsService.findByServerName(body.getEmail())
                .orElse(minecraftServerReportsService.findByOwnerEmail(body.getEmail()).orElse(null));

        if (user == null) {
            throw new BadRequestException("No user found with this email or username");
        }

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        if (isUserAgentBot(userAgent)) {
            throw new BadRequestException("Bots are not allowed to log in");
        }

        TrSession sessionToken = trSessionService.createSession(user, userAgent, ipAddress);

        ResponseCookie cookie = ResponseCookie.from("tr_token", sessionToken.getToken())
                .httpOnly(serverInfo.getSetCookieHttpOnly())
                .path("/")
                .maxAge(serverInfo.getAuthCookieMaxAge())
                .sameSite(serverInfo.getAuthCookieSameSite())
                .domain(serverInfo.getAuthCookieDomain())
                .secure(serverInfo.getAuthCookieSecure())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new DefaultResponse(false, user));
    }

    @PostMapping(
            value = "/login",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> tryLogin(
            HttpServletRequest request,
            @RequestBody AuthLoginRequest loginRequest
    ) {

        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        } else if (loginRequest.getToken() == null) {
            throw new BadRequestException("Captcha token is required!");
        }

        boolean isCaTokenValid = turnStileService.validate(loginRequest.getToken());
        if (!isCaTokenValid) throw new ResourceAccessForbiddenException();

        User user = userService.findByEmailRaw(loginRequest.getEmail());

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid password"), HttpStatus.FORBIDDEN);
        } else if (user.getStatus() != UserAccountStatus.ACTIVE) {
            return new ResponseEntity<>(new DefaultResponse(true, "Your account is not active!"), HttpStatus.FORBIDDEN);
        } else if (user.getApiKey() == null) {
            throw new InvalidApiKeyException();
        }

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        if (isUserAgentBot(userAgent)) {
            return ResponseEntity.ok()
                    .body(new DefaultResponse(false, user));
        }

        String sessionToken = sessionService.createSession(user, userAgent, ipAddress);

        //String code = String.valueOf((int)(Math.random() * 900000) + 100000);
        //emailService.sendVerificationCode(user.getEmail(), code, );
        prometheusMetricService.recordEvent(MetricRecordType.USER_LOGIN);
        auditLogService.saveLog(PortalLogType.USER_LOGIN, user);

        ResponseCookie cookie = ResponseCookie.from("session_token", sessionToken)
                .httpOnly(serverInfo.getSetCookieHttpOnly())
                .path("/")
                .maxAge(serverInfo.getAuthCookieMaxAge())
                .sameSite(serverInfo.getAuthCookieSameSite())
                .domain(serverInfo.getAuthCookieDomain())
                .secure(serverInfo.getAuthCookieSecure())
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
            throw new BadRequestException("No session token provided");
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
                .httpOnly(serverInfo.getSetCookieHttpOnly())
                .secure(serverInfo.getAuthCookieSecure())
                .sameSite(serverInfo.getAuthCookieSameSite())
                .domain(serverInfo.getAuthCookieDomain())
                .build();

        Session session = sessionService.getSession(token);
        if (session != null) {
            auditLogService.saveLog(PortalLogType.USER_LOGOUT, session.getUser());
        }

        prometheusMetricService.recordEvent(MetricRecordType.USER_LOGOUT);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body("Logged out");
    }

    @PostMapping(
            value = "/verify/telegram",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> verifyEmailTelegram(
            HttpServletRequest request,
            @CookieValue(value = "verify_token", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            throw new BadRequestException("No verification token provided");
        }
        User uploader = apiKeyService.validateApiKey(token);

        EmailVerifyCodes verifyCode = emailVerifyCodeService.findTopByUserStrict(uploader);

        if (verifyCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new EmailVerifyCodeExpired();
        } else if (verifyCode.isUsed()) {
            throw new BadRequestException("This code has already been used");
        }

        Map<String, String> res = Map.of(
                "botname", serverInfo.getTelegramVerifyBotName(),
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
            throw new BadRequestException("No verification token provided");
        }

        User uploader = apiKeyService.validateApiKey(token);

        if (uploader.getStatus() != UserAccountStatus.WAITING_VERIFICATION) {
            throw new BadRequestException("User is not pending verification");
        }

        return ResponseEntity.ok(new DefaultResponse(false, "Verification token is valid for user: " + uploader.getUsername()));
    }

    @PostMapping(
            value = "/verify/resendemail",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    //@RequiresApiKey
    public ResponseEntity<?> resendVerifyEmail(
            HttpServletRequest request,
            @CookieValue(value = "verify_token", required = false) String token,
            @CookieValue(value = "new_email", required = false) String newMail
    ) {
        if (token == null || token.isEmpty()) return new ResponseEntity<>(new DefaultResponse(true, "No verification token provided"), HttpStatus.BAD_REQUEST);

        //User uploaderFromReq = (User) request.getAttribute("uploader"); //
        User uploader = apiKeyService.validateApiKey(token);
        EmailVerifyCodes verifyCode = emailVerifyCodeService.findTopByUserStrict(uploader);

        if (verifyCode.isUsed()) throw new ResourceNotFoundException();
        else if (verifyCode.getExpiresAt().isBefore(LocalDateTime.now())) throw new EmailVerifyCodeExpired();
        if (newMail != null && !isValidEmailAddress(newMail)) throw new BadRequestException("Invalid newMail");

        emailService.sendVerificationCode(newMail == null ? verifyCode.getEmail() : newMail, verifyCode.getCode(), verifyCode.getUrlCode());

        logsService.logFile("-- resendVerifyEmail - " + uploader.getUsername() + " - " + (newMail == null ? verifyCode.getEmail() : newMail));
        
        return new ResponseEntity<>(new DefaultResponse(true, "Email sent"), HttpStatus.OK);
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
        } else if (registerRequest.getUsername().contains(registerRequest.getEmail())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Username can't contain your email!"), HttpStatus.BAD_REQUEST);
        } else if (registerRequest.getUsername().contains(registerRequest.getPassword())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Username can't contain your password!"), HttpStatus.BAD_REQUEST);
        } else if (registerRequest.getEmail().equals(registerRequest.getPassword())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Email and password can't be the same!"), HttpStatus.BAD_REQUEST);
        } else if (!inviteCodeService.isValidInviteCode(registerRequest.getInviteCode())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid invite code"), HttpStatus.BAD_REQUEST);
        } else if (userService.existsByUsername(registerRequest.getUsername())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Username is taken!"), HttpStatus.BAD_REQUEST);
        } else if (userService.existsByEmail(registerRequest.getEmail())) {
            return new ResponseEntity<>(new DefaultResponse(true, "This email is already taken!"), HttpStatus.BAD_REQUEST);
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

    private boolean isUserAgentBot(String userAgent) {
        return userAgent.contains("Postman") || userAgent.contains("curl") || userAgent.contains("Insomnia");
    }
}
