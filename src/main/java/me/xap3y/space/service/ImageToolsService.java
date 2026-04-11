package me.xap3y.space.service;

import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.ImageToolsException;
import me.xap3y.space.util.FileProcessingUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Service
public class ImageToolsService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif",
            "image/bmp", "image/tiff", "image/avif", "image/svg+xml"
    );

    private static final Map<String, String> FORMAT_TO_MIME = Map.ofEntries(
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("webp", "image/webp"),
            Map.entry("gif", "image/gif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("tiff", "image/tiff"),
            Map.entry("avif", "image/avif")
    );

    private final FfmpegService ffmpeg;

    public ImageToolsService(FfmpegService ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    public ResponseEntity<byte[]> process(MultipartFile file, String tool, Map<String, String> options)
            throws IOException, InterruptedException {

        validateFile(file);

        Path tempDir = Files.createTempDirectory("imgtools-");
        try {
            String originalName = FileProcessingUtils.sanitizeFilename(file.getOriginalFilename(), "image.png");
            String inputExt = FileProcessingUtils.getExtension(originalName, ".png");
            Path inputPath = tempDir.resolve("input" + inputExt);

            try (InputStream is = file.getInputStream();
                 OutputStream os = Files.newOutputStream(inputPath)) {
                is.transferTo(os);
            }

            String outputExt = determineOutputExtension(tool, options, inputExt);
            Path outputPath = tempDir.resolve("output" + outputExt);

            List<String> cmd = buildCommand(tool, options, inputPath, outputPath);

            FfmpegService.FfmpegResult result = ffmpeg.run(cmd);
            if (!result.success()) {
                log.error("ffmpeg failed for tool={} exit={} output:\n{}", tool, result.exitCode(), result.output());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(("{\"error\":\"Processing failed: " + FileProcessingUtils.escapeJson(result.output()) + "\"}").getBytes());
            }

            if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\":\"Output file was not created\"}".getBytes());
            }

            byte[] outputBytes = Files.readAllBytes(outputPath);

            String outputFilename = buildOutputFilename(originalName, tool, outputExt);
            String mime = FORMAT_TO_MIME.getOrDefault(
                    outputExt.replace(".", ""),
                    "application/octet-stream"
            );

            return FileProcessingUtils.buildFileDownloadResponse(outputBytes, outputFilename, mime);

        } finally {
            FileProcessingUtils.deleteDirectory(tempDir);
        }
    }

    // --- Validation ---

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageToolsException("No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageToolsException("File too large. Maximum is 20MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ImageToolsException("Unsupported file type: " + contentType);
        }
    }

    // --- Command builders ---

    private List<String> buildCommand(String tool, Map<String, String> options, Path input, Path output) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-hide_banner");
        cmd.add("-loglevel"); cmd.add("error");

        // For GPU-accelerated encoding on supported formats (AVIF with HEVC, etc.)
        // Images mostly stay on CPU for filters, GPU helps with encode step
        boolean useGpuDecode = shouldUseGpuDecode(tool);
        if (useGpuDecode) {
            ffmpeg.addHwAccelInput(cmd);
        }

        cmd.add("-i"); cmd.add(input.toAbsolutePath().toString());

        switch (tool) {
            case "resize" -> buildResize(cmd, options, useGpuDecode);
            case "compress" -> buildCompress(cmd, options, output);
            case "convert" -> buildConvert(cmd, options, output);
            case "crop" -> buildCrop(cmd, options, useGpuDecode);
            case "rotate" -> buildRotate(cmd, options, useGpuDecode);
            case "flip" -> buildFlip(cmd, options, useGpuDecode);
            case "blur" -> buildBlur(cmd, options, useGpuDecode);
            case "sharpen" -> buildSharpen(cmd, options, useGpuDecode);
            case "grayscale" -> buildGrayscale(cmd, useGpuDecode);
            case "brightness" -> buildBrightness(cmd, options, useGpuDecode);
            case "watermark" -> buildWatermark(cmd, options, useGpuDecode);
            case "strip-metadata" -> buildStripMetadata(cmd);
            default -> throw new ImageToolsException("Unknown tool: " + tool);
        }

        cmd.add("-y");
        cmd.add(output.toAbsolutePath().toString());
        return cmd;
    }

    /**
     * Decide whether GPU decode is worthwhile.
     * For most image tools, CPU decode is fine since we need CPU filters anyway.
     * GPU decode helps for large images or when the output uses GPU encoding.
     */
    private boolean shouldUseGpuDecode(String tool) {
        // For images, GPU hwaccel decode can cause issues with filter compatibility.
        // Only enable for tools where we specifically handle the GPU pipeline.
        // Most image filters are CPU-only in ffmpeg, so we skip GPU decode.
        return false;
    }

    private void buildResize(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        int width = FileProcessingUtils.intOpt(options, "width", 1920);
        int height = FileProcessingUtils.intOpt(options, "height", 1080);
        String mode = options.getOrDefault("mode", "fit");
        boolean maintain = FileProcessingUtils.boolOpt(options, "maintainAspect", true);

        String scaleFilter;

        // Try GPU-accelerated scale if NVIDIA
        boolean useNppScale = ffmpeg.getDetectedGpu() == FfmpegService.GpuType.NVIDIA
                && ffmpeg.hasFilter("scale_npp");

        if (useNppScale && "fill".equals(mode)) {
            // NPP scale can do exact dimensions
            scaleFilter = "hwupload_cuda,scale_npp=" + width + ":" + height + ",hwdownload,format=nv12";
        } else {
            // CPU scale — more flexible with pad/crop
            switch (mode) {
                case "fill" -> scaleFilter = "scale=" + width + ":" + height;
                case "cover" -> scaleFilter = "scale=" + width + ":" + height
                        + ":force_original_aspect_ratio=increase,"
                        + "crop=" + width + ":" + height;
                default -> {
                    if (maintain) {
                        scaleFilter = "scale=" + width + ":" + height
                                + ":force_original_aspect_ratio=decrease,"
                                + "pad=" + width + ":" + height + ":(ow-iw)/2:(oh-ih)/2:color=black";
                    } else {
                        scaleFilter = "scale=" + width + ":" + height;
                    }
                }
            }
        }

        cmd.add("-vf"); cmd.add(scaleFilter);
    }

    private void buildCompress(List<String> cmd, Map<String, String> options, Path output) {
        int quality = FileProcessingUtils.intOpt(options, "quality", 80);
        String ext = FileProcessingUtils.getExtension(output.getFileName().toString(), ".png").replace(".", "").toLowerCase();

        switch (ext) {
            case "jpg", "jpeg" -> {
                int qscale = Math.max(2, Math.min(31, 2 + (int) ((100 - quality) * 29.0 / 100)));
                cmd.add("-q:v"); cmd.add(String.valueOf(qscale));
            }
            case "webp" -> {
                cmd.add("-quality"); cmd.add(String.valueOf(quality));
            }
            case "png" -> {
                int level = Math.min(9, (int) ((100 - quality) * 9.0 / 100));
                cmd.add("-compression_level"); cmd.add(String.valueOf(level));
            }
            case "avif" -> {
                // AVIF via libaom-av1 or GPU SVT-AV1
                if (ffmpeg.hasEncoder("libaom-av1")) {
                    cmd.add("-c:v"); cmd.add("libaom-av1");
                    int crf = Math.max(0, Math.min(63, (int) ((100 - quality) * 63.0 / 100)));
                    cmd.add("-crf"); cmd.add(String.valueOf(crf));
                    cmd.add("-still-picture"); cmd.add("1");
                }
            }
            default -> {
                cmd.add("-q:v"); cmd.add(String.valueOf(Math.max(1, 31 - (quality * 30 / 100))));
            }
        }
    }

    private void buildConvert(List<String> cmd, Map<String, String> options, Path output) {
        String format = options.getOrDefault("format", "webp").toLowerCase();
        String ext = format;

        switch (ext) {
            case "avif" -> {
                // Use GPU-accelerated HEVC if available, else libaom-av1
                if (ffmpeg.hasEncoder("libaom-av1")) {
                    cmd.add("-c:v"); cmd.add("libaom-av1");
                    cmd.add("-crf"); cmd.add("30");
                    cmd.add("-still-picture"); cmd.add("1");
                }
            }
            case "webp" -> {
                cmd.add("-c:v"); cmd.add("libwebp");
                cmd.add("-quality"); cmd.add("85");
            }
            case "jpg", "jpeg" -> {
                cmd.add("-q:v"); cmd.add("5");
            }
            // png, bmp, tiff, gif — ffmpeg auto-handles from extension
        }
    }

    private void buildCrop(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        int x = FileProcessingUtils.intOpt(options, "x", 0);
        int y = FileProcessingUtils.intOpt(options, "y", 0);
        int w = FileProcessingUtils.intOpt(options, "w", 800);
        int h = FileProcessingUtils.intOpt(options, "h", 600);

        cmd.add("-vf"); cmd.add("crop=" + w + ":" + h + ":" + x + ":" + y);
    }

    private void buildRotate(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        int angle = FileProcessingUtils.intOpt(options, "angle", 90);

        switch (angle) {
            case 90 -> { cmd.add("-vf"); cmd.add("transpose=1"); }
            case 180 -> { cmd.add("-vf"); cmd.add("transpose=1,transpose=1"); }
            case 270 -> { cmd.add("-vf"); cmd.add("transpose=2"); }
            default -> {
                String radians = String.format("%.6f", angle * Math.PI / 180.0);
                cmd.add("-vf"); cmd.add("rotate=" + radians + ":fillcolor=none");
            }
        }
    }

    private void buildFlip(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        String direction = options.getOrDefault("direction", "horizontal");

        switch (direction) {
            case "vertical" -> { cmd.add("-vf"); cmd.add("vflip"); }
            case "both" -> { cmd.add("-vf"); cmd.add("hflip,vflip"); }
            default -> { cmd.add("-vf"); cmd.add("hflip"); }
        }
    }

    private void buildBlur(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        int radius = FileProcessingUtils.intOpt(options, "radius", 5);
        int power = Math.max(1, radius / 2);
        cmd.add("-vf"); cmd.add("boxblur=" + radius + ":" + power);
    }

    private void buildSharpen(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        int amount = FileProcessingUtils.intOpt(options, "amount", 2);
        int size = 5;
        String strength = String.format("%.1f", amount * 0.5);
        cmd.add("-vf"); cmd.add("unsharp=" + size + ":" + size + ":" + strength);
    }

    private void buildGrayscale(List<String> cmd, boolean gpuDecode) {
        cmd.add("-vf"); cmd.add("format=gray");
    }

    private void buildBrightness(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        int brightness = FileProcessingUtils.intOpt(options, "brightness", 0);
        int contrast = FileProcessingUtils.intOpt(options, "contrast", 0);
        int saturation = FileProcessingUtils.intOpt(options, "saturation", 0);

        double b = brightness / 100.0;
        double c = 1.0 + (contrast / 100.0);
        double s = 1.0 + (saturation / 100.0);

        cmd.add("-vf"); cmd.add(String.format("eq=brightness=%.2f:contrast=%.2f:saturation=%.2f", b, c, s));
    }

    private void buildWatermark(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        String text = options.getOrDefault("text", "Sample");
        String position = options.getOrDefault("position", "bottomright");
        int opacity = FileProcessingUtils.intOpt(options, "opacity", 50);
        int size = FileProcessingUtils.intOpt(options, "size", 24);

        String safeText = text.replace("'", "\\'").replace(":", "\\:");

        String posX, posY;
        switch (position) {
            case "topleft" -> { posX = "10"; posY = "10"; }
            case "topright" -> { posX = "w-tw-10"; posY = "10"; }
            case "bottomleft" -> { posX = "10"; posY = "h-th-10"; }
            case "center" -> { posX = "(w-tw)/2"; posY = "(h-th)/2"; }
            default -> { posX = "w-tw-10"; posY = "h-th-10"; }
        }

        double alpha = opacity / 100.0;

        String drawtext = String.format(
                "drawtext=text='%s':fontsize=%d:fontcolor=white@%.2f:x=%s:y=%s:shadowcolor=black@0.5:shadowx=2:shadowy=2",
                safeText, size, alpha, posX, posY
        );

        cmd.add("-vf"); cmd.add(drawtext);
    }

    private void buildStripMetadata(List<String> cmd) {
        cmd.add("-map_metadata"); cmd.add("-1");
        cmd.add("-fflags"); cmd.add("+bitexact");
    }

    // --- Helpers ---

    private String determineOutputExtension(String tool, Map<String, String> options, String inputExt) {
        if ("convert".equals(tool)) {
            String format = options.getOrDefault("format", "webp").toLowerCase();
            if ("jpg".equals(format)) format = "jpeg";
            return "." + format;
        }
        if (inputExt.isEmpty() || inputExt.equals(".")) {
            return ".png";
        }
        return inputExt;
    }

    private String buildOutputFilename(String originalName, String tool, String outputExt) {
        String baseName = originalName;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) {
            baseName = baseName.substring(0, dotIdx);
        }
        return baseName + "_" + tool + outputExt;
    }

}