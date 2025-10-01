package me.xap3y.space.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.xap3y.space.api.enums.MetricRecordType;
import me.xap3y.space.api.wrapper.StatusCaptureResponseWrapper;
import me.xap3y.space.dto.LogDto;
import me.xap3y.space.service.LogsService;
import me.xap3y.space.service.PrometheusMetricService;
import me.xap3y.space.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;

@Component
public class RequestFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestFilter.class);

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


        if (userAgent == null) {
            userAgent = "curl";
        }

        log.info("[{}] Request from: {}, User-Agent: {}", httpRequest.getMethod(), remoteIp, userAgent);

        if (userAgent.contains("curl") || userAgent.contains("wget") || userAgent.contains("Custom-")) {
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"error\": \"You are in blacklist!\"}");
            return;
        }

        filterChain.doFilter(servletRequest, responseWrapper);

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        prometheusMetricService.recordEvent(MetricRecordType.REQUEST_MADE);

        if (userAgent.startsWith("HetrixTools") || userAgent.startsWith("Uptime-")) {
            return;
        }

        logsService.logFile(" --- [space] RequestFilter   : [" + method +"] REQ FROM: " + remoteIp + ", TO " + path);
        logsService.log(new LogDto(remoteIp, userAgent, path, method, "???"));

        int resultHttpCode = responseWrapper.getStatus();

        log.info("[{}] Request from: {}, to {} ({})", method, remoteIp, path, resultHttpCode);
        //webhookService.postMessage("Request from: " + remoteIp + ", to " + path + " (" + method + ")");
    }
}
