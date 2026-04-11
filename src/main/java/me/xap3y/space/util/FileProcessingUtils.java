package me.xap3y.space.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Shared utilities for file processing operations (image/video tools).
 * Consolidates duplicated code from ImageToolsService and VideoToolsService.
 */
@Component
public class FileProcessingUtils {

    private FileProcessingUtils() {
    }

    /**
     * Extracts file extension from filename, or returns default if not found.
     */
    public static String getExtension(String filename, String defaultExt) {
        if (filename == null) return defaultExt;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return defaultExt;
        return filename.substring(dot).toLowerCase();
    }

    /**
     * Sanitizes filename by removing/replacing invalid characters.
     */
    public static String sanitizeFilename(String filename, String defaultName) {
        if (filename == null || filename.isBlank()) return defaultName;
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Extracts integer option from map, returns defaultVal if not found or invalid.
     */
    public static int intOpt(Map<String, String> opts, String key, int defaultVal) {
        String val = opts.get(key);
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * Extracts double option from map, returns defaultVal if not found or invalid.
     */
    public static double doubleOpt(Map<String, String> opts, String key, double defaultVal) {
        String val = opts.get(key);
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * Extracts boolean option from map, returns defaultVal if not found.
     */
    public static boolean boolOpt(Map<String, String> opts, String key, boolean defaultVal) {
        String val = opts.get(key);
        if (val == null || val.isBlank()) return defaultVal;
        return Boolean.parseBoolean(val);
    }

    /**
     * Escapes special characters in JSON strings to avoid malformed JSON.
     */
    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Recursively deletes directory and all its contents.
     */
    public static void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * Builds a file download response with appropriate headers.
     */
    public static ResponseEntity<byte[]> buildFileDownloadResponse(byte[] data, String filename, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentDisposition(org.springframework.http.ContentDisposition
                .attachment()
                .filename(filename)
                .build());
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
    }
}
