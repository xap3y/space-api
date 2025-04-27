package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.exception.*;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ImageInfoMapper;
import me.xap3y.space.model.ImageGetRequest;
import me.xap3y.space.model.StatsRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.MetricService;
import me.xap3y.space.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
    private final WebhookService webhookService;
    private final MetricService metricService;
    private final ImageInfoMapper imageInfoMapper;
    private final PasswordEncoder passwordEncoder;

    public ImageController(ImageService imageService, ServerInfo serverInfo, WebhookService webhookService, MetricService metricService, ImageInfoMapper imageInfoMapper, PasswordEncoder passwordEncoder) {
        this.imageService = imageService;
        this.serverInfo = serverInfo;
        this.webhookService = webhookService;
        this.metricService = metricService;
        this.imageInfoMapper = imageInfoMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/upload")
    @RequiresApiKey
    public ResponseEntity<?> uploadImage(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uniqueId", required = false) String uniqueId,
            @RequestParam(value = "private", required = false) Boolean isPrivate,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "desc", required = false) String description
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) {
            throw new InvalidApiKeyException();
        }

        if (file.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "File is empty"), HttpStatus.BAD_REQUEST);
        }

        if (uniqueId != null) {
            if (imageService.doesImageExist(uniqueId)) {
                return new ResponseEntity<>(new DefaultResponse(true, "Image with this UID already exists"), HttpStatus.BAD_REQUEST);
            }
        }

        if (uploader.getApiKey().getKeyCode().equals(password)) {
            throw new BadRequestException("Password cannot be the same as your API key!");
        } else if (password != null && password.length() < 3) {
            throw new BadRequestException("Password must be at least 3 characters long!");
        } else if (password != null && password.length() > 100) {
            throw new BadRequestException("Password must be at most 100 characters long!");
        } else if (password != null && !password.matches("^[a-zA-Z0-9]*$")) {
            throw new BadRequestException("Password can only contain alphanumeric characters!");
        } else if (description != null && description.length() > 200) {
            throw new BadRequestException("Description must be at most 200 characters long!");
        }

        boolean isPublic = isPrivate == null || !isPrivate;
        String pass = (password == null) ? null : passwordEncoder.encode(password);

        try {
            Image savedImage = imageService.saveImage(file, uploader, uniqueId, pass, isPublic, description);
            ImageDto imgDto = new ImageDto(
                    null,
                    savedImage.getUploader(),
                    savedImage.getDescription(),
                    savedImage.getFileType(),
                    savedImage.getPassword(),
                    savedImage.getSize(),
                    null,
                    savedImage.getUploadTime(),
                    savedImage.getExpirationTime(),
                    savedImage.getIsPublic()
            );

            ImageInfoDto imageInfoDto = imageInfoMapper.apply(Pair.of(savedImage.getUniqueId(), imgDto));
            /*String url = serverInfo.getBaseUrl() + "/v1/image/get/" + savedImage.getUniqueId();
            Map<String, Object> data = new HashMap<>() {{
                put("raw_url", url);
                put("web_url", serverInfo.getBaseUrl() + "/web/image-render/" + savedImage.getUniqueId());
                put("short_url", serverInfo.getShortImageUrl() + "/" + savedImage.getUniqueId());
                put("portal_url", serverInfo.getFrontEndUrl() + "/image/" + savedImage.getUniqueId());
            }};*/
            metricService.setDatabaseUpdated(true);
            metricService.setSessionImagesUploaded(metricService.getSessionImagesUploaded() + 1);
            webhookService.postImageUpload( imageInfoDto.uniqueId(), imageInfoDto);
            return new ResponseEntity<>(new UIDResponse(false, imageInfoDto.uniqueId(), imageInfoDto), HttpStatus.OK);
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

        ImageDto image = imageService.getImage(uniqueId, false, true, false);
        if (image.uploader() == null) {
            throw new RuntimeException("Invalid uploader!");
        }

        if (!Objects.equals(image.uploader().getId(), uploader.getId()) &&
                (uploader.getRole() == UserRole.USER
                        || uploader.getRole() == UserRole.GUEST
                        || uploader.getRole() == UserRole.TESTER
                )
        ) {
            throw new InvalidApiKeyException();
        }

        boolean deleted = imageService.deleteImageFile(uniqueId + "." + image.type().toLowerCase(Locale.ROOT));
        if (!deleted) {
            throw new InternalServerException("Falied to delete image file");
        }
        imageService.deleteByUniqueId(uniqueId);
        metricService.setDatabaseUpdated(true);
        //metricService.setSessionImagesUploaded(metricService.getSessionImagesUploaded() - 1);
        return new ResponseEntity<>(new DefaultResponse(false, "Image deleted"), HttpStatus.OK);
    }

    @SneakyThrows
    @GetMapping(
            value = "/info/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<?> getImageBase64(
            @PathVariable String uniqueId
    ) {

        ImageDto image = imageService.getImage(uniqueId, false, true, false);
        ImageInfoDto imageInfoDto = imageInfoMapper.apply(Pair.of(uniqueId, image));

        return ResponseEntity.ok()
                .body(new UIDResponse(false, uniqueId,imageInfoDto));
    }

    @SneakyThrows
    @PostMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> getImageBase64(
            @PathVariable String uniqueId,
            @RequestBody(required = false) ImageGetRequest body,
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");

        ImageDto image = imageService.getImage(uniqueId, false, true, false);

        if (image.password() != null && body.getPassword() == null) {
            throw new MissingCredentialsException();
        }
        else if (image.password() != null && !passwordEncoder.matches(image.password(), body.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        } else if (!image.isPublic() && (uploader == null || !Objects.equals(image.uploader().getId(), uploader.getId()))) {
            throw new ResourceVisibilityException();
        } else if (image.expiresAt() != null && LocalDateTime.now().isAfter(image.expiresAt())) {
            throw new ResourceExpiredException();
        }

        ImageInfoDto imageInfoDto = imageInfoMapper.apply(Pair.of(uniqueId, image));

        return new ResponseEntity<>(new DefaultResponse(false, imageInfoDto), HttpStatus.OK);
    }

    @SneakyThrows
    @GetMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE,
                    MediaType.IMAGE_PNG_VALUE,
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_GIF_VALUE,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE
            }
    )
    public ResponseEntity<?> getImageBase64(
            @PathVariable String uniqueId,
            @RequestParam(required = false, defaultValue = "false", value = "base64") boolean valBool,
            @RequestParam(required = false, defaultValue = "false", value = "download") boolean download,
            @RequestParam(required = false, defaultValue = "false", value = "raw") boolean rawData,
            @RequestParam(required = false, defaultValue = "false", value = "image_info") boolean imageInfo,
            @RequestParam(required = false, defaultValue = "false", value = "info") boolean onlyInfo,
            @RequestHeader(required = false, value = "X-Password") String password,
            @RequestHeader(required = false, value = "X-API-Key") String apiKey,
            HttpServletRequest request
    ) {
        User uploader = (User) request.getAttribute("uploader");
        HttpHeaders headers = new HttpHeaders();
        NewImageDto image;
        Map<String, Object> data = new HashMap<>();

        try {
            image = imageService.getImageStream(uniqueId, valBool, true);
            //headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + uniqueId + "." + image.type() + "\"");
        } catch (ResourceNotFoundException | IOException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return new ResponseEntity<>(new UIDResponse(true, uniqueId, "Image not found"), headers, HttpStatus.NOT_FOUND);
        }

        if (
                (image.password() != null || !image.isPublic()) && (uploader == null || !Objects.equals(image.uploader().getId(), uploader.getId()))
                        && !(password != null && image.password() != null && passwordEncoder.matches(password, image.password())) // TODO - PasswordEncoder
                        && !(apiKey != null && Objects.equals(image.uploader().getApiKey().getKeyCode(), apiKey))
        ) {
            throw new ResourceVisibilityException("Insufficient permissions to view this resource");
        }
        else if (image.expiresAt() != null && LocalDateTime.now().isAfter(image.expiresAt())) {
            throw new ResourceExpiredException();
        }

        if (download) {
            Resource video = new FileSystemResource(image.path());
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uniqueId + "." + image.type().toLowerCase(Locale.ROOT) + "\"");
            return new ResponseEntity<>(video, headers, HttpStatus.OK);
        }

        data.put("uploader", image.uploader().getUsername());
        headers.add("X-Uploader", image.uploader().getUsername());

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

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        long fileLength = Files.size(image.path());

        String mimeType = Files.probeContentType(image.path());

        /*if (image.type().contains("png"))
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
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);*/

        headers.setContentType(MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream"));
        headers.setContentLength(fileLength);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        if (rangeHeader == null) {
            InputStreamResource fileResource = new InputStreamResource(Files.newInputStream(image.path()));
            return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
        }

        long rangeStart = 0, rangeEnd = fileLength - 1;
        String[] ranges = rangeHeader.split("=")[1].split("-");

        try {
            if (ranges.length > 0) {
                rangeStart = Long.parseLong(ranges[0]);
            }
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            }
        } catch (NumberFormatException ex) {
            return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        if (rangeEnd > fileLength - 1) {
            rangeEnd = fileLength - 1;
        }

        long contentLength = rangeEnd - rangeStart + 1;
        headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
        headers.setContentLength(contentLength);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        InputStream inputStream = Files.newInputStream(image.path());
        inputStream.skip(rangeStart);

        return new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.PARTIAL_CONTENT);
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
            LocalDate date = LocalDate.now().minusYears(10);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = LocalDateTime.now();
            statsRequest = new StatsRequest(startOfDay, endOfDay, 0, false);
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
