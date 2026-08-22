package me.xap3y.space.service;

import me.xap3y.space.model.UserStats;
import me.xap3y.space.repository.FileRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HelperService {

    private final ImageService imageService;
    private final UrlService urlService;
    private final PasteService pasteService;
    private final FileRepository fileRepository;

    public HelperService(ImageService imageService, UrlService urlService, PasteService pasteService, FileRepository fileRepository) {
        this.imageService = imageService;
        this.urlService = urlService;
        this.pasteService = pasteService;
        this.fileRepository = fileRepository;
    }

    public UserStats getUserStats(long uid) {
        Map<String, ?> map = imageService.getUserStats(uid);

        UserStats stats = new UserStats();

        long imageBytes = 0;
        int imageUploads = 0;
        if (map != null) {
            Object size = map.get("total_size");
            Object uploads = map.get("uploads");
            if (size instanceof Number number) imageBytes = number.longValue();
            if (uploads instanceof Number number) imageUploads = number.intValue();
        }
        Long fileBytes = fileRepository.sumStorageByUploaderId(uid);
        stats.setStorageUsed(imageBytes + (fileBytes == null ? 0 : fileBytes));
        stats.setTotalUploads(imageUploads + Math.toIntExact(fileRepository.countByUploaderId(uid)));
        stats.setUrlsShortened(urlService.countUrlsByUserId(uid));
        stats.setPastesCreated(pasteService.countPastesByUserId(uid));
        return stats;
    }
}
