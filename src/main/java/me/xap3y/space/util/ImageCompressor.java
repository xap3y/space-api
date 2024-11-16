package me.xap3y.space.util;

import net.coobird.thumbnailator.Thumbnails;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImageCompressor {

    /**
     * Compresses an image based on the original image's dimensions while maintaining the aspect ratio.
     *
     * @param imageInputStream InputStream of the uploaded image.
     * @param quality Quality factor between 0.0 (lowest) and 1.0 (highest).
     * @throws IOException If an error occurs during processing.
     */
    public static void compressImage(InputStream imageInputStream, File file, double scale, float quality) throws IOException {

        Thumbnails.of(imageInputStream)
                .scale(scale)
                .outputQuality(quality)
                .toFile(file);
    }
}