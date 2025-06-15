package me.xap3y.space.service;

import me.xap3y.space.api.enums.SegmentType;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.ShortUserDto;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ShortUserMapper;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class StatsService {

    private final ImageService imageService;
    private final UserService userService;
    private final ShortUserMapper shortUserMapper;
    private final PasteService pasteService;
    private final UrlService urlService;

    public StatsService(ImageService imageService, UserService userService, ShortUserMapper shortUserMapper, PasteService pasteService, UrlService urlService) {
        this.imageService = imageService;
        this.userService = userService;
        this.shortUserMapper = shortUserMapper;
        this.pasteService = pasteService;
        this.urlService = urlService;
    }

    public Map<String, Object> getTotalStats(LocalDateTime from, LocalDateTime to, SegmentType type) {
        long total;

        if (type == SegmentType.IMAGE) {
            total = imageService.getImageCount(from, to);
        } else if (type == SegmentType.PASTE) {
            total = pasteService.countByCreatedAtBetween(from, to);
        } else if (type == SegmentType.URL) {
            total = urlService.countByCreatedAtBetween(from, to);
        } else {
            total = 0;
        }

        Optional<Map<String, ?>> bestUploader = getBestUploader(from, to, type);

        final Map<String, Object> stats = new HashMap<>() {{
            put("total", total);
            put("bestUploader", bestUploader.orElse(null));
        }};

        return stats;
    }

    private Optional<Map<String, ?>> getBestUploader(LocalDateTime from, LocalDateTime to, SegmentType type) {
        Optional<Pair<Long, Long>> bestUploader;

        if (type == SegmentType.IMAGE) {
            bestUploader = imageService.findBestUploader(from, to);
        } else if (type == SegmentType.PASTE) {
            bestUploader = pasteService.findBestUploader(from, to);
        } else if (type == SegmentType.URL) {
            bestUploader = urlService.findBiggestCreatorInRangeWithId(from, to);
        } else {
            return Optional.empty();
        }

        if (bestUploader.isEmpty()) {
            return Optional.empty();
        }

        User user = userService.findById(bestUploader.get().getFirst()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShortUserDto shortUserDto = shortUserMapper.apply(user);

        final Map<String, Object> bestUploaderMap = new HashMap<>() {{
            put("user", shortUserDto);
            put("count", bestUploader.get().getSecond());
        }};

        return Optional.of(bestUploaderMap);
    }
}
