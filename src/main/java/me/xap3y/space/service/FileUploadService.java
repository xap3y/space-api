package me.xap3y.space.service;

import lombok.RequiredArgsConstructor;
import me.xap3y.space.entity.FileEntity;
import me.xap3y.space.entity.FileUploadPack;
import me.xap3y.space.entity.User;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.repository.FileRepository;
import me.xap3y.space.repository.FileUploadPackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileRepository fileRepository;
    private final FileUploadPackRepository fileUploadPackRepository;

    /**
     * Create a new upload pack for batch uploads
     */
    @Transactional
    public FileUploadPack createUploadPack(User uploader, String description, String password, ResourceSourceType source) {
        FileUploadPack pack = new FileUploadPack();
        pack.setPackId(UUID.randomUUID().toString());
        pack.setUploader(uploader);
        pack.setDescription(description);
        pack.setPassword(password);
        pack.setSource(source);
        pack.setComplete(false);

        return fileUploadPackRepository.save(pack);
    }

    /**
     * Add a file to an upload pack
     */
    @Transactional
    public FileEntity addFileToUploadPack(FileUploadPack pack, String uniqueId, String fileName, String fileType,
                                    long size, String description, String password,
                                    LocalDateTime expirationTime, User uploader, ResourceSourceType source) {
        FileEntity file = new FileEntity();
        file.setUniqueId(uniqueId);
        file.setFileName(fileName);
        file.setFileType(fileType);
        file.setSize(size);
        file.setDescription(description);
        file.setPassword(password);
        file.setExpirationTime(expirationTime);
        file.setUploadPack(pack);
        file.setUploader(uploader);
        file.setSource(source);

        FileEntity savedFile = fileRepository.save(file);

        // Update pack total size
        pack.setTotalSize(pack.getTotalSize() + size);
        fileUploadPackRepository.save(pack);

        return savedFile;
    }

    /**
     * Upload single file (no pack)
     */
    @Transactional
    public FileEntity uploadSingleFile(String uniqueId, String fileName, String fileType, long size,
                                 String description, String password, LocalDateTime expirationTime,
                                 User uploader, ResourceSourceType source) {
        FileEntity file = new FileEntity();
        file.setUniqueId(uniqueId);
        file.setFileName(fileName);
        file.setFileType(fileType);
        file.setSize(size);
        file.setDescription(description);
        file.setPassword(password);
        file.setExpirationTime(expirationTime);
        file.setUploadPack(null);
        file.setUploader(uploader);
        file.setSource(source);

        return fileRepository.save(file);
    }

    /**
     * Mark upload pack as complete
     */
    @Transactional
    public FileUploadPack completeUploadPack(FileUploadPack pack) {
        pack.setComplete(true);
        return fileUploadPackRepository.save(pack);
    }

    /**
     * Get file by unique ID
     */
    public FileEntity getFileByUniqueId(String uniqueId) {
        return fileRepository.findByUniqueId(uniqueId).orElse(null);
    }

    /**
     * Get upload pack by pack ID
     */
    public FileUploadPack getUploadPackByPackId(String packId) {
        return fileUploadPackRepository.findByPackId(packId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<FileUploadPack> getUserPacksPaginated(Long userId, Pageable pageable) {
        return fileUploadPackRepository.findByUploaderId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FileUploadPack> getAllPacksPaginated(Pageable pageable) {
        return fileUploadPackRepository.findAll(pageable);
    }

    public void deleteUploadPack(FileUploadPack pack) {
        fileUploadPackRepository.delete(pack);
    }

    public void updateUploadPack(FileUploadPack pack) {
        fileUploadPackRepository.save(pack);
    }
}