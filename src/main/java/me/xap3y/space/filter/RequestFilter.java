package me.xap3y.space.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.wrapper.StatusCaptureResponseWrapper;
import me.xap3y.space.dto.LogDto;
import me.xap3y.space.service.LogsService;
import me.xap3y.space.service.PrometheusMetricService;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RequestFilter implements Filter {

    private final LogsService logsService;
    private final PrometheusMetricService prometheusMetricService;

    public RequestFilter(LogsService logsService, PrometheusMetricService prometheusMetricService) {
        this.logsService = logsService;
        this.prometheusMetricService = prometheusMetricService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        StatusCaptureResponseWrapper responseWrapper = new StatusCaptureResponseWrapper((HttpServletResponse) servletResponse);
        String remoteIp = httpRequest.getRemoteAddr();

        String userAgent = httpRequest.getHeader("User-Agent");
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            remoteIp = forwardedFor.split(",")[0];
        }

        prometheusMetricService.recordIpAccess(remoteIp, httpRequest.getRequestURI());

        if (userAgent == null) {
            userAgent = "curl";
        }

        String path = httpRequest.getRequestURI();

        String queryString = httpRequest.getQueryString();
        if (queryString != null) {
            path += "?" + queryString;
        }

        if (!path.startsWith("/actuator/prometheus")) {
            //log.info("[{}] Request from: {}, User-Agent: {}, PATH: {}", httpRequest.getMethod(), remoteIp, userAgent, path);
            // IGNORE
        } else {
            if (!path.contains("?Api-Key=")) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            String[] apiKeySplit = path.split("\\?Api-Key=");
            if (apiKeySplit.length < 2) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            String apiKey = apiKeySplit[1].split("&")[0];
            //log.info("[{}] Request from: {}, User-Agent: {}, PATH: {}, API Key: {}", httpRequest.getMethod(), remoteIp, userAgent, path, apiKey);
        }

        if (path.contains("/ws/playcore/in") || path.contains("/ws/playcore/out")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        } else if (path.contains("/get") && path.length() > 100) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (!path.startsWith("/actuator/prometheus")) {
            log.info(
                    "Req, cookie-size={} auth-type={}, user-agent={}",
                    httpRequest.getCookies() == null ? 0 : httpRequest.getCookies().length,
                    httpRequest.getAuthType(),
                    userAgent
            );
        }

        if ((userAgent.contains("curl") || userAgent.contains("wget") || userAgent.contains("Custom-")) && !path.contains("/actuator/prometheus")) {
            log.info("Blocking request from blacklisted user agent: {}", userAgent);
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"error\": \"You are in blacklist!\"}");
            return;
        }

        filterChain.doFilter(servletRequest, responseWrapper);

        String method = httpRequest.getMethod();

        if (!path.startsWith("/actuator/prometheus") && !path.equals("/favicon.ico")) {
            prometheusMetricService.recordEvent(MetricRecordType.REQUEST_MADE);
        }

        if (userAgent.startsWith("HetrixTools") || userAgent.startsWith("Uptime-")) {
            log.info("Ping from Uptime.");
            return;
        }

        int resultHttpCode = responseWrapper.getStatus();

        logsService.log(new LogDto(remoteIp, userAgent, path, method, resultHttpCode + ""));

        if (!path.startsWith("/actuator/prometheus") && !path.equals("/favicon.ico") && !path.equals("/status")) {
            log.info("Request result code: {}", resultHttpCode);

            logsService.logFile(" --- [space] RequestFilter   : [" + method +"] REQ FROM: " + remoteIp + ", TO " + path + " (" + resultHttpCode + ")");
        }
        //webhookService.postMessage("Request from: " + remoteIp + ", to " + path + " (" + method + ")");
    }
}
