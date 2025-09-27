package me.xap3y.space.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.exception.InvalidUniqueIdException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ImageMapper;
import me.xap3y.space.repository.ImageRepository;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.DngConverter;
import me.xap3y.space.util.ImageCompressor;
import me.xap3y.space.util.Utils;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class ImageService {

    private final String[] supportedExtensions = {"png", "jpg", "webp", "mp4", "gif", "jpeg", "bmp", "ico", "svg",
            "webm", "mkv", "mov", "avi", "wav", "flv", "wmv", "ogv", "ogg", "avif", "heic", "heif", "jfif", "jpeg", "jif",
            "tiff", "tif", "cur", "bmp", "vob", "drc", "qt", "3gp", "xbm", "dng"};

    private final ImageRepository imageRepository;
    private final ImageCompressor imageCompressor;
    private final ImageMapper imageMapper;

    public ImageService(ImageRepository imageRepository, ImageCompressor imageCompressor, ImageMapper imageMapper) {
        this.imageRepository = imageRepository;
        this.imageCompressor = imageCompressor;
        this.imageMapper = imageMapper;
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
        return saveImage(file, uploader, null, null, true, null);
    }

    // register image UID into DB
    public Image registerImage(User uploader, String uniqueId, String password, boolean isPublic, String description, String fileType, Long size, ImageLocation location) throws IOException, RuntimeException {

        if (uniqueId == null) {
            uniqueId = Utils.generateRandomId();
        } else {
            if (!uniqueId.matches("^[a-zA-Z0-9]*$")) {
                throw new InvalidUniqueIdException();
            }
        }

        Image imageDto = new Image();
        imageDto.setUniqueId(uniqueId);
        imageDto.setIsPublic(isPublic);
        imageDto.setPassword(password);
        imageDto.setLocation(location);
        imageDto.setDescription(description);
        imageDto.setFileType(fileType);
        imageDto.setSize(size != null ? size : 0L);
        imageDto.setUploadTime(LocalDateTime.now());
        imageDto.setUploader(uploader);

        return imageRepository.save(imageDto);
    }

    public Image saveImageFromUrl(String url, User uploader) {
        String uniqueId = Utils.generateRandomId();
        String fileExtension = url.substring(url.lastIndexOf(".") + 1).toLowerCase();

        // save from URL to file under the generated uniqueId
        String fileNameWithExtension = uniqueId + "." + fileExtension;
        File imageFile = new File(ConfigDb.getIMAGE_DIR(), fileNameWithExtension);
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Image downloaded from URL: {}", url);
            return registerImage(uploader, uniqueId, null, true, null, fileExtension, imageFile.length(), ImageLocation.LOCAL);
        } catch (Exception e) {
            log.error("Failed to download image from URL: {}", url, e);
            throw new RuntimeException("Failed to download image from URL");
        }
    }

    @SneakyThrows
    public Image saveImage(MultipartFile file, User uploader, String uniqueId, String password, boolean isPublic, String description) throws IOException, RuntimeException {

        if (uniqueId == null) {
            uniqueId = Utils.generateRandomId();
        } else {
            if (!uniqueId.matches("^[a-zA-Z0-9]*$")) {
                throw new InvalidUniqueIdException();
            }
        }

        String[] fileExtension = Objects.requireNonNull(file.getOriginalFilename()).split("\\.");
        String originalExt = fileExtension[fileExtension.length - 1].toLowerCase();
        String fileNameWithExtension = uniqueId + "." + fileExtension[fileExtension.length - 1].toLowerCase();
        String fElc = originalExt;

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

        log.info("Handling image with id: {}", uniqueId);
        File compressedImageFile = new File(ConfigDb.getIMAGE_DIR(), fileNameWithExtension);

        // If HEIC → convert to JPG
        if (fElc.equals("heic") || fElc.equals("heif")) {
            log.info("Converting HEIC image with id: {}", uniqueId);

            BufferedImage heicImage = ImageIO.read(file.getInputStream());
            if (heicImage == null) {
                throw new IOException("Failed to read HEIC image");
            }

            log.info("buffered image: {}x {}y", heicImage.getWidth(), heicImage.getHeight());

            // Change extension
            fElc = "jpg";
            fileNameWithExtension = uniqueId + ".jpg";
            compressedImageFile = new File(ConfigDb.getIMAGE_DIR(), fileNameWithExtension);

            BufferedImage awtImage = new BufferedImage(heicImage.getWidth(), heicImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = awtImage.createGraphics();
            g.drawImage(heicImage, 0, 0, null);
            g.dispose();

            // Save converted JPG
            try (OutputStream os = new FileOutputStream(compressedImageFile)) {
                if (!ImageIO.write(awtImage, "jpg", os)) {
                    throw new IOException("No ImageIO writer found for JPEG format.");
                }
            }

            file = null; // mark as already handled
        } else if (fElc.equals("dng")) {
            log.info("Converting DNG image with id: {}", uniqueId);
            // Convert DNG to JPG using ffmpeg

            compressedImageFile = DngConverter.convertDngToJpeg(file, ConfigDb.getIMAGE_DIR(), uniqueId);

            if (!compressedImageFile.exists()) {
                throw new IOException("Failed to convert DNG to JPG");
            }

            // Change extension
            fElc = "jpg";
            fileNameWithExtension = uniqueId + ".jpg";

            // From now on, work with the converted file instead of MultipartFile
            file = null; // mark as already handled
        }

        log.info("Checking image type: {}", fElc);

        if (file != null && (fElc.equals("png") || fElc.equals("jpg") || fElc.equals("webp")) && file.getSize() > 70000) {
            log.info("Compressing image with id: {} and size: {}", uniqueId, file.getSize());
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
        } else if (file != null) {
            log.info("Saving image with id: {}", uniqueId);
            Path filePath = Paths.get(ConfigDb.getIMAGE_DIR(), fileNameWithExtension);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            log.info("Image already handled, skipping saving step.");
        }

        log.info("Saving image with file name: {}", fileNameWithExtension);
        Image imageDto = new Image();
        imageDto.setUniqueId(uniqueId);
        imageDto.setLocation(ImageLocation.LOCAL);
        imageDto.setIsPublic(isPublic);
        imageDto.setPassword(password);
        imageDto.setDescription(description);
        imageDto.setFileType(fElc);
        imageDto.setSize(compressedImageFile.length());
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
                image.getDescription(),
                image.getPassword(),
                image.getExpirationTime(),
                Files.size(filePath) / 1024,
                null,
                image.getIsPublic()
        );
    }

    @NonNull
    public Image getImage(String uniqueId) throws ResourceNotFoundException {

        return imageRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
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

    public Long getStorageUsedInRange(LocalDateTime startDate, LocalDateTime endDate) {
        return imageRepository.sumByUploadTimeBetween(startDate, endDate);
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

    public int countByUploaderId(Long uid) {
        return imageRepository.countByUploaderId(uid);
    }

    public List<ImageInfoDto> getAllImagesByUser(Long uid, Long from, Long to, Integer limit) {

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        if (from == null) {
            fromDate = LocalDateTime.of(LocalDate.now().minusYears(10), LocalTime.MIN);
        } else {
            // Czechia timezone
            fromDate = LocalDateTime.ofEpochSecond(from, 0, java.time.ZoneOffset.UTC);
        }
        if (to == null) {
            toDate = LocalDateTime.now();
        } else {
            toDate = Instant.ofEpochMilli(to)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            //toDate = LocalDateTime.ofEpochSecond(to, 0, ZoneOffset.of("+02:00"));
        }
        log.info("To-DATE {}", toDate);
        if (limit == null) {
            limit = 25;
        }
        List<Image> images = imageRepository.findAllByUploaderIdBetween(uid, fromDate, toDate, limit);
        if (images.isEmpty()) return List.of();
        List<ImageInfoDto> imageDtos = new ArrayList<>();
        for (Image image : images) {
            imageDtos.add(imageMapper.apply(image));
            /*imageDtos.add(new StatImageDto(
                    image.getUniqueId(),
                    image.getUploadTime(),
                    image.getFileType(),
                    image.getSize(),
                    serverInfo.getBaseUrl() + "/v1/image/get/" + image.getUniqueId()
            ));*/
        }
        return imageDtos;
    }

    // COUNT METHODS

    public Long countByUploadTimeBetweenAndUploaderId(LocalDateTime startDate, LocalDateTime endDate, Long uploaderId) {
        return imageRepository.countByUploadTimeBetweenAndUploaderId(startDate, endDate, uploaderId);
    }

    public List<Pair<LocalDate, Long>> findTotalImagesPerDayByUser(LocalDateTime startDate, LocalDateTime endDate, Long uploaderId, boolean fillMissingDates) {
        List<Object[]> results = imageRepository.findTotalImagesPerDayByUser(startDate.with(LocalTime.MIN), endDate.with(LocalTime.MAX), uploaderId);
        return Utils.convertToPairList(startDate, endDate, results, fillMissingDates);
    }

    // Accept start and end date, get total of images each day filter by user, return List<Pair<LocalDate, Long>>
    // example: if start date is 2024-01-01 and end date is 2024-01-07, return:
    // 2024-01-01 -> 10 images | 2024-01-02 -> 5 images | 2024-01-03 -> 0 images
    // 2024-01-04 -> 0 images | 2024-01-05 -> 0 images | 2024-01-06 -> 0 images | 2024-01-07 -> 0 images
    /*public List<Pair<LocalDate, Long>> findTotalImagesPerDayByUser(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> result = imageRepository.findTotalImagesPerDayByUser(startDate, endDate);
        List<Pair<LocalDate, Long>> imagesPerDay = new ArrayList<>();
        for (Object[] row : result) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            imagesPerDay.add(Pair.of(date, count));
        }
        return imagesPerDay;
    }*/

    //findTotalImagesPerDay
    public List<Pair<LocalDate, Long>> findTotalImagesPerDay(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> result = imageRepository.findTotalImagesPerDay(startDate, endDate);
        List<Pair<LocalDate, Long>> imagesPerDay = new ArrayList<>();
        for (Object[] row : result) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            imagesPerDay.add(Pair.of(date, count));
        }
        return imagesPerDay;
    }

    public Optional<Pair<Long, Long>> findBestUploader(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> result = imageRepository.findBestUploader(startDate, endDate).orElse(null);
        return Utils.parseBestUploader(result);
    }
}
