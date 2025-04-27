package me.xap3y.space.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.dto.UrlLogDto;
import me.xap3y.space.entity.Url;
import me.xap3y.space.entity.UrlLogs;
import me.xap3y.space.mapper.UrlLogMapper;
import me.xap3y.space.repository.UrlLogsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class UrlLogsService {

    private final UrlLogsRepository urlLogsRepository;
    private final UrlLogMapper urlLogMapper;

    public UrlLogDto save(UrlLogs urlLogs) {
        UrlLogs savedUrlLog = urlLogsRepository.save(urlLogs);
        return urlLogMapper.apply(savedUrlLog);
    }

    public UrlLogDto getById(Long id) {
        return urlLogMapper.apply(urlLogsRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("UrlLogs not found for URL_id: " + id)
        ));
    }

    public List<UrlLogDto> getByUrl(Url url) {
        List<UrlLogs> urlLogs = urlLogsRepository.findAllByUrl(url).orElseThrow(
                () -> new ResourceNotFoundException("UrlLogs not found for URL_id: " + url.getId())
        );
        return urlLogs.stream().map(urlLogMapper).toList();
    }

    public List<UrlLogDto> getByUrlUniqueId(String uniqueId) {
        List<UrlLogs> urlLogs = urlLogsRepository.findByShortUrlUniqueId(uniqueId).orElseThrow(
                () -> new ResourceNotFoundException("UrlLogs not found for URL_id: " + uniqueId)
        );
        return urlLogs.stream().map(urlLogMapper).toList();
    }

    public List<UrlLogDto> getByUrlId(Long id) {
        List<UrlLogs> urlLogs = urlLogsRepository.findByShortUrlId(id).orElseThrow(
                () -> new ResourceNotFoundException("UrlLogs not found for URL_id: " + id)
        );
        return urlLogs.stream().map(urlLogMapper).toList();
    }

    public List<UrlLogDto> getByUrlIdAndIpAddress(Long id, String ipAddress) {
        List<UrlLogs> urlLogs = urlLogsRepository.findByShortUrlIdAndIpAddress(id, ipAddress).orElseThrow(
                () -> new ResourceNotFoundException("UrlLogs not found for URL_id: " + id + " and IP: " + ipAddress)
        );
        return urlLogs.stream().map(urlLogMapper).toList();
    }

}
