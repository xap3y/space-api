package me.xap3y.space.service;

import lombok.Getter;
import lombok.Setter;
import me.xap3y.space.repository.ImageRepository;
import me.xap3y.space.repository.PasteRepository;
import me.xap3y.space.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Getter
@Setter
public class MetricService {

    private boolean databaseUpdated = true;
    private final ImageRepository imageRepository;
    private final PasteRepository pasteRepository;
    private final UrlRepository urlRepository;

    private int sessionImagesUploaded = 0;
    private int sessionPastesCreated = 0;
    private int sessionUrlsShortened = 0;

    private long totalImagesUploaded;
    private long totalPastesCreated;
    private long totalUrlsShortened;

    private long todayImagesUploaded;
    private long todayPastesCreated;
    private long todayUrlsShortened;

    public MetricService(ImageRepository imageRepository, PasteRepository pasteRepository, UrlRepository urlRepository) {
        this.imageRepository = imageRepository;
        this.pasteRepository = pasteRepository;
        this.urlRepository = urlRepository;
        updateData();
    }


    private void updateData() {
        totalImagesUploaded = imageRepository.count();
        totalPastesCreated = pasteRepository.count();
        totalUrlsShortened = urlRepository.count();

        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime now = LocalDateTime.now();

        //todayImagesUploaded = imageRepository.countByUploadTimeAfter(LocalDate.now().atStartOfDay());
        todayImagesUploaded = imageRepository.countByUploadTimeBetween(startOfToday, now);
        todayPastesCreated = pasteRepository.countByCreatedAtBetween(startOfToday, now);
        todayUrlsShortened = urlRepository.countByCreatedAtBetween(startOfToday, now);
        
        this.databaseUpdated = false;
    }


    public long getTotalImagesUploaded() {
        if (!databaseUpdated) return totalImagesUploaded;
        updateData();
        return totalImagesUploaded;
    }

    public long getTotalPastesCreated() {
        if (!databaseUpdated) return totalPastesCreated;
        updateData();
        return totalPastesCreated;
    }

    public long getTotalUrlsShortened() {
        if (!databaseUpdated) return totalUrlsShortened;
        updateData();
        return totalUrlsShortened;
    }
}
