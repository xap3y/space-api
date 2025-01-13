package me.xap3y.space.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.util.ConfigDb;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class BasicController {

    private final ServerInfo serverInfo;

    public BasicController(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @GetMapping(
            value = "/v1/web/xap3y/render/{id}"
    ) public ResponseEntity<?> renderPage(
            @PathVariable String id
    ) {
        Map<String, Object> map = new HashMap<>();
        map.put("discord_id", "1234567890");
        map.put("discord", 23);
        Long discordId = (Long) map.get("discord_id");
        return new ResponseEntity<>(new DefaultResponse(true, "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(
            value = {"v1", "/", "status"},
            produces = "application/json"
    )
    public ResponseEntity<Map<String, Object>> rootRoute() {
        Map<String, Object> response = new HashMap<>() {{
            put("error", false);
            put("version", SpaceApplication.VERSION);
            put("level", SpaceApplication.env.toString());
            put("startedAt", SpaceApplication.startedAt);
            put("sitemap", "https://call.xap3y.tech/sitemap.xml");
            put("robots", "https://call.xap3y.tech/robots.txt");
            put("portal_url", "https://xap3y.space");
            // TODO: Get urls from enviroments variables
        }};
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(
            value = "/.env",
            produces = "application/json"
    )
    public ResponseEntity<Map<String, Object>> fakeEnv() {
        Map<String, Object> response = new HashMap<>() {{
            put("error", true);
            put("version", "v1");
            put("message", "False positive");
        }};
        return new ResponseEntity<>(response, HttpStatus.OK);
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

    @GetMapping("/{id}")
    public ResponseEntity<?> handleRoute(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        String host = request.getServerName();

        log.info(" HOST IS :: {}", host);
        HttpHeaders headers = new HttpHeaders();

        for (Map.Entry<String, String> entry : ConfigDb.getRedirectMapper().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(host)) {
                String finalUrl = entry.getValue()
                        .replaceAll("%BASE%", serverInfo.getBaseUrl()
                        .replaceAll("%PATH%", id));
                headers.setLocation(URI.create(finalUrl));
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
        }

        return null;
    }
}
