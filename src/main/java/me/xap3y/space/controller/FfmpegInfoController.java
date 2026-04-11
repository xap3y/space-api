package me.xap3y.space.controller;

import me.xap3y.space.service.FfmpegService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tools")
public class FfmpegInfoController {

    private final FfmpegService ffmpegService;

    public FfmpegInfoController(FfmpegService ffmpegService) {
        this.ffmpegService = ffmpegService;
    }

    @GetMapping("/gpu-info")
    public Map<String, Object> gpuInfo() {
        return Map.of(
                "gpu", ffmpegService.getDetectedGpu().name(),
                "hasGpu", ffmpegService.hasGpu(),
                "h264Encoder", ffmpegService.getH264Encoder(),
                "hevcEncoder", ffmpegService.getHevcEncoder(),
                "hasNvenc", ffmpegService.hasEncoder("h264_nvenc"),
                "hasAmf", ffmpegService.hasEncoder("h264_amf"),
                "hasQsv", ffmpegService.hasEncoder("h264_qsv"),
                "hasVideoToolbox", ffmpegService.hasEncoder("h264_videotoolbox")
        );
    }
}