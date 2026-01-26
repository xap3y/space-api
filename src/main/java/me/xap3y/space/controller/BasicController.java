package me.xap3y.space.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.MetricService;
import me.xap3y.space.util.ConfigDb;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class BasicController {

    private final ServerInfo serverInfo;
    private final MetricService metricService;
    private final ResourceLoader resourceLoader;

    public BasicController(ServerInfo serverInfo, MetricService metricService, ResourceLoader resourceLoader) {
        this.serverInfo = serverInfo;
        this.metricService = metricService;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping(
            value = "/v1/web/xap3y/render/{id}"
    ) public ResponseEntity<?> renderPage(
            @PathVariable String id
    ) {
        return new ResponseEntity<>(new DefaultResponse(true, "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(
            value = {"v1", "/", "status"},
            produces = "application/json"
    )
    public ResponseEntity<Map<String, ?>> rootRoute() {
        Map<String, Object> response = new HashMap<>() {{
            put("error", false);
            put("version", SpaceApplication.VERSION);
            put("level", SpaceApplication.env.toString());
            put("startedAt", SpaceApplication.startedAt);
            put("portal_url", serverInfo.getFrontEndUrl());
            put("namespace_tag", serverInfo.getNamespaceName());
            // TODO: Get urls from enviroments variables
        }};
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(
            value = "/.env",
            produces = "application/json"
    )
    public ResponseEntity<Map<String, Object>> fakeEnv() {
        return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
    }

    // Test for CSRF vulnerability
    @PostMapping(
            value = "/test-csrf",
            produces = "application/json"
    ) public ResponseEntity<Map<String, Object>> testCsrf() {
        Map<String, Object> response = new HashMap<>() {{
            put("error", false);
            put("message", "CSRF is not vulnerable");
        }};
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(
            value = "/test-csrf",
            produces = "application/json"
    ) public ResponseEntity<Map<String, Object>> testCsrfGet() {
        Map<String, Object> response = new HashMap<>() {{
            put("error", false);
            put("message", "CSRF is not vulnerable");
        }};
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/r/{id}")
    public ResponseEntity<?> handleRoute(
            @PathVariable String id,
            HttpServletRequest request
    ) {

        // If the resource exists, return it
        /*try {
            Resource resource = resourceLoader.getResource("classpath:static/" + id);

            if (resource.exists()) {
                Path path = Paths.get(resource.getURI());
                String contentType = Files.probeContentType(path);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error handling resource: " + e.getMessage());
        }*/

        // Otherwise, check if the host matches a redirect
        String host = request.getServerName();

        log.info(" HOST IS :: {}", host);
        log.info(" PATH-VARIABLE IS :: {}", id);
        HttpHeaders headers = new HttpHeaders();

        // Migrated to cloudflare redirect rules
        for (Map.Entry<String, String> entry : ConfigDb.getRedirectMapper().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(host)) {
                log.info(" FOUND MATCHER :: {}", entry.getKey());
                log.info(" RAW0 REDIRECTING TO :: {}", entry.getValue());
                String finalUrl = entry.getValue().replaceAll("%BASE%", serverInfo.getBaseUrl());
                log.info(" RAW1 REDIRECTING TO :: {}", finalUrl);
                headers.setLocation(URI.create(finalUrl.replaceAll("%PATH%", id)));
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
        }

        log.info(" MATCHER NOT FOUND");
        return null;
    }

    @GetMapping(
            value = "/error",
            produces = "application/json"
    )
    public ResponseEntity<?> error() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(
            value = "/metrics",
            produces = "application/json"
    )
    public ResponseEntity<?> metrics() {
        Map<String, Object> response = new HashMap<>() {{

            // INFO

            put("error", false);
            put("version", SpaceApplication.VERSION);
            put("level", SpaceApplication.env.toString());
            put("startedAt", SpaceApplication.startedAt);
            put("robots", serverInfo.getBaseUrl() + "/robots.txt");
            put("portal_url", serverInfo.getFrontEndUrl());

            // METRICS

            put("session_images", metricService.getSessionImagesUploaded());
            put("session_pastes", metricService.getSessionPastesCreated());
            put("session_urls", metricService.getSessionUrlsShortened());

            put("total_images", metricService.getTotalImagesUploaded());
            put("total_pastes", metricService.getTotalPastesCreated());
            put("total_urls", metricService.getTotalUrlsShortened());

            put("today_images", metricService.getTodayImagesUploaded());
            put("today_pastes", metricService.getTotalPastesCreated());
            put("today_urls", metricService.getTodayUrlsShortened());

        }};
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
