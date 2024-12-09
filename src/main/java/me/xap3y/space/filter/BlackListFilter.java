package me.xap3y.space.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Objects;

@WebFilter(urlPatterns = "/*")
public class BlackListFilter implements Filter {

    private final String[] allowedIps = {"169.254.169.126", "127.0.0.1", "localhost"};

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String remoteIp = httpRequest.getRemoteAddr();

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            remoteIp = forwardedFor.split(",")[0];
        }

        for (String i : allowedIps) {
            if (Objects.equals(remoteIp, i)) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        String userAgent = httpRequest.getHeader("User-Agent");

        if (userAgent == null) {
            userAgent = "curl";
        }

        if (userAgent.contains("curl") || userAgent.contains("wget") || userAgent.contains("Custom-")) {
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"error\": \"You are in blacklist!\"}");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
