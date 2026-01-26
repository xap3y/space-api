package me.xap3y.space.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.enums.UserAccountStatus;
import me.xap3y.space.api.enums.UserRole;
import me.xap3y.space.api.exception.InvalidApiKeyException;
import me.xap3y.space.api.iface.OptionalApiKey;
import me.xap3y.space.api.iface.OptionalCookieAuth;
import me.xap3y.space.api.iface.RequiresApiKey;
import me.xap3y.space.api.iface.RequiresSpecialApiKey;
import me.xap3y.space.entity.Session;
import me.xap3y.space.entity.User;
import me.xap3y.space.model.response.DefaultResponse;
import me.xap3y.space.service.ApiKeyService;
import me.xap3y.space.service.LogsService;
import me.xap3y.space.service.PrometheusMetricService;
import me.xap3y.space.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    private final String API_KEY_HEADER_NAME = "X-API-Key";
    private final String API_KEY_FORM_NAME = "key";
    private final LogsService logsService;
    private final PrometheusMetricService prometheusMetricService;
    private final SessionService sessionService;


    public ApiKeyInterceptor(ApiKeyService apiKeyService, ObjectMapper objectMapper, LogsService logsService, PrometheusMetricService prometheusMetricService, SessionService sessionService) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
        this.logsService = logsService;
        this.prometheusMetricService = prometheusMetricService;
        this.sessionService = sessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod method) {
            RequiresApiKey annotation = method.getMethodAnnotation(RequiresApiKey.class);
            OptionalApiKey optionalApiKeyAnnotation = method.getMethodAnnotation(OptionalApiKey.class);
            OptionalCookieAuth optionalCookieAuthAnn = method.getMethodAnnotation(OptionalCookieAuth.class);
            logsService.logFile(" --- - RequiresApiKey (RequiresApiKey.class) == " + annotation);
            RequiresSpecialApiKey specialKeyAnnotation = method.getMethodAnnotation(RequiresSpecialApiKey.class);
            logsService.logFile(" --- - RequiresSpecialApiKey (RequiresSpecialApiKey.class) == " + annotation);
            if (annotation != null || specialKeyAnnotation != null || optionalApiKeyAnnotation != null || optionalCookieAuthAnn != null) {
                String apiKey = request.getHeader(this.API_KEY_HEADER_NAME);
                if (apiKey == null && request.getContentType() != null && request.getContentType().contains("application/x-www-form-urlencoded")) {
                    apiKey = getApiKeyFromBody(request);
                }

                User uploader;

                if (apiKey == null) {
                    String sessionToken = null;
                    if (request.getCookies() != null) {
                        for (Cookie cookie : request.getCookies()) {
                            if ("session_token".equals(cookie.getName())) {
                                sessionToken = cookie.getValue();
                                break;
                            }
                        }
                    }

                    if (sessionToken != null) {
                        Session ses = sessionService.getValidSession(sessionToken);
                        if (ses == null) {
                            this.writeErrorResponse(response, new DefaultResponse(true, "Invalid session token"), HttpStatus.FORBIDDEN);
                            return false;
                        }
                        uploader = ses.getUser();
                        logsService.logFile(" --- - SESSION_TOKEN User == " + uploader.getUsername());
                        request.setAttribute("uploader", uploader);
                        return true;
                    }

                    if (optionalApiKeyAnnotation != null || optionalCookieAuthAnn != null) return true; // TODO
                    this.writeErrorResponse(response, new DefaultResponse(true, "API Key is required"), HttpStatus.BAD_REQUEST);
                    return false;
                }

                try {
                    uploader = apiKeyService.validateApiKey(apiKey);
                    logsService.logFile(" --- - API_KEY User == " + uploader.getUsername());
                    prometheusMetricService.recordEvent(MetricRecordType.VALID_API_KEY_REQUEST_MADE);
                } catch (InvalidApiKeyException e) {
                    if (optionalApiKeyAnnotation != null) return true;
                    log.info("Invalid API Key used: {}", apiKey);
                    prometheusMetricService.recordEvent(MetricRecordType.INVALID_API_KEY_REQUEST_MADE);
                    this.writeErrorResponse(response, new DefaultResponse(true, e.getMessage()), HttpStatus.UNAUTHORIZED);
                    return false;
                }

                String route = request.getRequestURI();
                log.info("API Key used by user: {} on route: {}", uploader.getUsername(), route);

                if (uploader.getStatus() != UserAccountStatus.ACTIVE && !Objects.equals(route, "/v1/auth/verify/email")) {
                    if (optionalApiKeyAnnotation != null) return true;
                    this.writeErrorResponse(response, new DefaultResponse(true, "Your account is not active! Please contact support."), HttpStatus.FORBIDDEN);
                    return false;
                }

                if (uploader.getRole() == UserRole.BANNED || uploader.getRole() == UserRole.DELETED) {
                    if (optionalApiKeyAnnotation != null) return true;
                    this.writeErrorResponse(response, new DefaultResponse(true, "Your account is not able to create any new posts! Contact support."), HttpStatus.FORBIDDEN);
                    return false;
                }

                if (specialKeyAnnotation != null && (!uploader.getRole().equals(UserRole.ADMIN) && !uploader.getRole().equals(UserRole.OWNER))) {
                    prometheusMetricService.recordEvent(MetricRecordType.INVALID_SPECIAL_API_KEY_REQUEST_MADE);
                    this.writeErrorResponse(response, new DefaultResponse(true, "You are not allowed to access this resource (KEY)"), HttpStatus.FORBIDDEN);
                    return false;
                } else {
                    log.info("Special API Key used on {} by {}", route, uploader.getId());
                    prometheusMetricService.recordEvent(MetricRecordType.VALID_SPECIAL_API_KEY_REQUEST_MADE);
                }

                request.setAttribute("uploader", uploader);
            }
        }
        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, Object errorObject, HttpStatus status) {
        response.setStatus(status.value());
        response.setContentType("application/json");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(errorObject));
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private String getApiKeyFromBody(HttpServletRequest request) throws IOException {
        String body = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
        Map<String, String> params = parseUrlEncodedBody(body);
        return params.get(this.API_KEY_FORM_NAME);
    }

    private Map<String, String> parseUrlEncodedBody(String body) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = body.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(decodeUrl(keyValue[0]), decodeUrl(keyValue[1]));
            }
        }
        return params;
    }

    private String decodeUrl(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (NullPointerException | IllegalArgumentException e) {
            return value;
        }
    }
}