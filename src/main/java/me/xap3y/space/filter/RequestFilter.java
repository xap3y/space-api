package me.xap3y.space.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import me.xap3y.space.dto.LogDto;
import me.xap3y.space.service.LogsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@WebFilter(urlPatterns = "/*")
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

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            remoteIp = forwardedFor.split(",")[0];
        }

        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(httpResponse);

        filterChain.doFilter(servletRequest, servletResponse);

        String path = httpRequest.getRequestURI();

        int response = responseWrapper.getStatus();
        String userAgent = httpRequest.getHeader("User-Agent");
        String method = httpRequest.getMethod();

        logsService.log(new LogDto(remoteIp, userAgent, path, method, String.valueOf(response)));
        log.info("[{}] Request from: {}, to {} ({})", method, remoteIp, path, response);
    }
}
