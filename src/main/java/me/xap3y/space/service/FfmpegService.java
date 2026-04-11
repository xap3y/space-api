package me.xap3y.space.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class FfmpegService {

    private static final int TIMEOUT_SECONDS = 120;

    public enum GpuType {
        NONE, NVIDIA, AMD, INTEL, VIDEOTOOLBOX
    }

    private GpuType detectedGpu = GpuType.NONE;
    private final Set<String> availableEncoders = new HashSet<>();
    private final Set<String> availableDecoders = new HashSet<>();
    private final Set<String> availableFilters = new HashSet<>();

    @PostConstruct
    public void init() {
        detectEncoders();
        detectDecoders();
        detectFilters();
        detectGpu();
        log.info("FFmpeg GPU detection: {} | HW encoders available: {}", detectedGpu, availableEncoders);
    }

    // --- GPU Detection ---

    private void detectEncoders() {
        String output = runQuickCommand(List.of("ffmpeg", "-hide_banner", "-encoders"));
        if (output == null) return;
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            // Lines like: V..... h264_nvenc ...
            if (trimmed.length() > 7) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    availableEncoders.add(parts[1].toLowerCase());
                }
            }
        }
    }

    private void detectDecoders() {
        String output = runQuickCommand(List.of("ffmpeg", "-hide_banner", "-decoders"));
        if (output == null) return;
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.length() > 7) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    availableDecoders.add(parts[1].toLowerCase());
                }
            }
        }
    }

    private void detectFilters() {
        String output = runQuickCommand(List.of("ffmpeg", "-hide_banner", "-filters"));
        if (output == null) return;
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.length() > 4) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    availableFilters.add(parts[1].toLowerCase());
                }
            }
        }
    }

    private void detectGpu() {
        // Priority: NVIDIA > AMD > Intel > VideoToolbox (macOS) > NONE
        if (hasEncoder("h264_nvenc")) {
            detectedGpu = GpuType.NVIDIA;
            log.info("Detected NVIDIA GPU (NVENC available)");
        } else if (hasEncoder("h264_amf")) {
            detectedGpu = GpuType.AMD;
            log.info("Detected AMD GPU (AMF available)");
        } else if (hasEncoder("h264_qsv")) {
            detectedGpu = GpuType.INTEL;
            log.info("Detected Intel GPU (QSV available)");
        } else if (hasEncoder("h264_videotoolbox")) {
            detectedGpu = GpuType.VIDEOTOOLBOX;
            log.info("Detected macOS VideoToolbox");
        } else {
            detectedGpu = GpuType.NONE;
            log.info("No GPU acceleration detected, using CPU");
        }
    }

    private String runQuickCommand(List<String> cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            proc.waitFor(10, TimeUnit.SECONDS);
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to run command: {}", cmd, e);
            return null;
        }
    }

    // --- Public API ---

    public GpuType getDetectedGpu() {
        return detectedGpu;
    }

    public boolean hasEncoder(String name) {
        return availableEncoders.contains(name.toLowerCase());
    }

    public boolean hasDecoder(String name) {
        return availableDecoders.contains(name.toLowerCase());
    }

    public boolean hasFilter(String name) {
        return availableFilters.contains(name.toLowerCase());
    }

    public boolean hasGpu() {
        return detectedGpu != GpuType.NONE;
    }

    /**
     * Get the best available H264 encoder.
     */
    public String getH264Encoder() {
        return switch (detectedGpu) {
            case NVIDIA -> "h264_nvenc";
            case AMD -> "h264_amf";
            case INTEL -> "h264_qsv";
            case VIDEOTOOLBOX -> "h264_videotoolbox";
            default -> "libx264";
        };
    }

    /**
     * Get the best available HEVC/H265 encoder.
     */
    public String getHevcEncoder() {
        return switch (detectedGpu) {
            case NVIDIA -> hasEncoder("hevc_nvenc") ? "hevc_nvenc" : "libx265";
            case AMD -> hasEncoder("hevc_amf") ? "hevc_amf" : "libx265";
            case INTEL -> hasEncoder("hevc_qsv") ? "hevc_qsv" : "libx265";
            case VIDEOTOOLBOX -> hasEncoder("hevc_videotoolbox") ? "hevc_videotoolbox" : "libx265";
            default -> "libx265";
        };
    }

    /**
     * Add hardware input acceleration flags if GPU is available.
     * Call BEFORE -i flag.
     */
    public void addHwAccelInput(List<String> cmd) {
        switch (detectedGpu) {
            case NVIDIA -> {
                cmd.add("-hwaccel"); cmd.add("cuda");
                cmd.add("-hwaccel_output_format"); cmd.add("cuda");
            }
            case AMD -> {
                // AMF doesn't have a hwaccel input, uses CPU decode + GPU encode
            }
            case INTEL -> {
                cmd.add("-hwaccel"); cmd.add("qsv");
                cmd.add("-hwaccel_output_format"); cmd.add("qsv");
            }
            case VIDEOTOOLBOX -> {
                cmd.add("-hwaccel"); cmd.add("videotoolbox");
            }
            default -> {}
        }
    }

    /**
     * Wrap a filter string with GPU upload/download if needed.
     * NVIDIA CUDA requires: hwdownload,format=nv12 BEFORE cpu filters, then hwupload_cuda after.
     * For image processing we typically just download to CPU since filters are CPU-based.
     */
    public String wrapFilterForGpu(String filter) {
        if (detectedGpu == GpuType.NVIDIA) {
            return "hwdownload,format=nv12," + filter;
        }
        if (detectedGpu == GpuType.INTEL) {
            return "hwdownload,format=nv12," + filter;
        }
        return filter;
    }

    /**
     * For image processing — don't use GPU hwaccel input since images
     * don't benefit from GPU decode. Just use GPU encoder where applicable.
     */
    public void addImageOutputFlags(List<String> cmd, String outputExt) {
        // Images don't need video codec flags — ffmpeg picks the right one from extension
        // But for formats that support it, we can set encoder explicitly
        // Mostly a no-op for images; more useful for video
    }

    // --- Run ---

    public FfmpegResult run(List<String> cmd) throws IOException, InterruptedException {
        log.debug("Running ffmpeg: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        boolean finished = proc.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            proc.destroyForcibly();
            return new FfmpegResult(false, -1, "ffmpeg timed out after " + TIMEOUT_SECONDS + " seconds");
        }

        int exitCode = proc.exitValue();
        return new FfmpegResult(exitCode == 0, exitCode, output.toString().trim());
    }

    public record FfmpegResult(boolean success, int exitCode, String output) {}
}