package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.*;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ShortUserMapper;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.ImageListResponse;
import me.xap3y.space.model.response.PackListResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.service.UrlService;
import me.xap3y.space.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/admin/user")
@AllArgsConstructor
public class AdminUserController {

    private final ImageService imageService;
    private final PasteService pasteService;
    private final UrlService urlService;
    private final UserService userService;
    private final ShortUserMapper shortUserMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping(
            value = "/get",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getAllUsers(
            HttpServletRequest request
    ) {

        List<UserDto> usersDto = userService.getAllUsers()
                .stream()
                .map(u -> userMapper.apply(u, false, false, false))
                .toList();

        return ResponseEntity.ok(new DefaultResponse(false, usersDto, usersDto.size()));
    }

    /*@GetMapping(
            value = "/{uid}/images",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUserImages(
            HttpServletRequest request,
            @PathVariable("uid") Long uid,
*//*            @RequestBody(required = false) UserImagesRequest body,*//*
            @RequestParam(value = "from", required = false) Long from,
            @RequestParam(value = "to", required = false) Long to,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        List<ImageInfoDto> imageDtos = imageService.getAllImagesByUser(uid, from, to, limit);
        int count = imageService.countByUploaderId(uid);

        if (imageDtos.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No images found for this user UID"), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(new DefaultResponse(false, imageDtos, count));
    }*/

    @GetMapping(
            value = "/{uid}/images",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUserImages(
            HttpServletRequest request,
            @PathVariable("uid") Long uid,
            /*            @RequestBody(required = false) UserImagesRequest body,*/
            @RequestParam(value = "from", required = false) Long from,
            @RequestParam(value = "to", required = false) Long to,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        List<ImageInfoDto> imageDtos = imageService.getAllImagesByUser(uid, from, to, limit);
        int count = imageService.countByUploaderId(uid);

        if (imageDtos.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        return ResponseEntity.ok(new DefaultResponse(false, imageDtos, count));
    }

    @GetMapping(
            value = "/{uid}/images/pageable",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUserImagesPageable(
            HttpServletRequest request,
            @PathVariable("uid") Long uid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> formats
    ) {
        Page<PageImage> imageDtos = imageService.getAllImagesByUser(uid, page, size, from, to, formats);

        if (imageDtos.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        ImageListResponse list = new ImageListResponse(
                imageDtos.getContent(),
                imageDtos.getTotalElements(),
                imageDtos.getTotalPages(),
                imageDtos.getNumber(),
                imageDtos.getSize()
        );

        return ResponseEntity.ok(new DefaultResponse(false, list, imageDtos.getContent().size()));
    }

    @GetMapping(
            value = "/{uid}/pastes",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUserPastes(
            @PathVariable Long uid,
            @RequestParam(value = "content", required = false, defaultValue = "false") boolean content
    ) {

        List<PasteDto> pastesDtos = pasteService.getAllPastesByUserId(uid, content);
        if (pastesDtos.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        return ResponseEntity.ok(new DefaultResponse(false, pastesDtos));
    }

    @GetMapping(
            value = "/{uid}/urls",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUserUrls(
            @RequestParam(value = "logs", required = false, defaultValue = "false") boolean logs,
            @PathVariable Long uid
    ) {

        List<ShortUrlDto> urlsDtos = urlService.getAllShortUrlsByCreatorId(uid, logs);
        if (urlsDtos.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        return ResponseEntity.ok(new DefaultResponse(false, urlsDtos));
    }

    @PutMapping(
            value = "/{uid}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> updateUser(
            @PathVariable("uid") Long uid,
            @RequestBody UserUpdateRequest body,
            HttpServletRequest request
    ) {
        User user = userService.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newEmail = body.getEmail() != null ? body.getEmail() : body.getMail();
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userService.existsByEmail(newEmail)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new DefaultResponse(true, "Email already in use"));
            }
            user.setEmail(newEmail);
        }

        String newAvatar = body.getAvatar() != null ? body.getAvatar() : body.getProfilePicUrl();
        if (newAvatar != null) {
            user.setAvatar(newAvatar);
        }

        if (body.getRole() != null) {
            user.setRole(body.getRole());
        }

        if (body.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(body.getPassword()));
        }

        userService.saveUser(user);

        return ResponseEntity.ok(new DefaultResponse(false, "User updated successfully"));
    }

    @PostMapping(
            value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresSpecialApiKey
    public ResponseEntity<?> adminCreateUser(
            @RequestBody AdminCreateUserRequest body,
            HttpServletRequest request
    ) {
        if (body.getUsername() == null || body.getUsername().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse(true, "Username is required"));
        }
        if (body.getEmail() == null || body.getEmail().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse(true, "Email is required"));
        }
        if (body.getPassword() == null || body.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse(true, "Password is required"));
        }

        if (userService.existsByUsername(body.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse(true, "Username is already taken"));
        }
        if (userService.existsByEmail(body.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse(true, "Email is already taken"));
        }

        try {
            userService.registerAdminCreatedUser(
                    body.getEmail().trim(),
                    body.getUsername().trim(),
                    body.getPassword(),
                    body.getVerified() != null && body.getVerified()
            );
            return ResponseEntity.ok(new DefaultResponse(false, "User created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DefaultResponse(true, "Failed to create user: " + e.getMessage()));
        }
    }
}
