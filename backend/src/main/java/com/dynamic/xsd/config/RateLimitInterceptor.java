package com.dynamic.xsd.config;

import com.dynamic.xsd.domain.entity.RateLimitBucket;
import com.dynamic.xsd.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for rate limiting API requests.
 *
 * Features:
 * - Token bucket algorithm
 * - Per-user rate limiting
 * - Configurable limits
 * - Rate limit headers (X-RateLimit-*)
 * - Graceful degradation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Skip rate limiting for health checks and actuator endpoints
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/actuator") || requestPath.equals("/health")) {
            return true;
        }

        // Get username from security context
        String username = getUsername();

        // Check rate limit
        RateLimitService.RateLimitStatus status = rateLimitService.checkStatus(
            username, RateLimitBucket.BucketType.USER);

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(status.getMaxTokens()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getTokensRemaining()));
        response.setHeader("X-RateLimit-Reset", status.getResetAt().toString());

        // Try to consume token
        boolean allowed = rateLimitService.allowRequest(username, RateLimitBucket.BucketType.USER, 1);

        if (!allowed) {
            log.warn("Rate limit exceeded for user: {} - path: {}", username, requestPath);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"retryAfter\":\"%s\"}",
                status.getResetAt().toString()
            ));

            return false;
        }

        return true;
    }

    /**
     * Gets username from security context, defaults to IP address.
     */
    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }

        return "anonymous";
    }
}
