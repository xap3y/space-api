package me.xap3y.space.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class TempMailCreateRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String clientIp) {
        return buckets.computeIfAbsent(clientIp, ip ->
                Bucket.builder()
                        .addLimit(limit -> limit.capacity(2).refillGreedy(2, Duration.ofMinutes(1)))
                        .build()
        );
    }

    private String getClientIp(HttpServletRequest request) {
        /*String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }*/
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/v1/email/create/public");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        Bucket bucket = resolveBucket(clientIp);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long nanosToWait = probe.getNanosToWaitForRefill();
        long waitForRefill = (long) Math.ceil(nanosToWait / 1_000_000_000.0);

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(waitForRefill));
        response.setHeader("X-Rate-Limit-Limit", "2");
        response.setHeader("X-Rate-Limit-Remaining", "0");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Too Many Requests\",\"retryAfter\":" + waitForRefill + "}");
    }
}