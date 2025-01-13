package me.xap3y.space.service;

import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.entity.Url;
import me.xap3y.space.entity.User;
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

    public UrlService(UrlRepository urlRepository, UrlMapper urlMapper) {
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
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

    public UrlDto createUrl(String url, User creator, int maxUses) {
        Url urlDto = new Url();
        urlDto.setOriginalUrl(url);
        urlDto.setCreatedAt(LocalDateTime.now());
        urlDto.setExpiresAt(LocalDateTime.now().plusDays(7));
        urlDto.setShortCode(Utils.generateRandomId());
        urlDto.setVisits(0);
        urlDto.setMaxUses(maxUses);
        urlDto.setCreatedBy(creator);

        return urlMapper.apply(urlRepository.save(urlDto));
    }

    public void deleteByShortCode(String shortCode) {
        urlRepository.deleteByShortCode(shortCode);
    }
}
