package me.xap3y.space.util;

import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.service.PrometheusMetricService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageCompressor {

    private final PrometheusMetricService prometheusMetricService;

    public ImageCompressor(PrometheusMetricService prometheusMetricService) {
        this.prometheusMetricService = prometheusMetricService;
    }

    /**
     * Compresses an image based on the original image's dimensions while maintaining the aspect ratio.
     *
     * @param imageInputStream InputStream of the uploaded image.
     * @param quality Quality factor between 0.0 (lowest) and 1.0 (highest).
     * @throws IOException If an error occurs during processing.
     */
    public void compressImage(InputStream imageInputStream, File file, double scale, float quality) throws IOException {
        prometheusMetricService.recordEvent(MetricRecordType.IMAGE_COMPRESSED);
        Thumbnails.of(imageInputStream)
                .scale(scale)
                .outputQuality(quality)
                .toFile(file);
    }
}