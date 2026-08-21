package me.xap3y.space.service;

import me.xap3y.space.repository.ImageRepository;
import me.xap3y.space.repository.PasteRepository;
import me.xap3y.space.repository.TempMailRepository;
import me.xap3y.space.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserAnalyticsService {

    private final ImageRepository imageRepository;
    private final PasteRepository pasteRepository;
    private final UrlRepository urlRepository;
    private final TempMailRepository tempMailRepository;

    public UserAnalyticsService(ImageRepository imageRepository,
                                PasteRepository pasteRepository,
                                UrlRepository urlRepository,
                                TempMailRepository tempMailRepository) {
        this.imageRepository = imageRepository;
        this.pasteRepository = pasteRepository;
        this.urlRepository = urlRepository;
        this.tempMailRepository = tempMailRepository;
    }

    public Map<String, Object> getAnalytics(Long userId,
                                             LocalDateTime from,
                                             LocalDateTime to) {
        LocalDateTime rangeStart = from.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime rangeEnd = to.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        LocalDate startDay = rangeStart.toLocalDate();
        LocalDate endDay = rangeEnd.toLocalDate();

        Map<LocalDate, Long> images = dailyValues(
                imageRepository.findTotalImagesPerDayByUser(rangeStart, rangeEnd, userId));
        Map<LocalDate, Long> pastes = dailyValues(
                pasteRepository.findTotalPastesPerDayByUser(rangeStart, rangeEnd, userId));
        Map<LocalDate, Long> urls = dailyValues(
                urlRepository.findTotalUrlsPerDayByUser(rangeStart, rangeEnd, userId));
        Map<LocalDate, Long> tempMails = dailyValues(
                tempMailRepository.findTotalPerDayByUser(rangeStart, rangeEnd, userId));
        Map<LocalDate, Long> storage = dailyValues(
                imageRepository.findStoragePerDayByUser(rangeStart, rangeEnd, userId));

        long cumulativeStorage = number(imageRepository.sumStorageByUploaderIdBefore(userId, rangeStart));
        List<Map<String, Object>> daily = new ArrayList<>();
        long totalImages = 0;
        long totalPastes = 0;
        long totalUrls = 0;
        long totalTempMails = 0;
        long storageAdded = 0;

        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            long dayImages = images.getOrDefault(day, 0L);
            long dayPastes = pastes.getOrDefault(day, 0L);
            long dayUrls = urls.getOrDefault(day, 0L);
            long dayTempMails = tempMails.getOrDefault(day, 0L);
            long dayStorage = storage.getOrDefault(day, 0L);
            cumulativeStorage += dayStorage;
            totalImages += dayImages;
            totalPastes += dayPastes;
            totalUrls += dayUrls;
            totalTempMails += dayTempMails;
            storageAdded += dayStorage;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.toString());
            point.put("images", dayImages);
            point.put("pastes", dayPastes);
            point.put("urls", dayUrls);
            point.put("tempMails", dayTempMails);
            point.put("storageAddedBytes", dayStorage);
            point.put("storageBytes", cumulativeStorage);
            daily.add(point);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("images", totalImages);
        summary.put("pastes", totalPastes);
        summary.put("urls", totalUrls);
        summary.put("tempMails", totalTempMails);
        summary.put("storageAddedBytes", storageAdded);
        summary.put("storageBytes", number(imageRepository.sumStorageByUploaderId(userId)));
        summary.put("urlVisits", number(urlRepository.sumVisitsByUser(rangeStart, rangeEnd, userId)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", rangeStart);
        result.put("to", rangeEnd);
        result.put("daily", daily);
        result.put("summary", summary);
        result.put("fileTypes", categorized(
                imageRepository.findFileTypesByUser(rangeStart, rangeEnd, userId), true));
        result.put("storageLocations", categorized(
                imageRepository.findLocationsByUser(rangeStart, rangeEnd, userId), true));
        result.put("visibility", visibility(
                imageRepository.findVisibilityByUser(rangeStart, rangeEnd, userId)));
        result.put("pasteLanguages", categorized(
                pasteRepository.findLanguagesByUser(rangeStart, rangeEnd, userId), false));
        result.put("mailStatuses", categorized(
                tempMailRepository.findStatusesByUser(rangeStart, rangeEnd, userId), false));
        return result;
    }

    private Map<LocalDate, Long> dailyValues(List<Object[]> rows) {
        Map<LocalDate, Long> values = new LinkedHashMap<>();
        for (Object[] row : rows)
            values.put(localDate(row[0]), number(row[1]));
        return values;
    }

    private List<Map<String, Object>> categorized(List<Object[]> rows, boolean includeBytes) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("label", row[0] == null ? "Unknown" : row[0].toString());
            value.put("count", number(row[1]));
            if (includeBytes && row.length > 2)
                value.put("bytes", number(row[2]));
            values.add(value);
        }
        return values;
    }

    private List<Map<String, Object>> visibility(List<Object[]> rows) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("label", Boolean.TRUE.equals(row[0]) ? "Public" : "Private");
            value.put("count", number(row[1]));
            values.add(value);
        }
        return values;
    }

    private LocalDate localDate(Object value) {
        if (value instanceof LocalDate date)
            return date;
        if (value instanceof Date date)
            return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
