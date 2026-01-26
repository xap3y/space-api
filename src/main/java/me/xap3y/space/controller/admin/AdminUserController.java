package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.*;
import me.xap3y.space.mapper.ShortUserMapper;
import me.xap3y.space.mapper.UserMapper;
import me.xap3y.space.model.request.UserImagesRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.service.UrlService;
import me.xap3y.space.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return new ResponseEntity<>(new DefaultResponse(true, "No images found for this user UID"), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(new DefaultResponse(false, imageDtos, count));
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
            return new ResponseEntity<>(new DefaultResponse(true, "No pastes found for this UID"), HttpStatus.NOT_FOUND);
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
            return new ResponseEntity<>(new DefaultResponse(true, "No short urls found for this UID"), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(new DefaultResponse(false, urlsDtos));
    }
}
