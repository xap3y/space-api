package me.xap3y.space.controller;

import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.dto.JsonResponse;
import me.xap3y.space.entity.Image;
import me.xap3y.space.service.ApiKeyService;
import me.xap3y.space.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;

@RestController
@RequestMapping("/v1/image")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;
    private final ApiKeyService apiKeyService;

    public ImageController(ImageService imageService, ApiKeyService apiKeyService) {
        this.imageService = imageService;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/upload")
    public ResponseEntity<JsonResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-API-Key") String apiKey
    ) {

        try {
            apiKeyService.validateApiKey(apiKey);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid API Key")) {
                return new ResponseEntity<>(new JsonResponse(true, "Invalid API Key!"), HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (file.isEmpty()) {
            return new ResponseEntity<>(new JsonResponse(true, "File is empty"), HttpStatus.BAD_REQUEST);
        }

        try {
            Image savedImage = imageService.saveImage(file, apiKey);
            String url = "https://api.xap3y.tech/v1/image/get/" + savedImage.getUniqueId();
            return new ResponseEntity<>(new JsonResponse(false, savedImage.getUniqueId(), url), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
    public ResponseEntity<Object> getImageBase64(
            @PathVariable String uniqueId,
            @RequestParam(required = false, defaultValue = "false", value = "base64") boolean valBool,
            @RequestParam(required = false, defaultValue = "false", value = "raw") boolean rawData,
            @RequestParam(required = false, defaultValue = "false", value = "uploader_info") boolean getUserInfo,
            @RequestParam(required = false, defaultValue = "false", value = "image_info") boolean imageInfo
    ) {
        HttpHeaders headers = new HttpHeaders();

        ImageDto image;

        try {
            image = imageService.getImage(uniqueId, valBool, getUserInfo);
        } catch (FileNotFoundException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return new ResponseEntity<>(new JsonResponse(true, "Image not found"), headers, HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return new ResponseEntity<>(new JsonResponse(true, "Failed to get image"), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (getUserInfo) {
            headers.add("X-Uploader", image.uploader().getUsername());
        }

        if (imageInfo) {
            headers.add("X-Image-Size", String.valueOf(image.size()));
            headers.add("X-Image-Type", image.type());
        }

        if (valBool) {
            headers.add(HttpHeaders.CONTENT_TYPE, (rawData) ? MediaType.TEXT_PLAIN_VALUE : MediaType.APPLICATION_JSON_VALUE);
            if (rawData)
                return new ResponseEntity<>(image.base64(), headers, HttpStatus.OK);

            return new ResponseEntity<>(new JsonResponse(false, image.base64()), headers, HttpStatus.OK);
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
