package me.xap3y.space.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import me.xap3y.space.api.enums.ImageLocation;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.dto.ImageInfoDto;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.entity.Image;
import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.Url;
import me.xap3y.space.mapper.ImageMapper;
import me.xap3y.space.mapper.PasteMapper;
import me.xap3y.space.mapper.ShortUrlMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.PagedResponse;
import me.xap3y.space.repository.ImageRepository;
import me.xap3y.space.repository.PasteRepository;
import me.xap3y.space.repository.UrlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/admin")
@AllArgsConstructor
public class AdminResourceController {

    private final PasteRepository pasteRepository;
    private final UrlRepository urlRepository;
    private final ImageRepository imageRepository;

    private final PasteMapper pasteMapper;
    private final ShortUrlMapper shortUrlMapper;
    private final ImageMapper imageMapper;

    @GetMapping("/pastes")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getPastes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<Long> includeUsers,
            @RequestParam(required = false) List<Long> excludeUsers,
            @RequestParam(required = false) String uniqueId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Paste> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (includeUsers != null && !includeUsers.isEmpty()) {
                predicates.add(root.get("createdBy").get("id").in(includeUsers));
            }
            if (excludeUsers != null && !excludeUsers.isEmpty()) {
                predicates.add(cb.not(root.get("createdBy").get("id").in(excludeUsers)));
            }
            if (uniqueId != null && !uniqueId.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("uniqueId")), "%" + uniqueId.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Paste> pastes = pasteRepository.findAll(spec, pageable);
        Page<PasteDto> pasteDtos = pastes.map(paste -> pasteMapper.apply(paste, false));

        PagedResponse<PasteDto> response = new PagedResponse<>(
                pasteDtos.getContent(),
                pasteDtos.getTotalElements(),
                pasteDtos.getTotalPages(),
                pasteDtos.getNumber(),
                pasteDtos.getSize()
        );

        return ResponseEntity.ok(new DefaultResponse(false, response));
    }

    @GetMapping("/urls")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getUrls(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<Long> includeUsers,
            @RequestParam(required = false) List<Long> excludeUsers,
            @RequestParam(required = false) String uniqueId,
            @RequestParam(required = false) Integer minVisits,
            @RequestParam(required = false) Integer maxUses,
            @RequestParam(required = false) Boolean expired
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Url> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (includeUsers != null && !includeUsers.isEmpty()) {
                predicates.add(root.get("createdBy").get("id").in(includeUsers));
            }
            if (excludeUsers != null && !excludeUsers.isEmpty()) {
                predicates.add(cb.not(root.get("createdBy").get("id").in(excludeUsers)));
            }
            if (uniqueId != null && !uniqueId.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("shortCode")), "%" + uniqueId.trim().toLowerCase() + "%"));
            }
            if (minVisits != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("visits"), minVisits));
            }
            if (maxUses != null) {
                predicates.add(cb.equal(root.get("maxUses"), maxUses));
            }
            if (expired != null) {
                if (expired) {
                    Predicate isExpiredDate = cb.and(cb.isNotNull(root.get("expiresAt")), cb.lessThan(root.get("expiresAt"), LocalDateTime.now()));
                    Predicate isExpiredUses = cb.and(cb.notEqual(root.get("maxUses"), -1), cb.greaterThanOrEqualTo(root.get("visits"), root.get("maxUses")));
                    predicates.add(cb.or(isExpiredDate, isExpiredUses));
                } else {
                    Predicate isActiveDate = cb.or(cb.isNull(root.get("expiresAt")), cb.greaterThan(root.get("expiresAt"), LocalDateTime.now()));
                    Predicate isActiveUses = cb.or(cb.equal(root.get("maxUses"), -1), cb.lessThan(root.get("visits"), root.get("maxUses")));
                    predicates.add(cb.and(isActiveDate, isActiveUses));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Url> urls = urlRepository.findAll(spec, pageable);
        Page<ShortUrlDto> urlDtos = urls.map(shortUrlMapper);

        PagedResponse<ShortUrlDto> response = new PagedResponse<>(
                urlDtos.getContent(),
                urlDtos.getTotalElements(),
                urlDtos.getTotalPages(),
                urlDtos.getNumber(),
                urlDtos.getSize()
        );

        return ResponseEntity.ok(new DefaultResponse(false, response));
    }

    @GetMapping("/images")
    @RequiresSpecialApiKey
    public ResponseEntity<?> getImages(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<Long> includeUsers,
            @RequestParam(required = false) List<Long> excludeUsers,
            @RequestParam(required = false) String uniqueId,
            @RequestParam(required = false) List<String> formats,
            @RequestParam(required = false) ImageLocation location
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadTime").descending());

        Specification<Image> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("uploadTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("uploadTime"), to));
            }
            if (includeUsers != null && !includeUsers.isEmpty()) {
                predicates.add(root.get("uploader").get("id").in(includeUsers));
            }
            if (excludeUsers != null && !excludeUsers.isEmpty()) {
                predicates.add(cb.not(root.get("uploader").get("id").in(excludeUsers)));
            }
            if (uniqueId != null && !uniqueId.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("uniqueId")), "%" + uniqueId.trim().toLowerCase() + "%"));
            }
            if (formats != null && !formats.isEmpty()) {
                predicates.add(root.get("fileType").in(formats));
            }
            if (location != null) {
                predicates.add(cb.equal(root.get("location"), location));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Image> images = imageRepository.findAll(spec, pageable);
        Page<ImageInfoDto> imageDtos = images.map(imageMapper);

        PagedResponse<ImageInfoDto> response = new PagedResponse<>(
                imageDtos.getContent(),
                imageDtos.getTotalElements(),
                imageDtos.getTotalPages(),
                imageDtos.getNumber(),
                imageDtos.getSize()
        );

        return ResponseEntity.ok(new DefaultResponse(false, response));
    }
}
