package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.entity.Url;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ShortUrlMapper;
import me.xap3y.space.mapper.UrlMapper;
import me.xap3y.space.model.ShortUrlRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.repository.UrlRepository;
import me.xap3y.space.service.MetricService;
import me.xap3y.space.service.UrlService;
import me.xap3y.space.service.WebhookService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/v1/url")
public class UrlController {


    private final UrlService urlService;
    private final UrlMapper urlMapper;
    private final ServerInfo serverInfo;
    private final MetricService metricService;
    private final WebhookService webhookService;
    private final ShortUrlMapper shortUrlMapper;

    public UrlController(UrlService urlService, UrlMapper urlMapper, ServerInfo serverInfo, UrlRepository urlRepository, MetricService metricService, WebhookService webhookService, ShortUrlMapper shortUrlMapper) {
        this.urlService = urlService;
        this.urlMapper = urlMapper;
        this.serverInfo = serverInfo;
        this.metricService = metricService;
        this.webhookService = webhookService;
        this.shortUrlMapper = shortUrlMapper;
    }

    @PostMapping(
            value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresApiKey
    public ResponseEntity<?> createShortUrl(
            HttpServletRequest request,
            @RequestBody ShortUrlRequest body
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) {
            throw new InvalidApiKeyException();
        }

        if (body == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Missing request body"), HttpStatus.BAD_REQUEST);
        }

        String uniqueId = body.getUniqueId();
        String url = body.getUrl();

        if (url == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Please provide a URL"), HttpStatus.BAD_REQUEST);
        }

        if (uniqueId != null) {
            if (urlService.existByShortCode(uniqueId)) {
                return new ResponseEntity<>(new DefaultResponse(true, "ShortUrl with this UID already exists"), HttpStatus.BAD_REQUEST);
            }
        }

        if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        int maxUses = body.getMaxUses() == null ? -1 : body.getMaxUses();

        ShortUrlDto urlDto = urlService.createUrl(url, uploader, maxUses, uniqueId);
        metricService.setSessionUrlsShortened(metricService.getSessionUrlsShortened() + 1);
        metricService.setDatabaseUpdated(true);
        webhookService.postUrlShorten(urlDto);
        return new ResponseEntity<>(new UIDResponse(false, urlDto.uniqueId(), urlDto), HttpStatus.OK);
    }

    @GetMapping(
            value = "/r/{uniqueId}"
    )
    public ResponseEntity<?> redirectUrl(
            @PathVariable String uniqueId
    ) {
        UrlDto urlDto = urlService.getUrlByUniqueId(uniqueId)
                .map(urlMapper)
                .orElseThrow(() -> new ResourceNotFoundException("Url not found"));

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(urlDto.url()));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    @DeleteMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    public ResponseEntity<?> deleteUrl(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) throw new InvalidApiKeyException();

        Url urlDto = urlService.getUrlByUniqueId(uniqueId).orElseThrow(() -> new ResourceNotFoundException("Url not found"));

        if (!Objects.equals(urlDto.getCreatedBy().getId(), uploader.getId())  &&
                !(uploader.getRole() == UserRole.USER
                        || uploader.getRole() == UserRole.GUEST
                        || uploader.getRole() == UserRole.TESTER
                )) {
            throw new InvalidApiKeyException();
        }

        urlService.deleteByShortCode(urlDto.getShortCode());
        metricService.setDatabaseUpdated(true);
        //metricService.setSessionUrlsShortened(metricService.getSessionUrlsShortened() - 1);
        return new ResponseEntity<>(new DefaultResponse(false, "ShortUrl deleted"), HttpStatus.OK);
    }

    @GetMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    public ResponseEntity<?> getUrl(
            @PathVariable String uniqueId
            /*@RequestParam(required = false, defaultValue = "false", value = "raw") boolean rawData
            @RequestParam(required = false, defaultValue = "false", value = "uploader_info") boolean getUserInfo,
            @RequestParam(required = false, defaultValue = "false", value = "url_info") boolean urlInfo*/
    ) {
        ShortUrlDto urlDto = urlService.getUrlByUniqueId(uniqueId)
                .map(shortUrlMapper)
                .orElseThrow(() -> new ResourceNotFoundException("Url not found"));

        /*boolean isValid;

        isValid = LocalDateTime.now().isBefore(urlDto.expiresAt());
        if (isValid) isValid = urlDto.maxUses() < 0 || urlDto.maxUses() > urlDto.visits();

        boolean finalIsValid = isValid;
        Map<String, Object> map = new HashMap<>() {{
            put("original_url", urlDto.url());
            put("shorturl", serverInfo.getShortShortenerUrl() + "/" + urlDto.shortCode());
            put("redirecturl", serverInfo.getBaseUrl() + "/v1/url/r/" + urlDto.shortCode());
            put("created_at", urlDto.createdAt());
            put("expire_at", urlDto.expiresAt());
            put("creator", urlDto.uploader());
            put("max_uses", urlDto.maxUses());
            put("visits", urlDto.visits());
            put("valid", finalIsValid);
        }};*/

        return ResponseEntity.ok()
                .body(new UIDResponse(false, urlDto.uniqueId(), urlDto));
    }


}
