package com.dynamic.xsd.config;

import com.dynamic.xsd.interceptor.MetricsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 *
 * Features:
 * - CORS configuration
 * - Rate limiting interceptor
 * - Metrics interceptor
 * - Custom interceptors
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final MetricsInterceptor metricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add rate limiting to all API endpoints
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/v1/auth/login", "/api/v1/auth/register"); // Allow auth endpoints

        // Add metrics collection for dynamic service endpoints
        registry.addInterceptor(metricsInterceptor)
            .addPathPatterns(
                "/api/v1/services/**",  // REST endpoints
                "/ws/**"                 // SOAP endpoints
            )
            .excludePathPatterns(
                "/api/v1/services/deploy",      // Exclude management endpoints
                "/api/v1/services/*/undeploy",
                "/api/v1/services/*/status"
            );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:5173") // Vite dev server
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);

        registry.addMapping("/ws/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:5173")
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
