package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.enums.ResourceLimitType;
import me.xap3y.space.api.exception.BadRequestException;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.api.iface.PathLengthValidator;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.ShortUrlDto;
import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.dto.UrlLogDto;
import me.xap3y.space.entity.Url;
import me.xap3y.space.entity.UrlLogs;
import me.xap3y.space.entity.User;
import me.xap3y.space.mapper.ShortUrlMapper;
import me.xap3y.space.mapper.UrlMapper;
import me.xap3y.space.model.request.ShortUrlRequest;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
import me.xap3y.space.repository.UrlRepository;
import me.xap3y.space.service.MetricService;
import me.xap3y.space.service.ResourceLimitService;
import me.xap3y.space.service.UrlLogsService;
import me.xap3y.space.service.UrlService;
import me.xap3y.space.service.WebhookService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
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
    private final UrlLogsService urlLogsService;
    private final ResourceLimitService resourceLimitService;

    public UrlController(UrlService urlService, UrlMapper urlMapper, ServerInfo serverInfo, UrlRepository urlRepository, MetricService metricService, WebhookService webhookService, ShortUrlMapper shortUrlMapper, UrlLogsService urlLogsService, ResourceLimitService resourceLimitService) {
        this.urlService = urlService;
        this.urlMapper = urlMapper;
        this.serverInfo = serverInfo;
        this.metricService = metricService;
        this.webhookService = webhookService;
        this.shortUrlMapper = shortUrlMapper;
        this.urlLogsService = urlLogsService;
        this.resourceLimitService = resourceLimitService;
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

        if (body == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Missing request body"), HttpStatus.BAD_REQUEST);
        } else if (body.getUniqueId() != null && !uploader.isAdmin()) {
            throw new BadRequestException("No permission to use custom UID");
        } else if (body.getUniqueId() != null && uploader.isAdmin() && urlService.existByUniqueId(body.getUniqueId())) {
            throw new BadRequestException("This UID already exists!");
        }

        String uniqueId = body.getUniqueId();
        String url = body.getUrl();

        if (url == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Please provide a URL"), HttpStatus.BAD_REQUEST);
        }

        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        resourceLimitService.assertCanCreate(uploader, ResourceLimitType.URL, 1, 0);

        int maxUses = body.getMaxUses() == null ? -1 : body.getMaxUses();

        ShortUrlDto urlDto = urlService.createUrl(url, uploader, maxUses, uniqueId);
        resourceLimitService.recordCreation(uploader, ResourceLimitType.URL, 1, 0);
        metricService.setSessionUrlsShortened(metricService.getSessionUrlsShortened() + 1);
        metricService.setDatabaseUpdated(true);
        webhookService.postUrlShorten(urlDto);
        return new ResponseEntity<>(new UIDResponse(false, urlDto.uniqueId(), urlDto), HttpStatus.OK);
    }

    @GetMapping(
            value = "/r/{uniqueId}"
    )
    @PathLengthValidator
    public ResponseEntity<?> redirectUrl(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) {
        Url url = urlService.getUrlByUniqueId(uniqueId).orElseThrow(() -> new ResourceNotFoundException("Url not found"));
        UrlDto urlDto = urlMapper.apply(url);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(URLDecoder.decode(urlDto.url(), StandardCharsets.UTF_8)));


        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        UrlLogs urlLogs = new UrlLogs();
        urlLogs.setUrl(url);
        urlLogs.setUserAgent(userAgent);
        urlLogs.setIpAddress(ipAddress);
        urlLogs.setTime(LocalDateTime.now());
        urlLogsService.save(urlLogs);
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
    @PathLengthValidator
    public ResponseEntity<?> deleteUrl(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) {
        User uploader = (User) request.getAttribute("uploader");
        resourceLimitService.assertMutationAllowed(uploader);

        Url urlDto = urlService.getUrlByUniqueId(uniqueId).orElseThrow(() -> new ResourceNotFoundException("Url not found"));

        if (!Objects.equals(urlDto.getCreatedBy().getId(), uploader.getId())  &&
                (uploader.getRole() == UserRole.USER
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
    @PathLengthValidator
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

    @GetMapping(
            value = "/get/{uniqueId}/logs",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    @RequiresApiKey
    @PathLengthValidator
    public ResponseEntity<?> getUrlLogs(
            HttpServletRequest request,
            @PathVariable String uniqueId
    ) {
        User uploader = (User) request.getAttribute("uploader");

        ShortUrlDto urlDto = urlService.getUrlByUniqueId(uniqueId)
                .map(shortUrlMapper)
                .orElseThrow(() -> new ResourceNotFoundException("Url not found"));

        if (urlDto.uploader().uid() != uploader.getId() && (uploader.getRole() != UserRole.ADMIN && uploader.getRole() != UserRole.OWNER)) {
            throw new InvalidApiKeyException();
        }

        List<UrlLogDto> logs = urlLogsService.getByUrlUniqueId(uniqueId);

        if (logs.isEmpty()) {
            return new ResponseEntity<>(new DefaultResponse(true, "No logs found"), HttpStatus.NOT_FOUND);
        }


        return ResponseEntity.ok()
                .body(new UIDResponse(false, uniqueId, logs));
    }


}
