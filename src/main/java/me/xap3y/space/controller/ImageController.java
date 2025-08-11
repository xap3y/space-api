package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.ArchiveType;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.exception.*;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.FoundImageDto;
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ImageInfoMapper;
import me.xap3y.space.mapper.ImageMapper;
import me.xap3y.space.model.ImageGetRequest;
import me.xap3y.space.model.StatsRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.MetricService;
import me.xap3y.space.service.TelegramService;
import me.xap3y.space.service.WebhookService;
import me.xap3y.space.util.Utils;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

@Slf4j
@RestController
@RequestMapping("/v1/image")
public class ImageController {

    private final ImageService imageService;
    private final ServerInfo serverInfo;
    private final WebhookService webhookService;
    private final MetricService metricService;
    private final ImageInfoMapper imageInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final TelegramService telegramService;
    private final ImageMapper imageMapper;

    public ImageController(ImageService imageService, ServerInfo serverInfo, WebhookService webhookService, MetricService metricService, ImageInfoMapper imageInfoMapper, PasswordEncoder passwordEncoder, TelegramService telegramService, ImageMapper imageMapper) {
        this.imageService = imageService;
        this.serverInfo = serverInfo;
        this.webhookService = webhookService;
        this.metricService = metricService;
        this.imageInfoMapper = imageInfoMapper;
        this.passwordEncoder = passwordEncoder;
        this.telegramService = telegramService;
        this.imageMapper = imageMapper;
    }

    @PostMapping("/upload/zip")
    @RequiresApiKey
    public ResponseEntity<?> uploadArchive(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) throw new InvalidApiKeyException();
        if (file.isEmpty()) return new ResponseEntity<>(new DefaultResponse(true, "File is empty"), HttpStatus.BAD_REQUEST);

        try (InputStream is = file.getInputStream()) {
            byte[] magic = new byte[4];
            if (is.read(magic) != 4 || !(magic[0] == 0x50 && magic[1] == 0x4B && magic[2] == 0x03 && magic[3] == 0x04)) {
                throw new BadRequestException("File is not a valid ZIP archive");
            }
        } catch (IOException e) {
            throw new InternalServerException("Failed to read uploaded archive file");
        }

        List<FoundImageDto> foundImages;

        try {
            foundImages = Utils.extractFoundImages(file, ArchiveType.ZIP);
        } catch (ZipException e) {
            return ResponseEntity.badRequest().body("Uploaded file is not a valid ZIP archive");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error processing archive: " + e.getMessage());
        }

        if (foundImages.isEmpty()) {
            return ResponseEntity.ok("No supported media files found in the archive");
        }

        // Upload each found image
        /*for (FoundImageDto dto : foundImages) {

        }*/

        return ResponseEntity.ok("Found image files:\n" + foundImages);
    }

    @PostMapping("/register")
    @RequiresApiKey
    public ResponseEntity<?> registerImage(
            HttpServletRequest request,
            @RequestParam(value = "uniqueId", required = false) String uniqueId,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "size", required = false) Long size,
            @RequestParam(value = "private", required = false) Boolean isPrivate,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "desc", required = false) String description
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) throw new InvalidApiKeyException();

        if (uniqueId == null || uniqueId.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "Unique ID is required"), HttpStatus.BAD_REQUEST);
        }

        boolean isPublic = isPrivate == null || !isPrivate;
        String pass = (password == null) ? null : passwordEncoder.encode(password);

        log.info("Size is {}", size);

        try {
            Image savedImage = imageService.registerImage(uploader, uniqueId, pass, isPublic, description, fileType, size);

            ImageInfoDto imageInfoDto = imageMapper.apply(savedImage);

            return new ResponseEntity<>(new UIDResponse(false, imageInfoDto.uniqueId(), imageInfoDto), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error registering image: {}", e.getMessage());
            return new ResponseEntity<>(new DefaultResponse(true, "Failed to register image"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
        if (uploader == null) throw new InvalidApiKeyException();

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
                    savedImage.getIsPublic(),
                    savedImage.getLocation()
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
            telegramService.sendImageUrl("5759660343", imageInfoDto);
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
        webhookService.postImageDeleted(uniqueId, image);
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
                    MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    "video/mp4",
                    "image/heif",
                    "image/heic"
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

        if (!Files.exists(image.path())) {
            return ResponseEntity.notFound().build();
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
            Resource resource = new FileSystemResource(image.path());
            String contentType = switch (image.type().toLowerCase(Locale.ROOT)) {
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                case "gif" -> "image/gif";
                case "mp4" -> "video/mp4";
                case "heif" -> "image/heif";
                case "heic" -> "image/heic";
                default -> "application/octet-stream";
            };
            headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uniqueId + "." + image.type().toLowerCase(Locale.ROOT) + "\"");
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        }

        if (image.uploader() != null) {
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

        long fileLength = Files.size(image.path());

        String mimeType = Files.probeContentType(image.path());

        MediaType contentType = MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream");
        headers.setContentType(contentType);
        log.info("MIME type for {}: {}", image.path(), mimeType);
        log.info("MIME type for {}: {}", image.path(), mimeType);

        if (contentType.getType().toLowerCase(Locale.ROOT).startsWith("video")) {
            log.info("Redirecting to video endpoint for {}", uniqueId);
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .location(URI.create(serverInfo.getBaseUrl() + "/v1/image/get/video/" + uniqueId))
                    .build();
        }

        headers.setContentLength(fileLength);

        headers.set(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(Duration.ofHours(12)).getHeaderValue());
        InputStreamResource fileResource = new InputStreamResource(Files.newInputStream(image.path()));
        return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
    }

    @GetMapping(
            value = "/get/video/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE,
                    MediaType.IMAGE_PNG_VALUE,
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_GIF_VALUE,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    "video/mp4",
                    "image/heif",
                    "image/heic",
                    "video/x-matroska",
                    "video/webm",
                    "video/quicktime"
            }
    ) public ResponseEntity<StreamingResponseBody> getVideo(
            @PathVariable String uniqueId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request
    ) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        NewImageDto image;

        try {
            image = imageService.getImageStream(uniqueId, false, true);
        } catch (ResourceNotFoundException | IOException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return ResponseEntity.notFound().build();
        }

        if (!Files.exists(image.path())) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = Files.size(image.path());

        String mimeType = Files.probeContentType(image.path());

        headers.setContentType(MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream"));
        log.info("MIME type for {}: {}", image.path(), mimeType);
        headers.setContentLength(fileLength);

        if (mimeType != null && mimeType.contains("x-matroska")) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + uniqueId + "." + image.type().toLowerCase(Locale.ROOT) + "\"");
        }
        //headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        log.info("Range header: {}", rangeHeader);
        log.info("File size: {}", fileLength);

        if (rangeHeader == null) {
            // No Range header → full file with 200 OK
            headers.setContentLength(fileLength);

            StreamingResponseBody stream = outputStream -> {
                streamRange(image.path(), 0, fileLength - 1, outputStream);
            };

            return new ResponseEntity<>(stream, headers, HttpStatus.OK);
        }

        List<Range> ranges = parseRanges(rangeHeader, fileLength);
        if (ranges == null || ranges.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        if (ranges.size() > 1) {
            log.info("USING RANGE > 1");
            String boundary = UUID.randomUUID().toString();
            headers.setContentType(
                    MediaType.parseMediaType("multipart/byteranges; boundary=" + boundary));

            StreamingResponseBody stream = outputStream -> {
                for (Range r : ranges) {
                    // Boundary
                    outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                    // Part headers
                    outputStream.write(("Content-Type: " + mimeType + "\r\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.write(("Content-Range: bytes " + r.start + "-" + r.end + "/" + fileLength + "\r\n\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    streamRange(image.path(), r.start, r.end, outputStream);
                    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                }
                outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            };

            return new ResponseEntity<>(stream, headers, HttpStatus.PARTIAL_CONTENT);
        }

        Range r = ranges.get(0);

        headers.setContentLength(r.length());
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + r.start + "-" + r.end + "/" + fileLength);
        headers.set(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(Duration.ofHours(12)).getHeaderValue());

        StreamingResponseBody stream = outputStream -> {
            streamRange(image.path(), r.start, r.end, outputStream);
        };

        return new ResponseEntity<>(stream, headers, HttpStatus.PARTIAL_CONTENT);
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

    private void streamRange(Path filePath, long start, long end, OutputStream out) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(start);
            byte[] buf = new byte[8192];
            long toRead = end - start + 1;
            while (toRead > 0) {
                int len = raf.read(buf, 0, (int)Math.min(buf.length, toRead));
                if (len == -1) break;
                out.write(buf, 0, len);
                toRead -= len;
            }
        }
    }

    /**
     * Helper to parse single or multiple range headers.
     */
    private List<Range> parseRanges(String rangeHeader, long fileLength) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) return null;
        String[] parts = rangeHeader.substring(6).split(",");
        List<Range> result = new ArrayList<>();
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String[] split = part.split("-");
            try {
                long start = split[0].isEmpty() ? -1 : Long.parseLong(split[0]);
                long end = (split.length > 1 && !split[1].isEmpty()) ? Long.parseLong(split[1]) : -1;
                if (start == -1 && end == -1) continue;
                if (start == -1) {
                    start = fileLength - end;
                    end = fileLength - 1;
                } else if (end == -1 || end >= fileLength) {
                    end = fileLength - 1;
                }
                if (start > end || start < 0) continue;
                result.add(new Range(start, end));
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private static class Range {
        final long start;
        final long end;
        Range(long start, long end) { this.start = start; this.end = end; }
        long length() { return end - start + 1; }
    }
}
