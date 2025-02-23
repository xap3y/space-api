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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    public int countUrlsByUserId(Long uid) {
        return urlRepository.countAllByCreatedById(uid);
    }

    public ShortUrlDto createUrl(String url, User creator, int maxUses, String uniqueId) {

        if (uniqueId == null) {
            uniqueId = Utils.generateRandomId();
        } else if (!uniqueId.matches("^[a-zA-Z0-9]*$")) {
            throw new InvalidUniqueIdException();
        }

        Url urlDto = new Url();
        urlDto.setOriginalUrl(url);
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
}
