package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.SegmentType;
import me.xap3y.space.api.enums.UrlSetPreference;
import me.xap3y.space.api.enums.ResourceLimitType;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.PageImage;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.dto.UserDto;
import me.xap3y.space.dto.UserSettingsDto;
import me.xap3y.space.dto.UserSocialsPatchDto;
import me.xap3y.space.entity.User;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.UserSettings;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.mapper.UserSettingsMapper;
import me.xap3y.space.model.UserSocials;
import me.xap3y.space.model.UserWebhookSettings;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.ImageListResponse;
import me.xap3y.space.model.request.TwoFactorCodeRequest;
import me.xap3y.space.service.*;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/user")
@Getter
@Slf4j
public class UserController {

    public record PasswordChangeRequest(String oldPassword, String newPassword, String twoFactorCode) {}
    public record EmailChangeRequest(String password, String newEmail, String twoFactorCode) {}

    private final UserService userService;
    private final ImageService imageService;
    private final UserMapper userMapper;
    private final ServerInfo serverInfo;
    private final UrlService urlService;
    private final PasteService pasteService;
    private final UserSettingsService userSettingsService;
    private final UserSettingsMapper userSettingsMapper;
    private final PasswordEncoder passwordEncoder;
    private final ResourceLimitService resourceLimitService;
    private final TwoFactorService twoFactorService;
    private final AuditLogService auditLogService;

    @PutMapping("/me/password")
    @RequiresApiKey
    public ResponseEntity<?> changePassword(HttpServletRequest request, @RequestBody PasswordChangeRequest body) {
        User user = (User) request.getAttribute("uploader");
        if (body.oldPassword() == null || !passwordEncoder.matches(body.oldPassword(), user.getPassword()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DefaultResponse(true, "Current password is incorrect"));
        if (body.newPassword() == null || body.newPassword().length() < 5)
            return ResponseEntity.badRequest().body(new DefaultResponse(true, "New password must be at least 5 characters"));
        if (passwordEncoder.matches(body.newPassword(), user.getPassword()))
            return ResponseEntity.badRequest().body(new DefaultResponse(true, "New password must be different"));
        twoFactorService.verifySensitiveAction(user, body.twoFactorCode());
        user.setPassword(passwordEncoder.encode(body.newPassword()));
        userService.saveUser(user);
        auditLogService.saveLog(PortalLogType.USER_UPDATE_PROFILE, user, "Changed account password", "PROFILE");
        return ResponseEntity.ok(new DefaultResponse(false, "Password changed successfully"));
    }

    @PutMapping("/me/email")
    @RequiresApiKey
    public ResponseEntity<?> changeEmail(HttpServletRequest request, @RequestBody EmailChangeRequest body) {
        User user = (User) request.getAttribute("uploader");
        if (body.password() == null || !passwordEncoder.matches(body.password(), user.getPassword()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DefaultResponse(true, "Current password is incorrect"));
        if (body.newEmail() == null || !body.newEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            return ResponseEntity.badRequest().body(new DefaultResponse(true, "Enter a valid email address"));
        String email = body.newEmail().trim().toLowerCase();
        if (email.equalsIgnoreCase(user.getEmail()))
            return ResponseEntity.badRequest().body(new DefaultResponse(true, "This is already your email address"));
        if (userService.existsByEmail(email))
            return ResponseEntity.badRequest().body(new DefaultResponse(true, "Email address is already in use"));
        twoFactorService.verifySensitiveAction(user, body.twoFactorCode());
        user.setEmail(email);
        userService.saveUser(user);
        auditLogService.saveLog(PortalLogType.USER_UPDATE_PROFILE, user, "Changed account email address", "PROFILE");
        return ResponseEntity.ok(new DefaultResponse(false, "Email address changed successfully"));
    }

    public UserController(UserService userService, ImageService imageService, UserMapper userMapper, ServerInfo serverInfo, UrlService urlService, PasteService pasteService, UserSettingsService userSettingsService, UserSettingsMapper userSettingsMapper, PasswordEncoder passwordEncoder, ResourceLimitService resourceLimitService, TwoFactorService twoFactorService, AuditLogService auditLogService) {
        this.userService = userService;
        this.imageService = imageService;
        this.userMapper = userMapper;
        this.serverInfo = serverInfo;
        this.urlService = urlService;
        this.pasteService = pasteService;
        this.userSettingsService = userSettingsService;
        this.userSettingsMapper = userSettingsMapper;
        this.passwordEncoder = passwordEncoder;
        this.resourceLimitService = resourceLimitService;
        this.twoFactorService = twoFactorService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/me/2fa")
    @RequiresApiKey
    public ResponseEntity<?> twoFactorStatus(HttpServletRequest request) {
        return ResponseEntity.ok(new DefaultResponse(false, twoFactorService.status((User) request.getAttribute("uploader"))));
    }

    @PostMapping("/me/2fa/setup")
    @RequiresApiKey
    public ResponseEntity<?> beginTwoFactorSetup(HttpServletRequest request) {
        return ResponseEntity.ok(new DefaultResponse(false, twoFactorService.beginSetup((User) request.getAttribute("uploader"))));
    }

    @PostMapping("/me/2fa/confirm")
    @RequiresApiKey
    public ResponseEntity<?> confirmTwoFactorSetup(HttpServletRequest request, @RequestBody TwoFactorCodeRequest body) {
        if (body == null || body.code() == null) throw new BadRequestException("Authentication code is required");
        User uploader = (User) request.getAttribute("uploader");
        List<String> backupCodes = twoFactorService.confirmSetup(uploader, body.code());
        auditLogService.saveLog(PortalLogType.TWO_FACTOR_SETUP, uploader, "2FA enabled", "PORTAL");
        return ResponseEntity.ok(new DefaultResponse(false, Map.of("backupCodes", backupCodes)));
    }

    @PostMapping("/me/2fa/backup-codes")
    @RequiresApiKey
    public ResponseEntity<?> regenerateTwoFactorBackupCodes(HttpServletRequest request, @RequestBody TwoFactorCodeRequest body) {
        if (body == null || body.code() == null) throw new BadRequestException("Authentication code is required");
        User uploader = (User) request.getAttribute("uploader");
        List<String> backupCodes = twoFactorService.regenerateBackupCodes(uploader, body.code());
        auditLogService.saveLog(PortalLogType.TWO_FACTOR_BACKUP_CODES_REGENERATE, uploader, "Backup codes regenerated", "PORTAL");
        return ResponseEntity.ok(new DefaultResponse(false, Map.of("backupCodes", backupCodes)));
    }

    @PostMapping("/me/2fa/disable")
    @RequiresApiKey
    public ResponseEntity<?> disableTwoFactor(HttpServletRequest request, @RequestBody TwoFactorCodeRequest body) {
        if (body == null || body.code() == null) throw new BadRequestException("Authentication or backup code is required");
        User uploader = (User) request.getAttribute("uploader");
        twoFactorService.disable(uploader, body.code());
        auditLogService.saveLog(PortalLogType.TWO_FACTOR_REVOKE, uploader, "2FA disabled by user", "PORTAL");
        return ResponseEntity.ok(new DefaultResponse(false, "Two-factor authentication disabled"));
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @RequiresApiKey
    public ResponseEntity<?> updateAvatar(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        User uploader = (User) request.getAttribute("uploader");
        if (file.isEmpty()) throw new BadRequestException("Choose an image to upload");
        if (file.getSize() > 5L * 1024 * 1024) throw new BadRequestException("Profile pictures must be 5 MB or smaller");

        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();
        if (!List.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(contentType)) {
            throw new BadRequestException("Use a JPEG, PNG, WebP, or GIF image");
        }

        resourceLimitService.assertCanCreate(uploader, ResourceLimitType.IMAGE, 1, file.getSize());
        Image image = imageService.saveImage(file, uploader, null, null, true, "Profile picture", ResourceSourceType.PORTAL);
        resourceLimitService.recordCreation(uploader, ResourceLimitType.IMAGE, 1, file.getSize());

        String avatarUrl = serverInfo.getBaseUrl() + "/v1/image/get/" + image.getUniqueId();
        uploader.setAvatar(avatarUrl);
        userService.saveUser(uploader);
        auditLogService.saveLog(PortalLogType.PROFILE_PICTURE_CHANGE, uploader, image.getUniqueId(), "PORTAL");
        return ResponseEntity.ok(new DefaultResponse(false, Map.of("avatar", avatarUrl)));
    }

    @PostMapping("/create")
    public ResponseEntity<Object> createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "false") Boolean test
    ) {

        //return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);

        if (!serverInfo.getEnv().equals("dev")) {
            return new ResponseEntity<>(new DefaultResponse(true, "Registration is currently disabled"), HttpStatus.FORBIDDEN);
        }

        userService.createUser(username, password, email, test);

        return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.CREATED);
    }

    @GetMapping(
            value = "/me/images",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> getUserImages(
            HttpServletRequest request,
            @RequestParam(value = "from", required = false) Long from,
            @RequestParam(value = "to", required = false) Long to,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        User uploader = (User) request.getAttribute("uploader");

        List<ImageInfoDto> imageDtos = imageService.getAllImagesByUser(uploader.getId(), from, to, limit);
        int count = imageService.countByUploaderId(uploader.getId());

        if (imageDtos.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No images found for this user UID"), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(new DefaultResponse(false, imageDtos, count));
    }

    /**
     * Native and other trusted first-party clients cannot use the portal's
     * server-side administrative key. These self-scoped endpoints expose the
     * same list data while deriving ownership from the authenticated API key.
     */
    @GetMapping(value = "/me/images/pageable", produces = "application/json")
    @RequiresApiKey
    public ResponseEntity<?> getCurrentUserImagesPageable(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "21") Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> formats
    ) {
        User uploader = (User) request.getAttribute("uploader");
        Page<PageImage> images = imageService.getAllImagesByUser(uploader.getId(), page, size, from, to, formats);
        ImageListResponse response = new ImageListResponse(
                images.getContent(), images.getTotalElements(), images.getTotalPages(), images.getNumber(), images.getSize()
        );
        return ResponseEntity.ok(new DefaultResponse(false, response, images.getContent().size()));
    }

    @GetMapping(value = "/me/pastes", produces = "application/json")
    @RequiresApiKey
    public ResponseEntity<?> getCurrentUserPastes(
            HttpServletRequest request,
            @RequestParam(value = "content", required = false, defaultValue = "false") boolean content
    ) {
        User uploader = (User) request.getAttribute("uploader");
        List<PasteDto> pastes = pasteService.getAllPastesByUserId(uploader.getId(), content);
        return ResponseEntity.ok(new DefaultResponse(false, pastes, pastes.size()));
    }

    @GetMapping(value = "/me/urls", produces = "application/json")
    @RequiresApiKey
    public ResponseEntity<?> getCurrentUserUrls(
            HttpServletRequest request,
            @RequestParam(value = "logs", required = false, defaultValue = "false") boolean logs
    ) {
        User uploader = (User) request.getAttribute("uploader");
        List<ShortUrlDto> urls = urlService.getAllShortUrlsByCreatorId(uploader.getId(), logs);
        return ResponseEntity.ok(new DefaultResponse(false, urls, urls.size()));
    }

    @PatchMapping(
            value = "/me/socials",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> updateUserSocials(
            HttpServletRequest request,
            @RequestBody UserSocialsPatchDto body
    ) {
        User uploader = (User) request.getAttribute("uploader");

        UserSocials socials = uploader.getSocials();
        if (socials == null) socials = new UserSocials();

        if (body.getWebsite() != null) socials.setWebsite(body.getWebsite());
        if (body.getTwitter() != null) socials.setTwitter(body.getTwitter());
        if (body.getGithub() != null) socials.setGithub(body.getGithub());
        if (body.getGitlab() != null) socials.setGitlab(body.getGitlab());
        if (body.getDiscord() != null) socials.setDiscord(body.getDiscord());
        if (body.getTelegram() != null) socials.setTelegram(body.getTelegram());
        if (body.getVk() != null) socials.setVk(body.getVk());
        if (body.getFacebook() != null) socials.setFacebook(body.getFacebook());
        if (body.getInstagram() != null) socials.setInstagram(body.getInstagram());
        if (body.getYoutube() != null) socials.setYoutube(body.getYoutube());
        if (body.getTwitch() != null) socials.setTwitch(body.getTwitch());
        if (body.getSteam() != null) socials.setSteam(body.getSteam());
        if (body.getReddit() != null) socials.setReddit(body.getReddit());
        if (body.getLinkedin() != null) socials.setLinkedin(body.getLinkedin());
        if (body.getTiktok() != null) socials.setTiktok(body.getTiktok());
        if (body.getSnapchat() != null) socials.setSnapchat(body.getSnapchat());
        if (body.getWhatsapp() != null) socials.setWhatsapp(body.getWhatsapp());
        if (body.getSoundcloud() != null) socials.setSoundcloud(body.getSoundcloud());
        if (body.getSpotify() != null) socials.setSpotify(body.getSpotify());
        if (body.getThreads() != null) socials.setThreads(body.getThreads());
        if (body.getEmail() != null) socials.setEmail(body.getEmail());
        if (body.getMessenger() != null) socials.setMessenger(body.getMessenger());

        uploader.setSocials(socials);
        userService.saveUser(uploader);
        auditLogService.saveLog(PortalLogType.USER_UPDATE_PROFILE, uploader, "Social profiles updated", "PORTAL");

        return new ResponseEntity<>(new DefaultResponse(false, "Updated"), HttpStatus.NO_CONTENT);
    }

    @GetMapping(
            value = "/get/@me",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> getCurrentUser(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        log.info("Getting current user: {}", uploader.getUsername());

        UserDto userDto = userMapper.apply(uploader);
        return ResponseEntity.ok()
                .body(new DefaultResponse(false, userDto));
    }

    @GetMapping(
            value = "/get/@me/settings/webhook",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> getCurrentUserSettingsWebhook(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        log.info("Getting current user: {}", uploader.getUsername());

        Optional<UserSettings> userSettings = userSettingsService.getUserSettingsByUserId(uploader.getId());
        if (userSettings.isEmpty()) {
            userSettings = userSettingsService.createDefaultSettingsForUser(uploader);
        }
        UserSettingsDto userSettingsDto = userSettingsMapper.apply(userSettings.orElseThrow(() -> new ResourceNotFoundException("User settings not found")));

        return ResponseEntity.ok()
                .body(new DefaultResponse(false, userSettingsDto.webhookSettings()));
    }

    @PatchMapping(
            value = "/get/@me/settings/webhook",
            consumes = "application/json",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> patchCurrentUserSettingsWebhook(
            HttpServletRequest request,
            @RequestBody UserWebhookSettings body
    ) {
        User uploader = (User) request.getAttribute("uploader");

        Optional<UserSettings> existingSettings = userSettingsService.getUserSettingsByUserId(uploader.getId());

        log.info("uploader is :: {}", uploader.getUsername());
        log.info("exists? :: {}", existingSettings.isPresent());

        log.info("PATH body is :: {}", body);
        log.info("body.getColor() is :: {}", body.getColor());

        if (existingSettings.isEmpty()) {
            existingSettings = userSettingsService.createDefaultSettingsForUser(uploader);
        }

        UserSettings userSettings = existingSettings.orElseThrow(() -> new ResourceNotFoundException("User settings not found"));


        if (body.getColor() != null)
            userSettings.getEmbedSettings().setColor(body.getColor());

        if (body.getDescription() != null)
            userSettings.getEmbedSettings().setDescription(body.getDescription());

        if (body.getTitle() != null)
            userSettings.getEmbedSettings().setTitle(body.getTitle());

        if (body.getTitleUrl() != null)
            userSettings.getEmbedSettings().setTitleUrl(body.getTitleUrl());

        if (body.getEnabled() != null)
            userSettings.getEmbedSettings().setEnabled(body.getEnabled());

        if (body.getAuthorName() != null)
            userSettings.getEmbedSettings().setAuthorName(body.getAuthorName());

        userSettingsService.saveUserSettings(userSettings);


        return new ResponseEntity<>(new DefaultResponse(false, "Updated"), HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(
            value = "/get/@me",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> deleteUser(
            HttpServletRequest request,
            @RequestParam(required = true) String password
    ) {
        User uploader = (User) request.getAttribute("uploader");
        boolean matches = passwordEncoder.matches(password, uploader.getPassword());
        if (!matches) {
            return new ResponseEntity<>(new DefaultResponse(true, "Invalid password"), HttpStatus.UNAUTHORIZED);
        }

        userService.deleteById(uploader.getId());

        return new ResponseEntity<>(new DefaultResponse(false, "User deleted"), HttpStatus.OK);
    }

    @GetMapping(
            value = "/get/@me/settings/url",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> getCurrentUserSettingsUrlPreferences(
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");

        UserSettings settings = userSettingsService.getUserSettingsByUserIdSafe(uploader);

        return new ResponseEntity<>(new DefaultResponse(false, settings.getUrlSettings()), HttpStatus.OK);
    }

    @PatchMapping(
            value = "/get/@me/settings/url",
            consumes = "application/json",
            produces = "application/json"
    )
    @RequiresApiKey
    public ResponseEntity<?> patchCurrentUserSettingsUrlPreferences(
            HttpServletRequest request,
            @RequestBody Map<SegmentType, UrlSetPreference> body
    ) {
        User uploader = (User) request.getAttribute("uploader");

        UserSettings settings = userSettingsService.getUserSettingsByUserIdSafe(uploader);

        if (body.containsKey(SegmentType.IMAGE)) settings.getUrlSettings().setImage(body.get(SegmentType.IMAGE));
        if (body.containsKey(SegmentType.PASTE)) settings.getUrlSettings().setPaste(body.get(SegmentType.PASTE));
        if (body.containsKey(SegmentType.URL)) settings.getUrlSettings().setUrl(body.get(SegmentType.URL));

        if (body.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No settings to update"), HttpStatus.NOT_MODIFIED);
        }

        userSettingsService.saveUserSettings(settings);

        return new ResponseEntity<>(new DefaultResponse(false, "Updated"), HttpStatus.NO_CONTENT);
    }

    @GetMapping(
            value = "/get/{username}",
            produces = "application/json"
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUserByUsername(
            @PathVariable String username
    ) {

        log.info("Getting user by username: {}", username);
        UserDto userDto;
        boolean isInteger = isInteger(username);
        boolean containsAt = username.contains("@");
        try {
            if (!isInteger) {
                if (!containsAt)
                    userDto = userService.findByUsername(username);
                else
                    userDto = userService.findByEmail(username);
            } else userDto = userService.findById(Long.parseLong(username))
                    .map(userMapper)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DefaultResponse(true, e.getMessage()));
        }

        return ResponseEntity.ok()
                .body(new DefaultResponse(false, userDto));

        /*User user = userService.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);*/
    }

    private static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    private static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
}
