package me.xap3y.space.controller;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.ResourceSourceType;
import me.xap3y.space.api.enums.ResourceLimitType;
import me.xap3y.space.api.enums.PortalLogType;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.ResourceAccessForbiddenException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.OptionalCookieAuth;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.dto.FileInfoDto;
import me.xap3y.space.entity.FileEntity;
import me.xap3y.space.entity.FileUploadPack;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.FileMapper;
import me.xap3y.space.model.request.FileRegisterRequest;
import me.xap3y.space.model.request.FileRegisterItemRequest;
import me.xap3y.space.model.request.PasswordRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.FileUploadResponse;
import me.xap3y.space.model.response.PackInfoResponse;
import me.xap3y.space.model.response.PackListResponse;
import me.xap3y.space.service.FileUploadService;
import me.xap3y.space.service.ResourceLimitService;
import me.xap3y.space.service.S3Service;
import me.xap3y.space.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileMapper fileMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final ResourceLimitService resourceLimitService;
    private final AuditLogService auditLogService;

    private static final String PREFIX = "files/";

    @PostMapping("/presigned-url/put")
    @RequiresApiKey
    public ResponseEntity<?> generatePresignedPutUrl(
            HttpServletRequest request,
            @RequestParam String filename,
            @RequestParam String contentType
    ) {
        resourceLimitService.assertMutationAllowed((User) request.getAttribute("uploader"));
        String url = s3Service.generatePresignedPutUrl(
                PREFIX + filename,
                contentType
        );
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Register files (single or batch)
     * Always creates a pack, even for single file
     */
    @PostMapping("/register")
    @RequiresApiKey
    public ResponseEntity<?> registerFiles(
            HttpServletRequest request,
            @RequestBody FileRegisterRequest registerRequest
    ) {
        User uploader = (User) request.getAttribute("uploader");

        if (registerRequest.items() == null || registerRequest.items().isEmpty()) {
            throw new BadRequestException("Items list cannot be empty");
        }

        long totalBytes = 0;
        for (FileRegisterItemRequest item : registerRequest.items()) {
            if (item.uniqueId() == null || item.uniqueId().isEmpty())
                throw new BadRequestException("uniqueId is required for all items");
            if (item.fileType() == null || item.fileType().isEmpty())
                throw new BadRequestException("fileType is required for all items");
            if (item.size() == null || item.size() <= 0)
                throw new BadRequestException("size must be greater than 0");
            try {
                totalBytes = Math.addExact(totalBytes, item.size());
            } catch (ArithmeticException exception) {
                throw new BadRequestException("Total upload size is too large");
            }
        }
        resourceLimitService.assertCanCreate(uploader, ResourceLimitType.FILE, registerRequest.items().size(), totalBytes);

        try {
            ResourceSourceType source = registerRequest.source() != null ?
                    registerRequest.source() : ResourceSourceType.API;

            String encodedPassword = registerRequest.password() != null ?
                    passwordEncoder.encode(registerRequest.password()) : null;

            // Create upload pack
            FileUploadPack pack = fileUploadService.createUploadPack(
                    uploader,
                    registerRequest.description(),
                    encodedPassword,
                    source
            );

            List<FileInfoDto> uploadedFiles = new ArrayList<>();

            // Register each file to the pack
            for (FileRegisterItemRequest item : registerRequest.items()) {
                if (item.uniqueId() == null || item.uniqueId().isEmpty()) {
                    throw new BadRequestException("uniqueId is required for all items");
                }

                if (item.fileType() == null || item.fileType().isEmpty()) {
                    throw new BadRequestException("fileType is required for all items");
                }

                if (item.size() == null || item.size() <= 0) {
                    throw new BadRequestException("size must be greater than 0");
                }

                LocalDateTime expirationTime = item.expiryDate() != null ?
                        LocalDateTime.now().plusSeconds(item.expiryDate() / 1000) : null;

                String itemPassword = item.password() != null ?
                        passwordEncoder.encode(item.password()) : null;

                FileEntity savedFile = fileUploadService.addFileToUploadPack(
                        pack,
                        item.uniqueId(),
                        item.fileName() != null ? item.fileName() : item.uniqueId(),
                        item.fileType(),
                        item.size(),
                        item.description(),
                        itemPassword,
                        expirationTime,
                        uploader,
                        source
                );

                uploadedFiles.add(fileMapper.apply(savedFile));
            }

            // Complete the pack
            FileUploadPack completedPack = fileUploadService.completeUploadPack(pack);
            resourceLimitService.recordCreation(uploader, ResourceLimitType.FILE, uploadedFiles.size(), completedPack.getTotalSize());
            auditLogService.saveLog(PortalLogType.FILE_PACK_UPLOAD, uploader, completedPack.getPackId(), source.toString());

            FileUploadResponse response = new FileUploadResponse(
                    false,
                    completedPack.getPackId(),
                    uploadedFiles,
                    completedPack.getFiles().size(),
                    completedPack.getTotalSize(),
                    completedPack.getUploadTime()
            );

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error registering files: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to register files: " + e.getMessage());
        }
    }

    /**
     * Get upload pack details by ID
     */
    @GetMapping("/pack/{packId}")
    @RequiresApiKey
    public ResponseEntity<?> getUploadPackDetails(
            HttpServletRequest request,
            @PathVariable String packId
    ) {
        try {
            FileUploadPack pack = fileUploadService.getUploadPackByPackId(packId);
            if (pack == null) {
                throw new ResourceNotFoundException("Pack not found");
            }

            List<FileInfoDto> files = pack.getFiles().stream()
                    .map(fileMapper)
                    .toList();

            FileUploadResponse response = new FileUploadResponse(
                    false,
                    pack.getPackId(),
                    files,
                    pack.getFiles().size(),
                    pack.getTotalSize(),
                    pack.getUploadTime()
            );

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting upload pack details: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to get pack details");
        }
    }

    @DeleteMapping("/pack/public/{packId}")
    @OptionalCookieAuth
    public ResponseEntity<?> deletePackFiles(
            HttpServletRequest request,
            @PathVariable String packId,
            @RequestBody(required = false) Map<String, String> passwordRequest
    ) {
        User uploader = (User) request.getAttribute("uploader");
        resourceLimitService.assertMutationAllowed(uploader);
        try {
            FileUploadPack pack = fileUploadService.getUploadPackByPackId(packId);
            if (pack == null) {
                throw new ResourceNotFoundException("Pack not found");
            }

            boolean owns = false;

            if (pack.getUploader() != null && uploader != null) {
                owns = pack.getUploader().getId().equals(uploader.getId());
            } else if (uploader != null && uploader.isAdmin()) {
                owns = true;
            }

            if (!owns) {
                throw new ResourceAccessForbiddenException("You do not have permission to delete this pack");
            }

            // Verify password if protected
            if (pack.getPassword() != null) {
                if (passwordRequest == null || passwordRequest.get("password") == null) {
                    throw new ResourceAccessForbiddenException("Pack is password protected");
                }

                String providedPassword = passwordRequest.get("password");
                boolean passwordMatches = passwordEncoder.matches(providedPassword, pack.getPassword());

                if (!passwordMatches) {
                    throw new ResourceAccessForbiddenException("Invalid password");
                }
            }

            // Delete all files in pack from S3
            List<String> deletedFiles = new ArrayList<>();
            s3Service.deleteFiles(pack.getFiles().stream().map(FileEntity::getUniqueId).toList());

            // Delete pack and all files from database
            fileUploadService.deleteUploadPack(pack);
            auditLogService.saveLog(PortalLogType.FILE_PACK_DELETE, uploader, packId, "PORTAL");

            log.info("Pack {} deleted successfully. {} files removed from S3", packId, deletedFiles.size());

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Pack deleted successfully");
                put("deletedFileCount", deletedFiles.size());
                put("deletedFiles", deletedFiles);
                put("packId", packId);
            }});

        } catch (ResourceNotFoundException | ResourceAccessForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting pack: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete pack");
        }
    }

    @DeleteMapping("/pack/public/{packId}/file/{fileId}")
    @OptionalCookieAuth
    public ResponseEntity<?> deletePackFile(
            HttpServletRequest request,
            @PathVariable String packId,
            @PathVariable String fileId,
            @RequestBody(required = false) Map<String, String> passwordRequest
    ) {
        User uploader = (User) request.getAttribute("uploader");
        resourceLimitService.assertMutationAllowed(uploader);
        try {
            FileUploadPack pack = fileUploadService.getUploadPackByPackId(packId);
            if (pack == null) {
                throw new ResourceNotFoundException("Pack not found");
            }

            boolean owns = false;

            if (pack.getUploader() != null && uploader != null) {
                owns = pack.getUploader().getId().equals(uploader.getId());
            } else if (uploader != null && uploader.isAdmin()) {
                owns = true;
            }

            if (!owns) {
                throw new ResourceAccessForbiddenException("You do not have permission to delete files from this pack");
            }

            // Verify password if protected
            if (pack.getPassword() != null) {
                if (passwordRequest == null || passwordRequest.get("password") == null) {
                    throw new ResourceAccessForbiddenException("Pack is password protected");
                }

                String providedPassword = passwordRequest.get("password");
                boolean passwordMatches = passwordEncoder.matches(providedPassword, pack.getPassword());

                if (!passwordMatches) {
                    throw new ResourceAccessForbiddenException("Invalid password");
                }
            }

            // Find file in pack
            FileEntity fileToDelete = pack.getFiles().stream()
                    .filter(f -> f.getUniqueId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("File not found in pack"));

            // Delete file from S3
            try {
                s3Service.deleteFile(fileToDelete.getUniqueId());
                log.info("Deleted file from S3: {}", fileToDelete.getUniqueId());
            } catch (Exception e) {
                log.error("Error deleting file {} from S3: {}", fileToDelete.getUniqueId(), e.getMessage());
                throw new BadRequestException("Failed to delete file from storage");
            }

            // Remove file from pack
            pack.getFiles().remove(fileToDelete);

            // If pack has no files left, delete the pack
            if (pack.getFiles().isEmpty()) {
                fileUploadService.deleteUploadPack(pack);
                auditLogService.saveLog(PortalLogType.FILE_PACK_DELETE, uploader, packId, "LAST_FILE_REMOVED");
                log.info("Pack {} deleted (no files remaining)", packId);

                return new ResponseEntity<>(new DefaultResponse(false, "OK"), HttpStatus.NO_CONTENT);
            } else {
                // Update pack in database
                fileUploadService.updateUploadPack(pack);
                log.info("File {} deleted from pack {}", fileId, packId);

                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", true);
                    put("message", "File deleted from pack");
                    put("deletedFile", fileToDelete.getFileName());
                    put("remainingFiles", pack.getFiles().size());
                    put("packId", packId);
                }});
            }

        } catch (ResourceNotFoundException | ResourceAccessForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting file from pack: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete file");
        }
    }

    /*@PostMapping("/pack/public/{packId}/download/zip")
    @OptionalCookieAuth
    public ResponseEntity<?> postDownloadZipPack(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String packId,
            @RequestBody(required = false) Map<String, String> passwordRequest
    ) {
        try {
            FileUploadPack pack = fileUploadService.getUploadPackByPackId(packId);
            if (pack == null) {
                throw new ResourceNotFoundException("Pack not found");
            }

            // Only allow access if pack is completed
            if (!pack.isComplete()) {
                throw new ResourceAccessForbiddenException("Pack is not completed");
            }

            // Verify password if protected
            if (pack.getPassword() != null) {
                if (passwordRequest == null || passwordRequest.get("password") == null) {
                    throw new ResourceAccessForbiddenException("Pack is password protected");
                }

                String providedPassword = passwordRequest.get("password");
                boolean passwordMatches = passwordEncoder.matches(providedPassword, pack.getPassword());

                if (!passwordMatches) {
                    throw new ResourceAccessForbiddenException("Invalid password");
                }
            }

            if (pack.getFiles().size() < 2) {
                throw new BadRequestException("Pack must contain at least 2 files to download as zip");
            }

            // Set response headers for ZIP download
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"pack-" + packId + ".zip\"");
            response.setHeader("Transfer-Encoding", "chunked");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

            // Stream ZIP directly to response with cancellation support
            try (ServletOutputStream outputStream = response.getOutputStream();
                 ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {

                zipOut.setLevel(ZipOutputStream.DEFLATED);
                byte[] buffer = new byte[8192]; // 8KB buffer

                for (FileEntity file : pack.getFiles()) {
                    // Check if client disconnected
                    if (request.getInputStream().available() < 0) {
                        log.warn("Client cancelled ZIP download for pack: {}", packId);
                        return null;
                    }

                    try (InputStream fileInputStream = s3Service.getFileAsStream(file.getUniqueId())) {
                        if (fileInputStream != null) {
                            ZipEntry zipEntry = new ZipEntry(file.getFileName());
                            zipEntry.setTime(System.currentTimeMillis());
                            zipOut.putNextEntry(zipEntry);

                            // Stream file to ZIP
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                // Check for client disconnect periodically
                                if (Thread.currentThread().isInterrupted()) {
                                    log.warn("ZIP download interrupted for pack: {}", packId);
                                    return null;
                                }

                                zipOut.write(buffer, 0, bytesRead);
                                outputStream.flush();
                            }

                            zipOut.closeEntry();
                            log.info("Added file to ZIP: {}", file.getFileName());
                        }
                    } catch (IOException e) {
                        if (e.getMessage().contains("Broken pipe") || e.getMessage().contains("Connection reset")) {
                            log.warn("Client disconnected during ZIP creation for pack: {}", packId);
                            return null;
                        }
                        log.error("Error adding file {} to ZIP: {}", file.getFileName(), e.getMessage());
                    }
                }

                zipOut.finish();
                outputStream.flush();
                log.info("ZIP created successfully for pack: {} with {} files", packId, pack.getFiles().size());

            } catch (IOException e) {
                if (e.getMessage().contains("Broken pipe") || e.getMessage().contains("Connection reset")) {
                    log.warn("Client cancelled ZIP download for pack: {}", packId);
                } else {
                    log.error("Error creating ZIP stream: {}", e.getMessage(), e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

            return null;

        } catch (ResourceNotFoundException | ResourceAccessForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error downloading zip pack: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to download pack");
        }
    }*/

    @PostMapping("/pack/public/{packId}/download/zip")
    @OptionalCookieAuth
    public void postDownloadZipPack(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String packId,
            @RequestBody(required = false) Map<String, String> passwordRequest
    ) {
        try {
            FileUploadPack pack = fileUploadService.getUploadPackByPackId(packId);
            if (pack == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Pack not found");
                return;
            }

            // Only allow access if pack is completed
            if (!pack.isComplete()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Pack is not completed");
                return;
            }

            // Verify password if protected
            if (pack.getPassword() != null) {
                if (passwordRequest == null || passwordRequest.get("password") == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Pack is password protected");
                    return;
                }

                String providedPassword = passwordRequest.get("password");
                boolean passwordMatches = passwordEncoder.matches(providedPassword, pack.getPassword());

                if (!passwordMatches) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid password");
                    return;
                }
            }

            if (pack.getFiles().size() < 2) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Pack must contain at least 2 files");
                return;
            }

            // ✅ Set response headers BEFORE getting output stream
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"pack-" + packId + ".zip\"");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            response.setBufferSize(32768); // ✅ Larger buffer

            // ✅ Stream ZIP directly to response
            try (ServletOutputStream outputStream = response.getOutputStream();
                 ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {

                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
                byte[] buffer = new byte[16384]; // ✅ 16KB buffer

                for (FileEntity file : pack.getFiles()) {
                    try {
                        // ✅ Get file from S3 as stream
                        InputStream fileInputStream = s3Service.getFileAsStream(file.getUniqueId());

                        if (fileInputStream == null) {
                            log.warn("File not found in S3: {}", file.getUniqueId());
                            continue;
                        }

                        try (fileInputStream) {
                            // ✅ Create ZIP entry
                            ZipEntry zipEntry = new ZipEntry(file.getFileName());
                            zipEntry.setTime(System.currentTimeMillis());
                            zipOut.putNextEntry(zipEntry);

                            // ✅ Stream file to ZIP
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                zipOut.write(buffer, 0, bytesRead);
                            }

                            zipOut.closeEntry();
                            log.info("Added file to ZIP: {}", file.getFileName());
                        }

                    } catch (IOException e) {
                        if (isClientDisconnected(e)) {
                            log.warn("Client disconnected during ZIP creation for pack: {}", packId);
                            return;
                        }
                        log.error("Error adding file {} to ZIP: {}", file.getFileName(), e.getMessage());
                        // Continue with next file
                    }
                }

                // ✅ Finish and flush
                zipOut.finish();
                zipOut.flush();
                outputStream.flush();

                log.info("ZIP created successfully for pack: {} with {} files", packId, pack.getFiles().size());

            } catch (IOException e) {
                if (isClientDisconnected(e)) {
                    log.warn("Client cancelled ZIP download for pack: {}", packId);
                } else {
                    log.error("Error creating ZIP stream: {}", e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Unexpected error downloading zip pack: {}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to download pack");
            } catch (IOException ioException) {
                log.error("Failed to send error response", ioException);
            }
        }
    }

    @PostMapping("/pack/public/{packId}")
    @OptionalCookieAuth
    public ResponseEntity<?> getPublicUploadPack(
            HttpServletRequest request,
            @PathVariable String packId,
            @RequestBody(required = false) PasswordRequest passwordRequest
    ) {
        User uploader = (User) request.getAttribute("uploader");
        try {
            FileUploadPack pack = fileUploadService.getUploadPackByPackId(packId);
            if (pack == null) {
                throw new ResourceNotFoundException("Pack not found");
            }

            // Only allow access if pack is completed
            if (!pack.isComplete()) {
                throw new ResourceAccessForbiddenException("Pack is not completed");
            }

            boolean owns = false;

            if (pack.getUploader() != null && uploader != null) {
                owns = pack.getUploader().getId().equals(uploader.getId());
            } else if (uploader != null && uploader.isAdmin()) {
                owns = true;
            }

            // Check if pack is password protected
            if (pack.getPassword() != null && !owns) {
                // Password required
                if (passwordRequest == null || passwordRequest.password() == null || passwordRequest.password().isEmpty()) {
                    throw new ResourceAccessForbiddenException("Pack is password protected. Password required.");
                }

                // Verify password
                boolean passwordMatches = passwordEncoder.matches(passwordRequest.password(), pack.getPassword());
                if (!passwordMatches) {
                    throw new ResourceAccessForbiddenException("Invalid password");
                }
            }

            List<FileInfoDto> files = pack.getFiles().stream()
                    .map(fileMapper)
                    .toList();

            FileUploadResponse response = new FileUploadResponse(
                    false,
                    pack.getPackId(),
                    files,
                    pack.getFiles().size(),
                    pack.getTotalSize(),
                    pack.getUploadTime()
            );

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | ResourceAccessForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting public pack: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to get pack");
        }
    }

    /**
     * Get public upload pack info (check if exists and if password protected)
     * No API key required
     */
    @GetMapping("/pack/public/{packId}/info")
    @OptionalCookieAuth
    public ResponseEntity<?> getPublicPackInfo(
            HttpServletRequest request,
            @PathVariable String packId
    ) {
        User uploader = (User) request.getAttribute("uploader");

        try {
            FileUploadPack pack = fileUploadService.getUploadPackByPackId(packId);
            if (pack == null) {
                throw new ResourceNotFoundException("Pack not found");
            }

            // Only allow access if pack is completed
            if (!pack.isComplete()) {
                throw new ResourceAccessForbiddenException("Pack is not completed");
            }

            boolean hasAccess = false;
            if (pack.getUploader() != null && uploader != null) {
                hasAccess = pack.getUploader().getId().equals(uploader.getId());
            } else if (uploader != null && uploader.isAdmin()) {
                hasAccess = true;
            }

            PackInfoResponse response = new PackInfoResponse(
                    pack.getPackId(),
                    pack.getDescription(),
                    pack.getFiles().size(),
                    pack.getTotalSize(),
                    pack.getUploadTime(),
                    pack.getPassword() != null,
                    hasAccess
            );

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | ResourceAccessForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting pack info: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to get pack info");
        }
    }

    @GetMapping("/all-packs")
    @RequiresApiKey
    public ResponseEntity<?> getAllPacks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            if (page < 0 || size <= 0 || size > 50) {
                throw new BadRequestException("Invalid page or size parameters");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<FileUploadPack> packs = fileUploadService.getAllPacksPaginated(pageable);

            List<PackListResponse> packList = packs.getContent().stream()
                    .map(pack -> new PackListResponse(
                            pack.getPackId(),
                            pack.getDescription(),
                            pack.isComplete(),
                            pack.getFiles().size(),
                            pack.getTotalSize(),
                            pack.getUploadTime(),
                            pack.getPassword() != null
                    ))
                    .toList();

            return ResponseEntity.ok(new PackListResponse(
                    packList,
                    packs.getTotalElements(),
                    packs.getTotalPages(),
                    packs.getNumber(),
                    packs.getSize()
            ));

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching all packs: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to fetch packs");
        }
    }

    @GetMapping("/packs")
    @RequiresApiKey
    public ResponseEntity<?> getUserPacks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        User uploader = (User) request.getAttribute("uploader");

        try {
            if (page < 0 || size <= 0 || size > 50) {
                throw new BadRequestException("Invalid page or size parameters");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<FileUploadPack> packs = fileUploadService.getUserPacksPaginated(uploader.getId(), pageable);

            List<PackListResponse> packList = packs.getContent().stream()
                    .map(pack -> PackListResponse.builder()
                            .packId(pack.getPackId())
                            .description(pack.getDescription())
                            .isComplete(pack.isComplete())
                            .totalFiles(pack.getFiles().size())
                            .files(pack.getFiles().stream().map(fileMapper).toList())
                            .totalSize(pack.getTotalSize())
                            .uploadTime(pack.getUploadTime())
                            .isPasswordProtected(pack.getPassword() != null)
                            .build()
                    )
                    .toList();

            return ResponseEntity.ok(new PackListResponse(
                    packList,
                    packs.getTotalElements(),
                    packs.getTotalPages(),
                    packs.getNumber(),
                    packs.getSize()
            ));

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user packs: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to fetch packs");
        }
    }

    private boolean isClientDisconnected(IOException e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("Broken pipe") ||
                        message.contains("Connection reset") ||
                        message.contains("ClientAbortException") ||
                        message.contains("Stream closed") ||
                        message.contains("Response committed") ||
                        message.contains("not usable after response errors")
        );
    }
}
