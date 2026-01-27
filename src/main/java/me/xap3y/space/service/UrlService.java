package me.xap3y.space.service;

import me.xap3y.space.api.exception.InvalidUniqueIdException;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.entity.Url;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ShortUrlMapper;
import me.xap3y.space.mapper.UrlMapper;
import me.xap3y.space.repository.UrlRepository;
import me.xap3y.space.util.Utils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final UrlMapper urlMapper;
    private final ShortUrlMapper shortUrlMapper;

    public UrlService(UrlRepository urlRepository, UrlMapper urlMapper, ShortUrlMapper shortUrlMapper) {
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
        this.shortUrlMapper = shortUrlMapper;
    }

    public Optional<Url> getUrlByUniqueId(String id) {
        return urlRepository.findByShortCode(id);
    }

    public List<UrlDto> getAllUrlsByCreatorId(Long id) {
        return urlRepository.findByCreatedById(id).stream()
                .map(urlMapper)
                .collect(Collectors.toList());
    }

    public List<ShortUrlDto> getAllShortUrlsByCreatorId(Long id) {
        return urlRepository.findByCreatedById(id).stream()
                .map(shortUrlMapper)
                .collect(Collectors.toList());
    }

    public List<ShortUrlDto> getAllShortUrlsByCreatorId(Long id, boolean logs) {
        return urlRepository.findByCreatedById(id).stream()
                .map((url) -> shortUrlMapper.applyWithLogs(url, logs))
                .collect(Collectors.toList());
    }

    public int countUrlsByUserId(Long uid) {
        return urlRepository.countAllByCreatedById(uid);
    }

    public ShortUrlDto createUrl(String url, User creator, int maxUses, String uniqueId) {

        if (uniqueId == null) {
            uniqueId = Utils.generateRandomId();
        } else if (!uniqueId.matches("^[a-zA-Z0-9]*$")) {
            throw new InvalidUniqueIdException();
        }

        if (urlRepository.existsByShortCode(uniqueId)) {
            throw new InvalidUniqueIdException();
        }

        Url urlDto = new Url();
        urlDto.setOriginalUrl(URLEncoder.encode(url, StandardCharsets.UTF_8));
        urlDto.setCreatedAt(LocalDateTime.now());
        urlDto.setExpiresAt(LocalDateTime.now().plusDays(7));
        urlDto.setShortCode(uniqueId);
        urlDto.setVisits(0);
        urlDto.setMaxUses(maxUses);
        urlDto.setCreatedBy(creator);

        return shortUrlMapper.apply(urlRepository.save(urlDto));
    }

    public void deleteByShortCode(String shortCode) {
        urlRepository.deleteByShortCode(shortCode);
    }

    public boolean existByShortCode(String shortCode) {
        return urlRepository.existsByShortCode(shortCode);
    }

    public List<Pair<LocalDate, Long>> findTotalUrlsPerDayByUser(LocalDateTime startDate, LocalDateTime endDate, Long uploaderId, boolean fillMissingDates) {
        List<Object[]> results = urlRepository.findTotalUrlsPerDayByUser(startDate.with(LocalTime.MIN), endDate.with(LocalTime.MAX), uploaderId);
        return Utils.convertToPairList(startDate, endDate, results, fillMissingDates);
    }

    public long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return urlRepository.countByCreatedAtBetween(startDate, endDate);
    }

    public Optional<Pair<Long, Long>> findBiggestCreatorInRangeWithId(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> result = urlRepository.findBiggestCreatorInRangeWithId(startDate, endDate).orElse(null);
        return Utils.parseBestUploader(result);
    }

    public boolean existByUniqueId(String uniqueId) {
        return urlRepository.existsByShortCode(uniqueId);
    }
}
