package me.xap3y.space.controller;

import me.xap3y.space.dto.JsonResponse;
import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.entity.User;
import me.xap3y.space.exception.ResourceNotFoundException;
import me.xap3y.space.mapper.UrlMapper;
import me.xap3y.space.service.ApiKeyService;
import me.xap3y.space.service.UrlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/url")
public class UrlController {


    private final ApiKeyService apiKeyService;
    private final UrlService urlService;
    private final UrlMapper urlMapper;

    public UrlController(UrlService urlService, ApiKeyService apiKeyService, UrlMapper urlMapper) {
        this.urlService = urlService;
        this.apiKeyService = apiKeyService;
        this.urlMapper = urlMapper;
    }

    @PostMapping(
            value = "/create",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonResponse> createPaste(
            @RequestParam(value = "url") String url,
            @RequestHeader("X-API-Key") String apiKey
    ) {
        if (url == null) {
            return new ResponseEntity<>(new JsonResponse(true, "Please provide a URL"), HttpStatus.BAD_REQUEST);
        }

        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        User creator;
        try {
            creator = apiKeyService.validateApiKey(apiKey);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid API Key")) {
                return new ResponseEntity<>(new JsonResponse(true, "Invalid API Key!"), HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        UrlDto urlDto = urlService.createUrl(url, creator);
        return new ResponseEntity<>(new JsonResponse(false, urlDto), HttpStatus.OK);
    }

    @GetMapping(
            value = "/get/{uniqueId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_PLAIN_VALUE
            }
    )
    public ResponseEntity<?> getUrl(
            @PathVariable String uniqueId,
            @RequestParam(required = false, defaultValue = "false", value = "raw") boolean rawData,
            @RequestParam(required = false, defaultValue = "false", value = "uploader_info") boolean getUserInfo,
            @RequestParam(required = false, defaultValue = "false", value = "url_info") boolean urlInfo
    ) {
        UrlDto urlDto = urlService.getUrlByUniqueId(uniqueId)
                .map(urlMapper)
                .orElseThrow(() -> new ResourceNotFoundException("Url not found"));

        HttpHeaders headers = new HttpHeaders();

        if (getUserInfo) {
            headers.add("X-Uploader", urlDto.uploader());
        }

        if (urlInfo) {
            headers.add("X-Url-CreatedAt", urlDto.createdAt().toString());
            headers.add("X-Url-ExpiresAt", urlDto.expiresAt().toString());
            headers.add("X-Url-Visits",  String.valueOf(urlDto.visits()));
        }

        if (rawData) {
            headers.set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(urlDto.url());
        } else {
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new JsonResponse(false, urlDto.shortCode(), urlDto.url()));
        }
    }


}
