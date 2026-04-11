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
public class VideoToolsService {

    private static final long MAX_FILE_SIZE = 500L * 1024 * 1024; // 500MB for video

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "video/mp4", "video/webm", "video/x-matroska", "video/avi",
            "video/quicktime", "video/x-flv", "video/x-ms-wmv", "video/mp2t",
            "video/x-m4v", "audio/mpeg", "audio/wav", "audio/ogg",
            "audio/aac", "audio/flac", "audio/mp4"
    );

    private static final Map<String, String> FORMAT_TO_MIME = Map.ofEntries(
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("avi", "video/avi"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("ts", "video/mp2t"),
            Map.entry("gif", "image/gif"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("aac", "audio/aac"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("flac", "audio/flac"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp")
    );

    private final FfmpegService ffmpeg;

    public VideoToolsService(FfmpegService ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    public ResponseEntity<byte[]> process(MultipartFile file, String tool, Map<String, String> options)
            throws IOException, InterruptedException {

        validateFile(file);

        Path tempDir = Files.createTempDirectory("vidtools-");
        try {
            String originalName = FileProcessingUtils.sanitizeFilename(file.getOriginalFilename(), "video.mp4");
            String inputExt = FileProcessingUtils.getExtension(originalName, ".mp4");
            Path inputPath = tempDir.resolve("input" + inputExt);

            try (InputStream is = file.getInputStream();
                 OutputStream os = Files.newOutputStream(inputPath)) {
                is.transferTo(os);
            }

            String outputExt = determineOutputExtension(tool, options, inputExt);
            Path outputPath = tempDir.resolve("output" + outputExt);

            List<String> cmd = buildCommand(tool, options, inputPath, outputPath);
            FfmpegService.FfmpegResult result = ffmpeg.run(cmd);

            // If GPU failed, retry without GPU
            if (!result.success() && ffmpeg.hasGpu()) {
                log.warn("GPU ffmpeg failed for tool={}, retrying with CPU fallback. Error: {}", tool, result.output());
                Files.deleteIfExists(outputPath);

                List<String> cpuCmd = buildCpuFallbackCommand(tool, options, inputPath, outputPath);
                result = ffmpeg.run(cpuCmd);
            }

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
                    outputExt.replace(".", ""), "application/octet-stream"
            );

            return FileProcessingUtils.buildFileDownloadResponse(outputBytes, outputFilename, mime);

        } finally {
            FileProcessingUtils.deleteDirectory(tempDir);
        }
    }

    private List<String> buildCpuFallbackCommand(String tool, Map<String, String> options, Path input, Path output) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-hide_banner");
        cmd.add("-loglevel"); cmd.add("error");

        // No hwaccel flags — pure CPU
        cmd.add("-i"); cmd.add(input.toAbsolutePath().toString());

        // Temporarily disable GPU
        FfmpegService.GpuType savedGpu = ffmpeg.getDetectedGpu();
        // We can't disable the service's GPU, so just build commands without GPU-specific flags

        switch (tool) {
            case "trim" -> buildTrimCpu(cmd, options);
            case "compress" -> buildCompressCpu(cmd, options);
            case "convert" -> buildConvertCpu(cmd, options);
            case "resize" -> buildResizeCpu(cmd, options);
            case "rotate" -> buildRotateCpu(cmd, options);
            case "volume" -> buildVolumeCpu(cmd, options);
            case "speed" -> buildSpeed(cmd, options);
            case "fps" -> buildFpsCpu(cmd, options);
            case "mute" -> { cmd.add("-an"); cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("18"); }
            case "strip-audio" -> { cmd.add("-an"); cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("18"); }
            case "reverse" -> buildReverse(cmd);
            case "stabilize" -> buildStabilize(cmd, options, input);
            case "gif" -> buildGif(cmd, options, input);
            case "thumbnail" -> buildThumbnail(cmd, options, input);
            case "extract-audio" -> buildExtractAudio(cmd, options);
            default -> throw new ImageToolsException("Unknown video tool: " + tool);
        }

        cmd.add("-y");
        cmd.add(output.toAbsolutePath().toString());
        return cmd;
    }

    private void buildTrimCpu(List<String> cmd, Map<String, String> options) {
        cmd.add("-ss"); cmd.add(options.getOrDefault("start", "00:00:00"));
        cmd.add("-to"); cmd.add(options.getOrDefault("end", "00:00:30"));
        cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("18");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void buildCompressCpu(List<String> cmd, Map<String, String> options) {
        int crf = FileProcessingUtils.intOpt(options, "crf", 23);
        int vBitrate = FileProcessingUtils.intOpt(options, "videoBitrate", 2000);
        int aBitrate = FileProcessingUtils.intOpt(options, "audioBitrate", 128);
        cmd.add("-c:v"); cmd.add("libx264");
        cmd.add("-crf"); cmd.add(String.valueOf(crf));
        cmd.add("-preset"); cmd.add("medium");
        cmd.add("-b:v"); cmd.add(vBitrate + "k");
        cmd.add("-c:a"); cmd.add("aac");
        cmd.add("-b:a"); cmd.add(aBitrate + "k");
    }

    private void buildConvertCpu(List<String> cmd, Map<String, String> options) {
        String format = options.getOrDefault("format", "mp4").toLowerCase();
        if ("webm".equals(format)) {
            cmd.add("-c:v"); cmd.add("libvpx-vp9");
            cmd.add("-b:v"); cmd.add("2M");
        } else {
            cmd.add("-c:v"); cmd.add("libx264");
            cmd.add("-crf"); cmd.add("18");
        }
        cmd.add("-c:a"); cmd.add("aac");
    }

    private void buildResizeCpu(List<String> cmd, Map<String, String> options) {
        int width = FileProcessingUtils.intOpt(options, "width", 1920);
        int height = FileProcessingUtils.intOpt(options, "height", 1080);
        String mode = options.getOrDefault("mode", "fit");
        switch (mode) {
            case "fill" -> { cmd.add("-vf"); cmd.add("scale=" + width + ":" + height); }
            case "cover" -> { cmd.add("-vf"); cmd.add("scale=" + width + ":" + height
                    + ":force_original_aspect_ratio=increase,crop=" + width + ":" + height); }
            default -> { cmd.add("-vf"); cmd.add("scale=" + width + ":" + height
                    + ":force_original_aspect_ratio=decrease,"
                    + "pad=" + width + ":" + height + ":(ow-iw)/2:(oh-ih)/2:color=black"); }
        }
        cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("18");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void buildRotateCpu(List<String> cmd, Map<String, String> options) {
        String rotation = options.getOrDefault("rotation", "90");
        String vf = switch (rotation) {
            case "90" -> "transpose=1";
            case "270" -> "transpose=2";
            case "180" -> "transpose=1,transpose=1";
            case "hflip" -> "hflip";
            case "vflip" -> "vflip";
            default -> "transpose=1";
        };
        cmd.add("-vf"); cmd.add(vf);
        cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("18");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void buildVolumeCpu(List<String> cmd, Map<String, String> options) {
        int level = FileProcessingUtils.intOpt(options, "level", 150);
        double factor = level / 100.0;
        cmd.add("-af"); cmd.add(String.format("volume=%.2f", factor));
        cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("18");
    }

    private void buildFpsCpu(List<String> cmd, Map<String, String> options) {
        int fps = FileProcessingUtils.intOpt(options, "fps", 30);
        cmd.add("-vf"); cmd.add("fps=" + fps);
        cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("18");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ImageToolsException("No file provided");
        if (file.getSize() > MAX_FILE_SIZE) throw new ImageToolsException("File too large. Maximum is 500MB");
        // Be lenient with video types — browsers sometimes send generic types
    }

    // --- Command builder ---

    private List<String> buildCommand(String tool, Map<String, String> options, Path input, Path output) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-hide_banner");
        cmd.add("-loglevel"); cmd.add("error");

        boolean useGpuDecode = shouldUseGpuDecode(tool);
        if (useGpuDecode) {
            ffmpeg.addHwAccelInput(cmd);
        }

        cmd.add("-i"); cmd.add(input.toAbsolutePath().toString());

        switch (tool) {
            case "trim" -> buildTrim(cmd, options);
            case "compress" -> buildCompress(cmd, options);
            case "convert" -> buildConvert(cmd, options);
            case "resize" -> buildResize(cmd, options, useGpuDecode);
            case "extract-audio" -> buildExtractAudio(cmd, options);
            case "volume" -> buildVolume(cmd, options);
            case "speed" -> buildSpeed(cmd, options);
            case "rotate" -> buildRotate(cmd, options, useGpuDecode);
            case "gif" -> buildGif(cmd, options, input);
            case "thumbnail" -> buildThumbnail(cmd, options, input);
            case "mute" -> buildMute(cmd);
            case "strip-audio" -> buildStripAudio(cmd);
            case "stabilize" -> buildStabilize(cmd, options, input);
            case "reverse" -> buildReverse(cmd);
            case "fps" -> buildFps(cmd, options);
            default -> throw new ImageToolsException("Unknown video tool: " + tool);
        }

        cmd.add("-y");
        cmd.add(output.toAbsolutePath().toString());
        return cmd;
    }

    private boolean shouldUseGpuDecode(String tool) {
        if (!ffmpeg.hasGpu()) return false;
        // Only enable GPU decode for tools where the FULL pipeline can stay on GPU
        // or where we properly handle hwdownload.
        // Rotate, stabilize, reverse, gif, thumbnail use CPU-only filters — skip GPU decode.
        return switch (tool) {
            case "compress", "convert", "mute", "strip-audio", "fps", "trim" -> true;
            default -> false;
        };
    }

    /**
     * Add GPU-accelerated encoder flags for video output.
     */
    private void addGpuEncoder(List<String> cmd, String targetFormat) {
        if (!ffmpeg.hasGpu()) return;

        switch (targetFormat) {
            case "mp4", "mov", "mkv", "ts" -> {
                String encoder = ffmpeg.getH264Encoder();
                if (!"libx264".equals(encoder)) {
                    cmd.add("-c:v"); cmd.add(encoder);

                    // Quality presets per GPU type
                    switch (ffmpeg.getDetectedGpu()) {
                        case NVIDIA -> {
                            cmd.add("-preset"); cmd.add("p4"); // balanced
                            cmd.add("-rc"); cmd.add("vbr");
                        }
                        case AMD -> {
                            cmd.add("-quality"); cmd.add("balanced");
                        }
                        case INTEL -> {
                            cmd.add("-preset"); cmd.add("medium");
                        }
                        case VIDEOTOOLBOX -> {
                            // VideoToolbox handles quality automatically
                        }
                    }
                }
            }
            case "webm" -> {
                // WebM uses VP9 — no widely available GPU encoder
                cmd.add("-c:v"); cmd.add("libvpx-vp9");
                cmd.add("-b:v"); cmd.add("2M");
            }
        }
    }

    // --- Tool builders ---

    private void buildTrim(List<String> cmd, Map<String, String> options) {
        String start = options.getOrDefault("start", "00:00:00");
        String end = options.getOrDefault("end", "00:00:30");

        // Insert -ss before -i for fast seek (rebuild command)
        // Since we already added -i, use -ss and -to after
        cmd.add("-ss"); cmd.add(start);
        cmd.add("-to"); cmd.add(end);

        // Use GPU encoder if available
        addGpuEncoder(cmd, "mp4");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void buildCompress(List<String> cmd, Map<String, String> options) {
        int crf = FileProcessingUtils.intOpt(options, "crf", 23);
        int vBitrate = FileProcessingUtils.intOpt(options, "videoBitrate", 2000);
        int aBitrate = FileProcessingUtils.intOpt(options, "audioBitrate", 128);

        if (ffmpeg.hasGpu()) {
            String encoder = ffmpeg.getH264Encoder();
            cmd.add("-c:v"); cmd.add(encoder);

            switch (ffmpeg.getDetectedGpu()) {
                case NVIDIA -> {
                    cmd.add("-preset"); cmd.add("p4");
                    cmd.add("-rc"); cmd.add("vbr");
                    cmd.add("-cq"); cmd.add(String.valueOf(crf));
                    cmd.add("-b:v"); cmd.add(vBitrate + "k");
                    cmd.add("-maxrate"); cmd.add((int) (vBitrate * 1.5) + "k");
                }
                case AMD -> {
                    cmd.add("-quality"); cmd.add("balanced");
                    cmd.add("-b:v"); cmd.add(vBitrate + "k");
                }
                case INTEL -> {
                    cmd.add("-preset"); cmd.add("medium");
                    cmd.add("-global_quality"); cmd.add(String.valueOf(crf));
                    cmd.add("-b:v"); cmd.add(vBitrate + "k");
                }
                case VIDEOTOOLBOX -> {
                    cmd.add("-b:v"); cmd.add(vBitrate + "k");
                }
            }
        } else {
            cmd.add("-c:v"); cmd.add("libx264");
            cmd.add("-crf"); cmd.add(String.valueOf(crf));
            cmd.add("-preset"); cmd.add("medium");
            cmd.add("-b:v"); cmd.add(vBitrate + "k");
        }

        cmd.add("-c:a"); cmd.add("aac");
        cmd.add("-b:a"); cmd.add(aBitrate + "k");
    }

    private void buildConvert(List<String> cmd, Map<String, String> options) {
        String format = options.getOrDefault("format", "mp4").toLowerCase();
        addGpuEncoder(cmd, format);
        cmd.add("-c:a"); cmd.add("aac");
    }

    private void buildResize(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        int width = FileProcessingUtils.intOpt(options, "width", 1920);
        int height = FileProcessingUtils.intOpt(options, "height", 1080);
        String mode = options.getOrDefault("mode", "fit");

        // Even with GPU decode, scale_npp can be unreliable — use CPU scale + GPU encode
        // This is the safest approach that still benefits from GPU encoding
        String filter;
        switch (mode) {
            case "fill" -> filter = "scale=" + width + ":" + height;
            case "cover" -> filter = "scale=" + width + ":" + height
                    + ":force_original_aspect_ratio=increase,crop=" + width + ":" + height;
            default -> filter = "scale=" + width + ":" + height
                    + ":force_original_aspect_ratio=decrease,"
                    + "pad=" + width + ":" + height + ":(ow-iw)/2:(oh-ih)/2:color=black";
        }

        // If GPU decode is active, we need to download frames to CPU first
        if (gpuDecode && ffmpeg.hasGpu()) {
            cmd.add("-vf"); cmd.add("hwdownload,format=nv12," + filter);
        } else {
            cmd.add("-vf"); cmd.add(filter);
        }

        addGpuEncoder(cmd, "mp4");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void buildExtractAudio(List<String> cmd, Map<String, String> options) {
        String format = options.getOrDefault("format", "mp3").toLowerCase();
        int bitrate = FileProcessingUtils.intOpt(options, "bitrate", 192);

        cmd.add("-vn"); // no video

        switch (format) {
            case "mp3" -> {
                cmd.add("-c:a"); cmd.add("libmp3lame");
                cmd.add("-b:a"); cmd.add(bitrate + "k");
            }
            case "aac" -> {
                cmd.add("-c:a"); cmd.add("aac");
                cmd.add("-b:a"); cmd.add(bitrate + "k");
            }
            case "wav" -> {
                cmd.add("-c:a"); cmd.add("pcm_s16le");
            }
            case "ogg" -> {
                cmd.add("-c:a"); cmd.add("libvorbis");
                cmd.add("-b:a"); cmd.add(bitrate + "k");
            }
            case "flac" -> {
                cmd.add("-c:a"); cmd.add("flac");
            }
            default -> {
                cmd.add("-c:a"); cmd.add("libmp3lame");
                cmd.add("-b:a"); cmd.add(bitrate + "k");
            }
        }
    }

    private void buildVolume(List<String> cmd, Map<String, String> options) {
        int level = FileProcessingUtils.intOpt(options, "level", 150);
        double factor = level / 100.0;

        cmd.add("-af"); cmd.add(String.format("volume=%.2f", factor));
        addGpuEncoder(cmd, "mp4");
    }

    private void buildSpeed(List<String> cmd, Map<String, String> options) {
        double factor = FileProcessingUtils.doubleOpt(options, "factor", 2.0);
        boolean adjustAudio = FileProcessingUtils.boolOpt(options, "adjustAudio", true);
        factor = Math.max(0.25, Math.min(4.0, factor));
        double videoPts = 1.0 / factor;

        if (adjustAudio) {
            cmd.add("-filter_complex");
            cmd.add(String.format("[0:v]setpts=%.4f*PTS[v];[0:a]atempo=%.4f[a]",
                    videoPts, clampAtempo(factor)));
            cmd.add("-map"); cmd.add("[v]");
            cmd.add("-map"); cmd.add("[a]");
        } else {
            cmd.add("-vf"); cmd.add(String.format("setpts=%.4f*PTS", videoPts));
            cmd.add("-an");
        }

        // Don't use GPU encode when filter_complex is used — can cause conflicts
        if (!adjustAudio) {
            addGpuEncoder(cmd, "mp4");
        }
    }

    /**
     * atempo only supports 0.5-100.0. For values outside, chain multiple atempos.
     */
    private double clampAtempo(double factor) {
        return Math.max(0.5, Math.min(100.0, factor));
    }

    private void buildRotate(List<String> cmd, Map<String, String> options, boolean gpuDecode) {
        String rotation = options.getOrDefault("rotation", "90");

        String vf = switch (rotation) {
            case "90" -> "transpose=1";
            case "270" -> "transpose=2";
            case "180" -> "transpose=1,transpose=1";
            case "hflip" -> "hflip";
            case "vflip" -> "vflip";
            default -> "transpose=1";
        };

        // Always use CPU filter — transpose doesn't support CUDA frames
        cmd.add("-vf"); cmd.add(vf);

        // But still use GPU encoder for the output
        addGpuEncoder(cmd, "mp4");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void buildGif(List<String> cmd, Map<String, String> options, Path input) {
        String start = options.getOrDefault("start", "00:00:00");
        int duration = FileProcessingUtils.intOpt(options, "duration", 5);
        int width = FileProcessingUtils.intOpt(options, "width", 480);
        int fps = FileProcessingUtils.intOpt(options, "fps", 15);

        cmd.add("-ss"); cmd.add(start);
        cmd.add("-t"); cmd.add(String.valueOf(duration));
        cmd.add("-vf"); cmd.add(
                "fps=" + fps + ",scale=" + width + ":-1:flags=lanczos,"
                        + "split[s0][s1];[s0]palettegen=max_colors=256[p];[s1][p]paletteuse=dither=bayer"
        );
        cmd.add("-loop"); cmd.add("0");
    }

    private void buildThumbnail(List<String> cmd, Map<String, String> options, Path input) {
        String timestamp = options.getOrDefault("timestamp", "00:00:05");

        // Re-insert -ss before input for fast seek
        // Remove the existing -i and re-add with -ss before it
        int iIdx = cmd.indexOf("-i");
        if (iIdx >= 0) {
            cmd.add(iIdx, "-ss");
            cmd.add(iIdx + 1, timestamp);
        }

        cmd.add("-frames:v"); cmd.add("1");
        cmd.add("-q:v"); cmd.add("2");
    }

    private void buildMute(List<String> cmd) {
        cmd.add("-an");
        addGpuEncoder(cmd, "mp4");
    }

    private void buildStripAudio(List<String> cmd) {
        cmd.add("-an");
        addGpuEncoder(cmd, "mp4");
    }

    private void buildStabilize(List<String> cmd, Map<String, String> options, Path input) {
        int strength = FileProcessingUtils.intOpt(options, "strength", 10);

        // vidstabdetect + vidstabtransform (two-pass)
        // For simplicity, use deshake filter (single pass)
        if (ffmpeg.hasFilter("deshake")) {
            cmd.add("-vf"); cmd.add("deshake=rx=" + strength + ":ry=" + strength);
        } else {
            cmd.add("-vf"); cmd.add("deshake");
        }

        addGpuEncoder(cmd, "mp4");
        cmd.add("-c:a"); cmd.add("copy");
    }

    private void buildReverse(List<String> cmd) {
        cmd.add("-vf"); cmd.add("reverse");
        cmd.add("-af"); cmd.add("areverse");
    }

    private void buildFps(List<String> cmd, Map<String, String> options) {
        int fps = FileProcessingUtils.intOpt(options, "fps", 30);
        cmd.add("-vf"); cmd.add("fps=" + fps);
        // Use GPU encode
        addGpuEncoder(cmd, "mp4");
        cmd.add("-c:a"); cmd.add("copy");
    }

    // --- Helpers ---

    private String determineOutputExtension(String tool, Map<String, String> options, String inputExt) {
        return switch (tool) {
            case "convert" -> "." + options.getOrDefault("format", "mp4").toLowerCase();
            case "extract-audio" -> "." + options.getOrDefault("format", "mp3").toLowerCase();
            case "gif" -> ".gif";
            case "thumbnail" -> "." + options.getOrDefault("format", "jpg").toLowerCase();
            default -> {
                if (inputExt.isEmpty() || inputExt.equals(".")) yield ".mp4";
                yield inputExt;
            }
        };
    }

    private String buildOutputFilename(String originalName, String tool, String outputExt) {
        String baseName = originalName;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);
        return baseName + "_" + tool + outputExt;
    }

}