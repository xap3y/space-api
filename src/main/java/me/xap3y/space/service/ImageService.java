package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.dto.StatImageDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.User;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.repository.ImageRepository;
import me.xap3y.space.repository.UserRepository;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.ImageCompressor;
import me.xap3y.space.util.Utils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
public class ImageService {

    private final String[] supportedExtensions = {"png", "jpg", "webp", "mp4", "gif", "jpeg", "bmp", "ico", "svg",
            "webm", "mkv", "mov", "avi", "wav", "flv", "wmv", "ogv", "ogg", "avif", "heic", "heif", "jfif", "jpeg", "jif",
            "tiff", "tif", "cur", "bmp", "vob", "drc", "qt", "3gp", "xbm"};

    private final ImageRepository imageRepository;
    private final ImageCompressor imageCompressor;
    private final ServerInfo serverInfo;

    public ImageService(ImageRepository imageRepository, ImageCompressor imageCompressor, ServerInfo serverInfo) {
        this.imageRepository = imageRepository;
        this.imageCompressor = imageCompressor;
        this.serverInfo = serverInfo;
    }

    public boolean deleteImageFile(String fileName) {
        try {
            Path filePath = Paths.get(ConfigDb.getIMAGE_DIR(), fileName);
            Files.delete(filePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Image saveImage(MultipartFile file, User uploader) throws IOException, RuntimeException {

        String random = Utils.generateRandomId();
        String[] fileExtension = Objects.requireNonNull(file.getOriginalFilename()).split("\\.");
        String fileNameWithExtension = random + "." + fileExtension[fileExtension.length - 1].toLowerCase();
        String fElc = fileExtension[fileExtension.length - 1].toLowerCase();

        boolean isSupported = false;
        for (String s : supportedExtensions) {
            if (s.equals(fElc)) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            throw new IOException("Unsupported file type");
        }


        log.info("Handling image with id: {}", random);
        File compressedImageFile = new File(ConfigDb.getIMAGE_DIR(), fileNameWithExtension);

        log.info("Checking image type: {}", fElc);


        if ((fElc.equals("png") || fElc.equals("jpg") || fElc.equals("webp")) && file.getSize() > 70000) {
            log.info("Compressing image with id: {} and size: {}", random, file.getSize());
            float quality = 0.9f;
            double scale = 1;

            if (file.getSize() > 990000) {
                quality = 0.35f;
                scale = 0.3;
            } else if (file.getSize() > 500000) {
                quality = 0.5f;
                scale = 0.4;
            } else {
                log.info("Using default compression settings");
            }

            imageCompressor.compressImage(file.getInputStream(), compressedImageFile, scale, quality);
        } else {
            log.info("Saving image with id: {}", random);
            byte[] bytes = file.getBytes();
            Path filePath = Paths.get(ConfigDb.getIMAGE_DIR(), fileNameWithExtension);
            Files.write(filePath, bytes);
        }

        log.info("Saving image with file name: {}", file.getOriginalFilename());
        Image imageDto = new Image();
        imageDto.setUniqueId(random);
        imageDto.setFileType(fileExtension[fileExtension.length - 1]);
        imageDto.setSize(file.getSize());
        imageDto.setUploadTime(LocalDateTime.now());
        imageDto.setUploader(uploader);

        try {
            return imageRepository.save(imageDto);
        } catch (Exception e) {
            compressedImageFile.delete();
        }

        throw new IOException("Failed to save image");
    }

    @NonNull
    public NewImageDto getImageStream(String uniqueId, boolean base64, boolean userInfo) throws IOException, ResourceNotFoundException {

        Image image = imageRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        Path filePath = Paths.get(ConfigDb.getIMAGE_DIR(), image.getUniqueId() + "." + image.getFileType());

        return new NewImageDto(
                filePath,
                userInfo ? image.getUploader() : null,
                image.getFileType(),
                Files.size(filePath) / 1024,
                null
        );
    }

    @NonNull
    public ImageDto getImage(String uniqueId, boolean base64, boolean userInfo, boolean info) throws IOException, ResourceNotFoundException {

        Image image = imageRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (image == null) {
            throw new ResourceNotFoundException("Image not found");
        }
        byte[] imageBytes = null;
        String imageBase64 = null;
        Path filePath = Paths.get(ConfigDb.getIMAGE_DIR(), image.getUniqueId() + "." + image.getFileType());
        if (info) {
            try {
                imageBytes = Files.readAllBytes(filePath);
            } catch(Exception e) {
                throw new ResourceNotFoundException("Image not found");
            }
            imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        }

        return new ImageDto(
                imageBytes,
                userInfo ? image.getUploader() : null,
                image.getFileType(),
                Files.size(filePath) / 1024,
                base64 ? imageBase64 : null
        );
    }

    public boolean doesImageExist(String uniqueId) {
        return imageRepository.existsByUniqueId(uniqueId);
    }

    public void deleteByUniqueId(String uniqueId) {
        imageRepository.deleteByUniqueId(uniqueId);
    }

    public long getImageCount(LocalDateTime startDate, LocalDateTime endDate) {
        return imageRepository.countByUploadTimeBetween(startDate, endDate);
    }

    public long getImageCountByDate(LocalDateTime fromDate, LocalDateTime toDate) {
        return imageRepository.countByUploadTimeBetween(fromDate, toDate);
    }

    @Nullable
    public Map<String, Object> getStats(LocalDateTime fromDate, LocalDateTime toDate) {

        long totalImages = imageRepository.count();
        if (totalImages < 1) return null;

        long todayImages = this.getImageCountByDate(fromDate, toDate);
        if (todayImages < 1) return null;

        Map<String, ?> sizes = this.getSizes(fromDate, toDate);
        Map<String, Long> formats = this.getFileTypeLeaderboard(fromDate, toDate);
        Map<String, ?> bestUploader = this.getBiggestUploaderInRange(fromDate, toDate);

        Map<String, Object> stats = new HashMap<>() {{
            put("fromDate", fromDate);
            put("toDate", toDate);
            put("total", todayImages);
            put("total_all", totalImages);
            put("sizes", sizes);
            put("formats", formats);
            put("best_uploader", bestUploader);
        }};

        return stats;
    }

    private Map<String, ?> getSizes(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> mapper = new HashMap<>();
        Object[] sizes = (Object[]) imageRepository.findTopSizesInRange(fromDate, toDate);
        if (sizes != null) {
            mapper.put("largest", sizes[0]);
            mapper.put("smallest", sizes[1]);
            mapper.put("average", sizes[2]);
            mapper.put("total", sizes[3]);
            return mapper;
        }
        return null;
    }

    private Map<String, Long> getFileTypeLeaderboard(LocalDateTime fromDate, LocalDateTime toDate) {
        List<Object[]> result = imageRepository.findFileTypeLeaderboardInRange(fromDate, toDate);
        Map<String, Long> fileTypeStats = new HashMap<>();
        for (Object[] row : result) {
            String fileType = (String) row[0];
            Long count = (Long) row[1];
            fileTypeStats.put(fileType, count);
        }
        return fileTypeStats;
    }

    private Map<String, ?> getBiggestUploaderInRange(LocalDateTime fromDate, LocalDateTime toDate) {
        Object[] result = imageRepository.findBiggestUploaderInRange(fromDate, toDate).get(0);
        Map<String, Object> fileTypeStats = new HashMap<>();
        if (result != null) {
            fileTypeStats.put("uid", result[0]);
            fileTypeStats.put("username", result[1]);
            fileTypeStats.put("avatar", result[2]);
            fileTypeStats.put("uploads", result[3]);
        } else return null;
        return fileTypeStats;
    }

    public Map<String, ?> getUserStats(LocalDateTime fromDate, LocalDateTime toDate, Long uid) {
        Object[] result = (Object[]) imageRepository.findTopUserStats(fromDate, toDate, uid);
        Map<String, Object> userStats = new HashMap<>();
        if (result != null) {
            userStats.put("uid", uid);
            userStats.put("uploads", result[1]);
            userStats.put("total_size", result[2]);
        } else return null;
        return userStats;
    }

    public Map<String, ?> getUserStats(Long uid) {
        return this.getUserStats(LocalDateTime.of(LocalDate.now().minusYears(10), LocalTime.MIN), LocalDateTime.now(), uid);
    }

    public List<StatImageDto> getAllImagesByUser(Long uid) {
        List<Image> images = imageRepository.findAllByUploaderId(uid);
        if (images.isEmpty()) return List.of();
        List<StatImageDto> imageDtos = new ArrayList<>();
        for (Image image : images) {
            imageDtos.add(new StatImageDto(
                    image.getUniqueId(),
                    image.getUploadTime(),
                    image.getFileType(),
                    image.getSize(),
                    serverInfo.getBaseUrl() + "/v1/image/get/" + image.getUniqueId()
            ));
        }
        return imageDtos;
    }
}
