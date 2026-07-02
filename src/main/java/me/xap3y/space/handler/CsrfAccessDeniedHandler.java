package me.xap3y.space.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;

public class CsrfAccessDeniedHandler implements AccessDeniedHandler {
    private static final Logger log = LoggerFactory.getLogger(CsrfAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        if (accessDeniedException instanceof CsrfException) {
            String clientIp = request.getRemoteAddr();
            log.error("CSRF Failure from IP: {} | Path: {} | Reason: {}",
                    clientIp, request.getRequestURI(), accessDeniedException.getMessage());
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
    }
}