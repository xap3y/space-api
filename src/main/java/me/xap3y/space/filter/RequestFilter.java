package me.xap3y.space.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import me.xap3y.space.dto.LogDto;
import me.xap3y.space.service.LogsService;
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

    public RequestFilter(LogsService logsService) {
        this.logsService = logsService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String remoteIp = httpRequest.getRemoteAddr();

        String userAgent = httpRequest.getHeader("User-Agent");
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            remoteIp = forwardedFor.split(",")[0];
        }

        if (userAgent == null) {
            userAgent = "curl";
        }

        if (userAgent.contains("curl") || userAgent.contains("wget") || userAgent.contains("Custom-")) {
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"error\": \"You are in blacklist!\"}");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        if (userAgent.startsWith("HetrixTools")) {
            return;
        }

        logsService.logFile(" --- [space] [nio-8012-exec-3] me.xap3y.space.filter.RequestFilter   : [" + method +"] REQ FROM: " + remoteIp + ", TO " + path);
        logsService.log(new LogDto(remoteIp, userAgent, path, method, "???"));
        log.info("[{}] Request from: {}, to {} ({})", method, remoteIp, path, "???");
        //webhookService.postMessage("Request from: " + remoteIp + ", to " + path + " (" + method + ")");
    }
}
