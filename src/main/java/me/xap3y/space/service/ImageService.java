package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.dto.ImageDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.User;
import me.xap3y.space.exception.InvalidApiKeyException;
import me.xap3y.space.exception.ResourceNotFoundException;
import me.xap3y.space.repository.ImageRepository;
import me.xap3y.space.repository.UserRepository;
import me.xap3y.space.util.ConfigDb;
import me.xap3y.space.util.ImageCompressor;
import me.xap3y.space.util.Utils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Service
public class ImageService {

    private final String[] supportedExtensions = {"png", "jpg", "webp", "mp4", "gif", "jpeg", "bmp"};

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    public ImageService(ImageRepository imageRepository, UserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
    }

    public Image saveImage(MultipartFile file, String apiKey) throws IOException, RuntimeException {

        User user = userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API key"));

        if (user == null) {
            throw new RuntimeException("Invalid API key");
        }

        String random = Utils.generateRandomId();
        String[] fileExtension = Objects.requireNonNull(file.getOriginalFilename()).split("\\.");
        String fileNameWithExtension = random + "." + fileExtension[fileExtension.length - 1].toLowerCase();
        String fElc = fileExtension[fileExtension.length - 1].toLowerCase();

        for (String s : supportedExtensions) {
            if (s.equals(fElc)) {
                break;
            }
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

            ImageCompressor.compressImage(file.getInputStream(), compressedImageFile, scale, quality);
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
        imageDto.setUploader(user);

        try {
            return imageRepository.save(imageDto);
        } catch (Exception e) {
            compressedImageFile.delete();
        }

        throw new IOException("Failed to save image");
    }

    @NonNull
    public ImageDto getImage(String uniqueId, boolean base64, boolean userInfo) throws IOException {

        Image image = imageRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (image == null) {
            throw new FileNotFoundException("Image not found");
        }

        Path filePath = Paths.get(ConfigDb.getIMAGE_DIR(), image.getUniqueId() + "." + image.getFileType());

        byte[] imageBytes= Files.readAllBytes(filePath);

        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

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
}
