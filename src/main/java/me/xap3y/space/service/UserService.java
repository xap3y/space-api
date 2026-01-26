package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.enums.UserAccountStatus;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.entity.ApiKey;
import me.xap3y.space.entity.EmailVerifyCodes;
import me.xap3y.space.entity.InviteCode;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.model.request.AuthRegisterRequest;
import me.xap3y.space.model.UserSocials;
import me.xap3y.space.repository.ApiKeyRepository;
import me.xap3y.space.repository.InviteCodeRepository;
import me.xap3y.space.repository.UserRepository;
import me.xap3y.space.util.Utils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final InviteCodeRepository inviteCodeRepository;
    private final EmailVerifyCodeService emailVerifyCodeService;
    //private final EmailService emailService;
    private final UserSettingsService userSettingsService;
    private final PrometheusMetricService prometheusMetricService;

    public UserService(UserRepository userRepository, ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder, UserMapper userMapper, InviteCodeRepository inviteCodeRepository, EmailVerifyCodeService emailVerifyCodeService, UserSettingsService userSettingsService, PrometheusMetricService prometheusMetricService) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.inviteCodeRepository = inviteCodeRepository;
        this.emailVerifyCodeService = emailVerifyCodeService;
        //this.emailService = emailService;
        this.userSettingsService = userSettingsService;
        this.prometheusMetricService = prometheusMetricService;
    }

    public void deleteById(Long uid) {
        userRepository.deleteById(uid);
        prometheusMetricService.recordEvent(MetricRecordType.USER_DELETED);
    }

    public User registerUser(AuthRegisterRequest req) {
        String encodedPassword = passwordEncoder.encode(req.getPassword());
        User user = new User(req.getEmail(), req.getUsername(), encodedPassword);
        ApiKey apiKey = new ApiKey();
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setKeyCode(Utils.generateApiKey());
        apiKey.setMaxUploadSize(-1);
        apiKeyRepository.save(apiKey);
        user.setApiKey(apiKey);

        InviteCode code = inviteCodeRepository.findByCode(req.getInviteCode()).orElseThrow();
        if (code.getCreatedBy() != null) {
            user.setInvitedBy(code.getCreatedBy());
        }

        User registeredUser = userRepository.save(user);

        userSettingsService.createDefaultSettingsForUser(registeredUser);

        EmailVerifyCodes verifyCode = emailVerifyCodeService.generateAndSaveCode(user);

        int res = inviteCodeRepository.markAsUsed(req.getInviteCode(), LocalDateTime.now(), user);
        log.info("Marked invite code as used: {}", res);
        prometheusMetricService.recordEvent(MetricRecordType.USER_SIGNUP);
        return registeredUser;
    }

    public void createUser(String username, String password, String email, boolean test) {

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(email, username, encodedPassword);
        UserSocials socials;
        if (test) socials = new UserSocials(
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test",
                "test"
        );
        else socials = new UserSocials();

        ApiKey apiKey = new ApiKey();
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setKeyCode(Utils.generateApiKey());
        apiKey.setMaxUploadSize(-1);
        apiKeyRepository.save(apiKey);

        user.setApiKey(apiKey);
        user.setSocials(socials);
        userRepository.save(user);
    }

    public boolean updateUserStatus(User user, UserAccountStatus status) {
        if (user == null) {
            return false;
        }
        user.setStatus(status);
        userRepository.save(user);
        return true;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> tryFindByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserDto findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Optional<User> findById(Long uid) {
        return userRepository.findById(uid);
    }

    public UserDto findByEmail(String email) throws ResourceNotFoundException {
        return userRepository.findByEmail(email)
                .map(userMapper)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User findByEmailRaw(String email) throws ResourceNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

}
