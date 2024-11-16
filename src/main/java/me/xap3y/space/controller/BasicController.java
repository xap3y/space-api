package me.xap3y.space.controller;


import me.xap3y.space.SpaceApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class BasicController {

    @GetMapping(
            value = {"v1", "/", "status"},
            produces = "application/json"
    )
    public ResponseEntity<Map<String, Object>> rootRoute() {
        Map<String, Object> response = new HashMap<>() {{
            put("error", false);
            put("version", "v1");
            put("version_number", SpaceApplication.VERSION);
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
}
