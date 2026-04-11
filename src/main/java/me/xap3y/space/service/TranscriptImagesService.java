package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.api.exception.InvalidUniqueIdException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.entity.MinecraftServerReports;
import me.xap3y.space.entity.TranscriptImage;
import me.xap3y.space.repository.TranscriptImagesRepository;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class TranscriptImagesService {

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

    public TranscriptImage saveTranscriptImageFromUrl(String url, MinecraftServerReports uploader, ResourceSourceType type) {
        String uniqueId = Utils.generateRandomId();
        String cleanUrl = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
        String fileExtension = cleanUrl.substring(cleanUrl.lastIndexOf(".") + 1).toLowerCase();
        // save from URL to file under the generated uniqueId
        String fileNameWithExtension = uniqueId + "." + fileExtension;
        Path folder = Paths.get(ConfigDb.TRANSCRIPT_IMAGE_DIR);
        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
        } catch (IOException e) {
            log.error("Failed to create transcript image directory: {}", folder.toAbsolutePath(), e);
            throw new RuntimeException("Failed to create transcript image directory");
        }
        File imageFile = new File(ConfigDb.TRANSCRIPT_IMAGE_DIR, fileNameWithExtension);
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Transcript image downloaded from URL: {}", url);
            return registerImage(uploader, uniqueId,fileExtension, imageFile.length(), ImageLocation.LOCAL, type);
        } catch (Exception e) {
            log.error("Failed to download transcript image from URL: {}", url, e);
            throw new RuntimeException("Failed to download transcript image from URL");
        }
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
