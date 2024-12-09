package me.xap3y.space.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.entity.User;
import me.xap3y.space.api.exception.ResourceNotFoundException;
import me.xap3y.space.mapper.UrlMapper;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.model.response.UIDResponse;
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


    private final UrlService urlService;
    private final UrlMapper urlMapper;

    public UrlController(UrlService urlService, UrlMapper urlMapper) {
        this.urlService = urlService;
        this.urlMapper = urlMapper;
    }

    @PostMapping(
            value = "/create",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RequiresApiKey
    public ResponseEntity<?> createPaste(
            HttpServletRequest request,
            @RequestParam(value = "url") String url
    ) {
        User uploader = (User) request.getAttribute("uploader");
        if (uploader == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }
        if (url == null) {
            return new ResponseEntity<>(new DefaultResponse(true, "Please provide a URL"), HttpStatus.BAD_REQUEST);
        }

        if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        UrlDto urlDto = urlService.createUrl(url, uploader);
        return new ResponseEntity<>(new UIDResponse(false, urlDto.shortCode(), urlDto), HttpStatus.OK);
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
                    .body(new UIDResponse(false, urlDto.shortCode(), urlDto.url()));
        }
    }


}
