package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.xap3y.space.api.exception.InternalServerException;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.StatsRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/v1/image")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;
    private final ServerInfo serverInfo;

    public ImageController(ImageService imageService, ServerInfo serverInfo) {
        this.imageService = imageService;
        this.serverInfo = serverInfo;
    }

    @PostMapping("/upload")
    @RequiresApiKey
    public ResponseEntity<?> uploadImage(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Unauthorized=test"), HttpStatus.UNAUTHORIZED);
        }

        if (file.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "File is empty"), HttpStatus.BAD_REQUEST);
        }

        try {
            Image savedImage = imageService.saveImage(file, uploader);
            String url = serverInfo.getBaseUrl() + "/v1/image/get/" + savedImage.getUniqueId();
            Map<String, Object> data = new HashMap<>() {{
                put("raw_url", url);
                put("web_url", serverInfo.getBaseUrl() + "/web/image-render/" + savedImage.getUniqueId());
                put("portal_url", serverInfo.getFrontEndUrl() + "/image/" + savedImage.getUniqueId());
            }};
            return new ResponseEntity<>(new UIDResponse(false, savedImage.getUniqueId(), data), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> deleteImage(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) throws RuntimeException, IOException {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) throw new InvalidApiKeyException();

        ImageDto image = imageService.getImage(uniqueId, false, true);
        if (image.uploader() == null) {
            throw new RuntimeException("Invalid uploader!");
        }

        if (!Objects.equals(image.uploader().getId(), uploader.getId())) {
            return new ResponseEntity<>(new DefaultResponse(true, "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        boolean deleted = imageService.deleteImageFile(uniqueId + "." + image.type().toLowerCase(Locale.ROOT));
        if (!deleted) {
            throw new InternalServerException("Falied to delete image file");
        }
        imageService.deleteByUniqueId(uniqueId);
        return new ResponseEntity<>(new DefaultResponse(false, "Image deleted"), HttpStatus.OK);
    }

    @GetMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE,
                    MediaType.IMAGE_PNG_VALUE,
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_GIF_VALUE
            }
    )
    public ResponseEntity<?> getImageBase64(
            @PathVariable String uniqueId,
            @RequestParam(required = false, defaultValue = "false", value = "base64") boolean valBool,
            @RequestParam(required = false, defaultValue = "false", value = "raw") boolean rawData,
            @RequestParam(required = false, defaultValue = "false", value = "uploader_info") boolean getUserInfo,
            @RequestParam(required = false, defaultValue = "false", value = "image_info") boolean imageInfo,
            @RequestParam(required = false, defaultValue = "false", value = "info") boolean onlyInfo
    ) {
        HttpHeaders headers = new HttpHeaders();
        ImageDto image;
        Map<String, Object> data = new HashMap<>();

        try {
            image = imageService.getImage(uniqueId, valBool, getUserInfo);
        } catch (ResourceNotFoundException | IOException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return new ResponseEntity<>(new UIDResponse(true, uniqueId, "Image not found"), headers, HttpStatus.NOT_FOUND);
        }

        if (getUserInfo) {
            data.put("uploader", image.uploader().getUsername());
            headers.add("X-Uploader", image.uploader().getUsername());
        }

        if (imageInfo) {
            data.put("image_size", image.size());
            data.put("image_type", image.type());
            headers.add("X-Image-Size", String.valueOf(image.size()));
            headers.add("X-Image-Type", image.type());
        }

        if (valBool) {
            headers.add(HttpHeaders.CONTENT_TYPE, (rawData) ? MediaType.TEXT_PLAIN_VALUE : MediaType.APPLICATION_JSON_VALUE);
            if (rawData)
                return new ResponseEntity<>(image.base64(), headers, HttpStatus.OK);

            data.put("base64", image.base64());
            return new ResponseEntity<>(new DefaultResponse(false, data), headers, HttpStatus.OK);
        } else if (onlyInfo) {
            return new ResponseEntity<>(new DefaultResponse(false, data), headers, HttpStatus.OK);
        }

        if (image.type().contains("png"))
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE);
        else if (image.type().contains("gif"))
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_GIF_VALUE);
        else if (image.type().contains("jpeg") || image.type().contains("jpg"))
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE);
        else if (image.type().contains("mp4"))
            headers.add(HttpHeaders.CONTENT_TYPE, "video/mp4");
        else if (image.type().contains("webp") || image.type().contains("webm"))
            headers.add(HttpHeaders.CONTENT_TYPE, "image/webp");
        else
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return new ResponseEntity<>(image.bytes(), headers, HttpStatus.OK);
    }

    @GetMapping(
            value = "/stats",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            },
            consumes = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<?> getImageStats(
            @RequestBody(required = false) StatsRequest statsRequest
    ) {
        if (statsRequest == null) {
            LocalDate date = LocalDate.now().minusYears(1);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = LocalDateTime.now();
            statsRequest = new StatsRequest(startOfDay, endOfDay);
        }
        Map<String, Object> stats = imageService.getStats(statsRequest.getFromDate(), statsRequest.getToDate());
        if (stats == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "No images found"), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(new DefaultResponse(false, stats), HttpStatus.OK);
    }

    /*@GetMapping(
            value = "/render/{uniqueId}",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE }
    )
    public ResponseEntity<byte[]> getImage(
            @PathVariable String uniqueId
    ) {

        if (!imageService.doesImageExist(uniqueId)) {
            log.info("Image with id: {} not found", uniqueId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            byte[] image = imageService.getImageBytes(uniqueId);
            log.info("Image with id: {} ({})", uniqueId, image.length);
            return new ResponseEntity<>(image, HttpStatus.OK);
        } catch (Exception e) {
            log.info("Cannot get image with id: {}", uniqueId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }*/
}
