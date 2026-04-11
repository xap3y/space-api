package me.xap3y.space.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.service.ImageToolsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/tools/image")
@AllArgsConstructor
public class ImageToolController {

    private final ImageToolsService imageToolsService;

    @PostMapping("/resize")
    public ResponseEntity<byte[]> resize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1920") int width,
            @RequestParam(defaultValue = "1080") int height,
            @RequestParam(defaultValue = "fit") String mode,
            @RequestParam(defaultValue = "true") boolean maintainAspect
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "resize", Map.of(
                "width", String.valueOf(width),
                "height", String.valueOf(height),
                "mode", mode,
                "maintainAspect", String.valueOf(maintainAspect)
        ));
    }

    @PostMapping("/compress")
    public ResponseEntity<byte[]> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "80") int quality
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "compress", Map.of(
                "quality", String.valueOf(quality)
        ));
    }

    @PostMapping("/convert")
    public ResponseEntity<byte[]> convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "webp") String format
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "convert", Map.of(
                "format", format
        ));
    }

    @PostMapping("/crop")
    public ResponseEntity<byte[]> crop(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int x,
            @RequestParam(defaultValue = "0") int y,
            @RequestParam(defaultValue = "800") int w,
            @RequestParam(defaultValue = "600") int h
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "crop", Map.of(
                "x", String.valueOf(x),
                "y", String.valueOf(y),
                "w", String.valueOf(w),
                "h", String.valueOf(h)
        ));
    }

    @PostMapping("/rotate")
    public ResponseEntity<byte[]> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "90") int angle
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "rotate", Map.of(
                "angle", String.valueOf(angle)
        ));
    }

    @PostMapping("/flip")
    public ResponseEntity<byte[]> flip(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "horizontal") String direction
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "flip", Map.of(
                "direction", direction
        ));
    }

    @PostMapping("/blur")
    public ResponseEntity<byte[]> blur(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "5") int radius
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "blur", Map.of(
                "radius", String.valueOf(radius)
        ));
    }

    @PostMapping("/sharpen")
    public ResponseEntity<byte[]> sharpen(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "2") int amount
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "sharpen", Map.of(
                "amount", String.valueOf(amount)
        ));
    }

    @PostMapping("/grayscale")
    public ResponseEntity<byte[]> grayscale(
            @RequestParam("file") MultipartFile file
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "grayscale", Map.of());
    }

    @PostMapping("/brightness")
    public ResponseEntity<byte[]> brightness(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int brightness,
            @RequestParam(defaultValue = "0") int contrast,
            @RequestParam(defaultValue = "0") int saturation
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "brightness", Map.of(
                "brightness", String.valueOf(brightness),
                "contrast", String.valueOf(contrast),
                "saturation", String.valueOf(saturation)
        ));
    }

    @PostMapping("/watermark")
    public ResponseEntity<byte[]> watermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "Sample") String text,
            @RequestParam(defaultValue = "bottomright") String position,
            @RequestParam(defaultValue = "50") int opacity,
            @RequestParam(defaultValue = "24") int size
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "watermark", Map.of(
                "text", text,
                "position", position,
                "opacity", String.valueOf(opacity),
                "size", String.valueOf(size)
        ));
    }

    @PostMapping("/strip-metadata")
    public ResponseEntity<byte[]> stripMetadata(
            @RequestParam("file") MultipartFile file
    ) throws IOException, InterruptedException {
        return imageToolsService.process(file, "strip-metadata", Map.of());
    }
}
