package me.xap3y.space.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
public class HeicConverter {

    public static File convertHeicToJpeg(MultipartFile heicFile, File outputFile, String originalExt)
            throws IOException, InterruptedException {

        // Save MultipartFile to a temporary HEIC/HEIF file
        File tempHeic = File.createTempFile("upload-", "." + originalExt);
        heicFile.transferTo(tempHeic);

        log.info("Converting HEIC to JPEG: {} -> {}", tempHeic.getAbsolutePath(), outputFile.getAbsolutePath());

        // Run ImageMagick convert
        ProcessBuilder pb = new ProcessBuilder(
                "convert",
                "-quality", "90",
                tempHeic.getAbsolutePath(),
                outputFile.getAbsolutePath()
        );

        pb.inheritIO();
        log.info("Starting HEIC conversion...");
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info("HEIC conversion finished with exit code: {}", exitCode);

        // Clean up temporary file
        if (!tempHeic.delete()) {
            log.warn("Failed to delete temp HEIC file: {}", tempHeic.getAbsolutePath());
        }

        if (exitCode != 0) {
            throw new IOException("Failed to convert HEIC to JPEG. Exit code: " + exitCode);
        }

        return outputFile;
    }
}
