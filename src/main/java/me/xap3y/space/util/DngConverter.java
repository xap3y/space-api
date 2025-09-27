package me.xap3y.space.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
public class DngConverter {

    public static File convertDngToJpg(File dngFile, String outputDir) throws IOException, InterruptedException {
        String outputFilePath = outputDir + "/" + dngFile.getName().replaceAll("\\.dng$", ".jpg");
        File outputFile = new File(outputFilePath);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", dngFile.getAbsolutePath(),
                outputFile.getAbsolutePath()
        );

        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to convert DNG to JPG. Exit code: " + exitCode);
        }

        return outputFile;
    }

    public static File convertDngToJpeg(MultipartFile dngFile, String outputDir, String outputFileName)
            throws IOException, InterruptedException {

        // Save MultipartFile to a temporary DNG file
        File tempDng = File.createTempFile("upload-", ".dng");
        dngFile.transferTo(tempDng);

        // Prepare output JPEG file
        File jpegFile = new File(outputDir, outputFileName + ".jpg");

        log.info("Converting DNG to JPEG: {} -> {}", tempDng.getAbsolutePath(), jpegFile.getAbsolutePath());
        // Run dcraw + ImageMagick convert
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "dcraw -c -w \"" + tempDng.getAbsolutePath() + "\" | convert -quality 85 - \"" + jpegFile.getAbsolutePath() + "\""
        );
        pb.inheritIO();
        log.info("Starting conversion process...");
        Process process = pb.start();
        log.info("Process started, waiting for it to finish...");
        int exitCode = process.waitFor();
        log.info("Conversion process finished with exit code: {}", exitCode);


        tempDng.delete();

        if (exitCode != 0) {
            throw new IOException("Failed to convert DNG to JPEG. Exit code: " + exitCode);
        }

        return jpegFile;
    }
}
