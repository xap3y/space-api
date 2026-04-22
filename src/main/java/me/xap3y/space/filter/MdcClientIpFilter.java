package me.xap3y.space.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MdcClientIpFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String remoteIp = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            remoteIp = forwardedFor.split(",")[0];
        }

        MDC.put("clientIp", remoteIp);
        MDC.put("uri", request.getRequestURI());
        MDC.put("method", request.getMethod());

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("clientIp");
            MDC.remove("uri");
            MDC.remove("method");
        }
    }
}