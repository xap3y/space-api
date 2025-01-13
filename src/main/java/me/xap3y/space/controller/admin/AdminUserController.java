package me.xap3y.space.controller.admin;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.StatImageDto;
import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @PathVariable Long uid
    ) {

        List<StatImageDto> imageDtos = imageService.getAllImagesByUser(uid);
        if (imageDtos.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No images found for this UID"), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(new DefaultResponse(false, imageDtos));
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
            @PathVariable Long uid
    ) {

        List<UrlDto> urlsDtos = urlService.getAllUrlsByCreatorId(uid);
        if (urlsDtos.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No short urls found for this UID"), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(new DefaultResponse(false, urlsDtos));
    }
}
