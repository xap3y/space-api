package me.xap3y.space.controller.admin;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.StatImageDto;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ImageService;
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

    public AdminUserController(ImageService imageService) {
        this.imageService = imageService;
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
}
