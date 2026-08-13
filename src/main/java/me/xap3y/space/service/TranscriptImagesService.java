package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.InvalidUniqueIdException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.entity.MinecraftServerReports;
import me.xap3y.space.entity.TranscriptImage;
import me.xap3y.space.repository.TranscriptImagesRepository;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class TranscriptImagesService {

    private static final int DOWNLOAD_TIMEOUT_MS = 30_000;
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/webp", "webp"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/webm", "webm"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/ogg", "ogg"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("text/plain", "txt"),
            Map.entry("application/zip", "zip")
    );

    private final TranscriptImagesRepository transcriptImagesRepository;
    private final PrometheusMetricService prometheusMetricService;
    private final AuditLogService auditLogService;

    public TranscriptImage save(TranscriptImage transcriptImage) {
        return transcriptImagesRepository.save(transcriptImage);
    }

    public long count() {
        return transcriptImagesRepository.count();
    }

    List<TranscriptImage> getAllByUploaderId(Long uploaderId) {
        return transcriptImagesRepository.getAllByUploader_Id(uploaderId);
    }

    public TranscriptImage getByUniqueIdStrict(String uniqueId) {
        return transcriptImagesRepository.findByUniqueId(uniqueId).orElseThrow(() -> new ResourceNotFoundException("Transcript image with unique ID " + uniqueId + " not found"));
    }

    Optional<TranscriptImage> getByUniqueId(String uniqueId) {
        return transcriptImagesRepository.findByUniqueId(uniqueId);
    }

    public TranscriptImage registerImage(MinecraftServerReports uploader, String uniqueId, String fileType, Long size, ImageLocation location, ResourceSourceType source) throws IOException, RuntimeException {
        if (uniqueId == null) {
            uniqueId = Utils.generateRandomId();
        } else {
            if (!uniqueId.matches("^[a-zA-Z0-9]*$")) throw new InvalidUniqueIdException();
        }

        TranscriptImage imageDto = new TranscriptImage();
        imageDto.setSource(source);
        imageDto.setUniqueId(uniqueId);
        imageDto.setLocation(location);
        imageDto.setFileType(fileType);
        imageDto.setSize(size != null ? size : 0L);
        imageDto.setUploader(uploader);

        prometheusMetricService.recordEvent(MetricRecordType.IMAGE_UPLOAD);
        auditLogService.saveLog(PortalLogType.TRANSCRIPT_IMAGE_UPLOAD, null, uniqueId, source.toString());

        return transcriptImagesRepository.save(imageDto);
    }

    public TranscriptImage saveTranscriptImageFromUrl(
            String url,
            String originalFilename,
            String contentType,
            MinecraftServerReports uploader,
            ResourceSourceType type
    ) {
        String uniqueId = Utils.generateRandomId();
        URI sourceUri;
        try {
            sourceUri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid attachment URL");
        }
        if (!"https".equalsIgnoreCase(sourceUri.getScheme())) {
            throw new BadRequestException("Attachment URL must use HTTPS");
        }
        String sourceHost = sourceUri.getHost() == null ? "" : sourceUri.getHost().toLowerCase(Locale.ROOT);
        boolean discordCdn = sourceHost.equals("cdn.discordapp.com") || sourceHost.equals("media.discordapp.net");
        if (!discordCdn || sourceUri.getPath() == null || !sourceUri.getPath().startsWith("/attachments/")) {
            throw new BadRequestException("Only Discord attachment URLs are supported");
        }

        String fileExtension = resolveExtension(originalFilename, sourceUri.getPath(), contentType);
        Path folder = Paths.get(ConfigDb.TRANSCRIPT_IMAGE_DIR);
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            log.error("Failed to create transcript image directory: {}", folder.toAbsolutePath(), e);
            throw new RuntimeException("Failed to create transcript image directory");
        }

        Path attachmentPath = folder.resolve(uniqueId + "." + fileExtension);
        try {
            URLConnection connection = sourceUri.toURL().openConnection();
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Space-Transcript-Attachment/1.0");
            if (connection instanceof HttpURLConnection httpConnection) {
                httpConnection.setInstanceFollowRedirects(false);
            }

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, attachmentPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Transcript attachment downloaded from {}", sourceHost);
            return registerImage(
                    uploader,
                    uniqueId,
                    fileExtension,
                    Files.size(attachmentPath),
                    ImageLocation.LOCAL,
                    type
            );
        } catch (Exception e) {
            try {
                Files.deleteIfExists(attachmentPath);
            } catch (IOException cleanupError) {
                log.warn("Failed to clean up attachment file {}", attachmentPath, cleanupError);
            }
            log.error("Failed to download transcript attachment from {}", sourceHost, e);
            throw new RuntimeException("Failed to download transcript attachment from URL", e);
        }
    }

    private String resolveExtension(String originalFilename, String urlPath, String contentType) {
        String fromFilename = extensionOf(originalFilename);
        if (fromFilename != null) return fromFilename;

        String fromPath = extensionOf(urlPath);
        if (fromPath != null) return fromPath;

        if (contentType != null) {
            String normalizedContentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            String knownExtension = CONTENT_TYPE_EXTENSIONS.get(normalizedContentType);
            if (knownExtension != null) return knownExtension;
        }
        return "bin";
    }

    private String extensionOf(String value) {
        if (value == null || value.isBlank()) return null;
        int lastSlash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int lastDot = value.lastIndexOf('.');
        if (lastDot <= lastSlash || lastDot == value.length() - 1) return null;

        String extension = value.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,10}") ? extension : null;
    }

    public NewImageDto getImageStreamByUniqueId(String uniqueId) throws IOException, ResourceNotFoundException {
        if (uniqueId.startsWith("tr-")) {
            uniqueId = uniqueId.substring(3);
        }
        TranscriptImage transcriptImage = getByUniqueIdStrict(uniqueId);
        Path imagePath = Paths.get(ConfigDb.TRANSCRIPT_IMAGE_DIR, uniqueId + "." + transcriptImage.getFileType());
        if (!Files.exists(imagePath)) {
            throw new ResourceNotFoundException("Transcript image file not found for unique ID " + uniqueId);
        }

        return new NewImageDto(
                imagePath,
                null,
                transcriptImage.getFileType(),
                null,
                null,
                null,
                Files.size(imagePath) / 1024,
                null,
                true
        );
    }
}
