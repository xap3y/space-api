package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.SegmentType;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.request.StatsRequest;
import me.xap3y.space.model.request.TotalStatsRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.*;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/stats")
public class StatsController {


    private final ImageService imageService;
    private final PasteService pasteService;
    private final UrlService urlService;
    private final StatsService statsService;
    private final UserService userService;
    private final UserAnalyticsService userAnalyticsService;

    public StatsController(ImageService imageService, PasteService pasteService, UrlService urlService, StatsService statsService, UserService userService, UserAnalyticsService userAnalyticsService) {
        this.imageService = imageService;
        this.pasteService = pasteService;
        this.urlService = urlService;
        this.statsService = statsService;
        this.userService = userService;
        this.userAnalyticsService = userAnalyticsService;
    }

    @PostMapping("/me/analytics")
    @RequiresApiKey
    public ResponseEntity<?> getUserAnalytics(HttpServletRequest request,
                                              @RequestBody(required = false) StatsRequest body) {
        User uploader = (User) request.getAttribute("uploader");
        LocalDateTime to = body != null && body.getToDate() != null
                ? body.getToDate() : LocalDateTime.now();
        LocalDateTime from = body != null && body.getFromDate() != null
                ? body.getFromDate() : to.minusMonths(1);
        if (from.isAfter(to))
            throw new BadRequestException("fromDate must be before toDate");
        if (from.isBefore(to.minusYears(5)))
            throw new BadRequestException("Analytics range cannot exceed 5 years");

        return ResponseEntity.ok(new DefaultResponse(false,
                userAnalyticsService.getAnalytics(uploader.getId(), from, to)));
    }

    @PostMapping(
            value = "/get"
    )
    @RequiresApiKey
    public ResponseEntity<?> getAllStats(
            HttpServletRequest request,
            @RequestBody(required = true) TotalStatsRequest body
    ) {

        LocalDateTime fromDate;
        LocalDateTime toDate;

        if (body.getPreset() == null && (body.getFromDate() == null || body.getToDate() == null)) {
            throw new BadRequestException("Fill all required fields");
        } else if (body.getPreset() != null) {
            fromDate = body.getPreset().getFrom();
            toDate = body.getPreset().getTo();
        } else {
            fromDate = body.getFromDate();
            toDate = body.getToDate();
        }

        final Map<String, Object> allStats = new HashMap<>();

        for (SegmentType type : SegmentType.values()) {
            allStats.put(type.name().toLowerCase(), statsService.getTotalStats(fromDate, toDate, type));
        }

        allStats.put("storageUsed", imageService.getStorageUsedInRange(fromDate, toDate));

        return new ResponseEntity<>(new DefaultResponse(false, allStats), HttpStatus.OK);
    }

    @PostMapping(
            value = "/all"
    )
    @RequiresApiKey
    public ResponseEntity<?> getAllStats(
            HttpServletRequest request,
            @RequestBody(required = false) StatsRequest body
    ) {
        User uploader = (User) request.getAttribute("uploader");

        StatsRequest requestFilter = new StatsRequest();
        if (body == null) {
            requestFilter.setLimit(0);
            requestFilter.setToDate(LocalDateTime.now());
            requestFilter.setFromDate(LocalDateTime.now().minusDays(7));
            requestFilter.setFillMissing(false);
        } else {
            requestFilter.setFillMissing((body.getFillMissing() != null) ? body.getFillMissing() : false);
            requestFilter.setToDate((body.getToDate() != null) ? body.getToDate() : LocalDateTime.now());
            requestFilter.setFromDate((body.getFromDate() != null) ? body.getFromDate() : LocalDateTime.now().minusWeeks(1));
            if (body.getLimit() != null) {
                if (body.getLimit() > 1000) {
                    return ResponseEntity.badRequest().body("Limit cannot be greater than 1000");
                } else if (body.getLimit() < 0) {
                    return ResponseEntity.badRequest().body("Limit cannot be less than 0");
                }
                requestFilter.setLimit(body.getLimit());
            } else {
                requestFilter.setLimit(0);
            }
        }

        final long totalImages = imageService.countByUploadTimeBetweenAndUploaderId(
                requestFilter.getFromDate(),
                requestFilter.getToDate(),
                uploader.getId()
        );

        log.info("Total images uploaded by user {}: {}", uploader.getId(), totalImages);

        final List<Pair<LocalDate, Long>> imagesPerDay = imageService.findTotalImagesPerDayByUser(
                requestFilter.getFromDate(),
                requestFilter.getToDate(),
                uploader.getId(),
                requestFilter.getFillMissing() != null ? requestFilter.getFillMissing() : false
        );

        log.info("Images per day for user {}: {}", uploader.getId(), imagesPerDay);

        final List<Pair<LocalDate, Long>> pastesPerDay = pasteService.findTotalImagesPerDayByUser(
                requestFilter.getFromDate(),
                requestFilter.getToDate(),
                uploader.getId(),
                requestFilter.getFillMissing() != null ? requestFilter.getFillMissing() : false
        );

        final List<Pair<LocalDate, Long>> urlsPerDay = urlService.findTotalUrlsPerDayByUser(
                requestFilter.getFromDate(),
                requestFilter.getToDate(),
                uploader.getId(),
                requestFilter.getFillMissing() != null ? requestFilter.getFillMissing() : false
        );

        final Map<String, Object> stats = Map.of(
                "totalImages", totalImages,
                "imagesPerDay", imagesPerDay,
                "pastesPerDay", pastesPerDay,
                "urlsPerDay", urlsPerDay
        );

        return new ResponseEntity<>(new DefaultResponse(false, stats), HttpStatus.OK);
    }
}
