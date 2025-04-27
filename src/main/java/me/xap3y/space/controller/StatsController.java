package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.StatsRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.service.UrlService;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/stats")
public class StatsController {


    private final ImageService imageService;
    private final PasteService pasteService;
    private final UrlService urlService;

    public StatsController(ImageService imageService, PasteService pasteService, UrlService urlService) {
        this.imageService = imageService;
        this.pasteService = pasteService;
        this.urlService = urlService;
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
        if (uploader == null) return new ResponseEntity<>(new DefaultResponse(true, "Unauthorized"), HttpStatus.UNAUTHORIZED);

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

        final List<Pair<LocalDate, Long>> imagesPerDay = imageService.findTotalImagesPerDayByUser(
                requestFilter.getFromDate(),
                requestFilter.getToDate(),
                uploader.getId(),
                requestFilter.getFillMissing() != null ? requestFilter.getFillMissing() : false
        );

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
