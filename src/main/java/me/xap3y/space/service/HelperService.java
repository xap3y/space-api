package me.xap3y.space.service;

import me.xap3y.space.model.UserStats;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HelperService {

    private final ImageService imageService;
    private final UrlService urlService;
    private final PasteService pasteService;

    public HelperService(ImageService imageService, UrlService urlService, PasteService pasteService) {
        this.imageService = imageService;
        this.urlService = urlService;
        this.pasteService = pasteService;
    }

    public UserStats getUserStats(long uid) {
        Map<String, ?> map = imageService.getUserStats(uid);

        UserStats stats = new UserStats();

        if (map != null) {
            try {
                stats.setStorageUsed((Long) map.get("total_size"));
                stats.setTotalUploads(Integer.parseInt(((Long) map.get("uploads")).toString()));
                stats.setUrlsShortened(urlService.countUrlsByUserId(uid));
                stats.setPastesCreated(pasteService.countPastesByUserId(uid));
            } catch (Exception e) {
                // IGNORE
            }
        }
        return stats;
    }
}
