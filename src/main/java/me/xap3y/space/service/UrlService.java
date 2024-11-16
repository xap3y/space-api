package me.xap3y.space.service;

import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.entity.Url;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.UrlMapper;
import me.xap3y.space.repository.UrlRepository;
import me.xap3y.space.util.Utils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final UrlMapper urlMapper;

    public UrlService(UrlRepository urlRepository, UrlMapper urlMapper) {
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
    }

    public Optional<Url> getUrlByUniqueId(String id) {
        return urlRepository.findByShortCode(id);
    }

    public UrlDto createUrl(String url, User creator) {
        Url urlDto = new Url();
        urlDto.setOriginalUrl(url);
        urlDto.setCreatedAt(LocalDateTime.now());
        urlDto.setExpiresAt(LocalDateTime.now().plusDays(7));
        urlDto.setShortCode(Utils.generateRandomId());
        urlDto.setVisits(0);
        urlDto.setCreatedBy(creator);

        return urlMapper.apply(urlRepository.save(urlDto));

    }
}
