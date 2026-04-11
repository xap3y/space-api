package me.xap3y.space.controller;

import me.xap3y.space.service.VideoToolsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/v1/tools/video")
public class VideoToolsController {

    private final VideoToolsService videoToolsService;

    public VideoToolsController(VideoToolsService videoToolsService) {
        this.videoToolsService = videoToolsService;
    }

    @PostMapping("/trim")
    public ResponseEntity<byte[]> trim(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "00:00:00") String start,
            @RequestParam(defaultValue = "00:00:30") String end
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "trim", Map.of("start", start, "end", end));
    }

    @PostMapping("/compress")
    public ResponseEntity<byte[]> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "23") int crf,
            @RequestParam(defaultValue = "2000") int videoBitrate,
            @RequestParam(defaultValue = "128") int audioBitrate
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "compress", Map.of(
                "crf", String.valueOf(crf),
                "videoBitrate", String.valueOf(videoBitrate),
                "audioBitrate", String.valueOf(audioBitrate)
        ));
    }

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "mp4") String format
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "convert", Map.of("format", format));
    }

    @PostMapping("/resize")
    public ResponseEntity<byte[]> resize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1920") int width,
            @RequestParam(defaultValue = "1080") int height,
            @RequestParam(defaultValue = "fit") String mode
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "resize", Map.of(
                "width", String.valueOf(width),
                "height", String.valueOf(height),
                "mode", mode
        ));
    }

    @PostMapping("/extract-audio")
    public ResponseEntity<byte[]> extractAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "mp3") String format,
            @RequestParam(defaultValue = "192") int bitrate
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "extract-audio", Map.of(
                "format", format, "bitrate", String.valueOf(bitrate)
        ));
    }

    @PostMapping("/volume")
    public ResponseEntity<byte[]> volume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "150") int level
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "volume", Map.of("level", String.valueOf(level)));
    }

    @PostMapping("/speed")
    public ResponseEntity<byte[]> speed(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "2") double factor,
            @RequestParam(defaultValue = "true") boolean adjustAudio
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "speed", Map.of(
                "factor", String.valueOf(factor),
                "adjustAudio", String.valueOf(adjustAudio)
        ));
    }

    @PostMapping("/rotate")
    public ResponseEntity<byte[]> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "90") String rotation
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "rotate", Map.of("rotation", rotation));
    }

    @PostMapping("/gif")
    public ResponseEntity<byte[]> gif(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "00:00:00") String start,
            @RequestParam(defaultValue = "5") int duration,
            @RequestParam(defaultValue = "480") int width,
            @RequestParam(defaultValue = "15") int fps
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "gif", Map.of(
                "start", start, "duration", String.valueOf(duration),
                "width", String.valueOf(width), "fps", String.valueOf(fps)
        ));
    }

    @PostMapping("/thumbnail")
    public ResponseEntity<byte[]> thumbnail(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "00:00:05") String timestamp,
            @RequestParam(defaultValue = "jpg") String format
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "thumbnail", Map.of(
                "timestamp", timestamp, "format", format
        ));
    }

    @PostMapping("/mute")
    public ResponseEntity<byte[]> mute(
            @RequestParam("file") MultipartFile file
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "mute", Map.of());
    }

    @PostMapping("/strip-audio")
    public ResponseEntity<byte[]> stripAudio(
            @RequestParam("file") MultipartFile file
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "strip-audio", Map.of());
    }

    @PostMapping("/stabilize")
    public ResponseEntity<byte[]> stabilize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "10") int strength
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "stabilize", Map.of("strength", String.valueOf(strength)));
    }

    @PostMapping("/reverse")
    public ResponseEntity<byte[]> reverse(
            @RequestParam("file") MultipartFile file
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "reverse", Map.of());
    }

    @PostMapping("/fps")
    public ResponseEntity<byte[]> fps(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "30") int targetFps
    ) throws IOException, InterruptedException {
        return videoToolsService.process(file, "fps", Map.of("fps", String.valueOf(targetFps)));
    }
}
