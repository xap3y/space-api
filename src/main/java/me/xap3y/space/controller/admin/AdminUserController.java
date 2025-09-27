package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.model.request.UserImagesRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/admin/user")
public class AdminUserController {

    private final ImageService imageService;
    private final PasteService pasteService;
    private final UrlService urlService;

    public AdminUserController(ImageService imageService, PasteService pasteService, UrlService urlService) {
        this.imageService = imageService;
        this.pasteService = pasteService;
        this.urlService = urlService;
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
            @PathVariable Long uid
    ) {

        List<PasteDto> pastesDtos = pasteService.getAllPastesByUserId(uid);
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
