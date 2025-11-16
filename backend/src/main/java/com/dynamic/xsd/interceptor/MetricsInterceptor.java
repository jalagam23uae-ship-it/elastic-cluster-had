package com.dynamic.xsd.interceptor;

import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.domain.entity.ServiceMetric;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import com.dynamic.xsd.service.MetricsCollectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor for collecting metrics on dynamic service endpoints.
 *
 * Captures:
 * - Request count
 * - Response time
 * - Error rate
 * - Endpoint path and method
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "startTime";
    private static final String SERVICE_ID_ATTR = "serviceId";
    private static final String ENDPOINT_ATTR = "endpoint";

    // Pattern to match dynamic service endpoints: /api/v1/services/{serviceName}/**
    private static final Pattern SERVICE_PATH_PATTERN =
        Pattern.compile("/api/v1/services/([^/]+)(/.*)?");

    // Pattern to match SOAP endpoints: /ws/{serviceName}
    private static final Pattern SOAP_PATH_PATTERN =
        Pattern.compile("/ws/([^/]+)");

    private final ServiceDefinitionRepository serviceDefinitionRepository;
    private final MetricsCollectionService metricsCollectionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {

        // Record start time
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        // Extract service name from path
        String path = request.getRequestURI();
        String serviceName = extractServiceName(path);

        if (serviceName != null) {
            // Find service definition
            Optional<ServiceDefinition> serviceOpt =
                serviceDefinitionRepository.findByServiceName(serviceName);

            if (serviceOpt.isPresent()) {
                ServiceDefinition service = serviceOpt.get();
                request.setAttribute(SERVICE_ID_ATTR, service.getId());
                request.setAttribute(ENDPOINT_ATTR, path);

                log.debug("Tracking metrics for service: {} on endpoint: {}",
                    serviceName, path);

                // Increment request count
                service.setTotalRequests((service.getTotalRequests() != null ?
                    service.getTotalRequests() : 0L) + 1L);
                serviceDefinitionRepository.save(service);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) throws Exception {

        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        String serviceId = (String) request.getAttribute(SERVICE_ID_ATTR);
        String endpoint = (String) request.getAttribute(ENDPOINT_ATTR);

        if (startTime != null && serviceId != null) {
            long responseTime = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatus();
            boolean isError = statusCode >= 400 || ex != null;

            // Update service statistics
            Optional<ServiceDefinition> serviceOpt =
                serviceDefinitionRepository.findById(serviceId);

            if (serviceOpt.isPresent()) {
                ServiceDefinition service = serviceOpt.get();

                // Update error count
                if (isError) {
                    service.setFailedRequests((service.getFailedRequests() != null ?
                        service.getFailedRequests() : 0L) + 1L);
                } else {
                    service.setSuccessfulRequests((service.getSuccessfulRequests() != null ?
                        service.getSuccessfulRequests() : 0L) + 1L);
                }

                // Update average response time
                Long totalRequests = service.getTotalRequests();
                Double currentAvg = service.getAverageResponseTime();

                if (totalRequests != null && totalRequests > 0) {
                    if (currentAvg == null) {
                        currentAvg = 0.0;
                    }
                    // Calculate running average
                    double newAvg = ((currentAvg * (totalRequests - 1)) + (double) responseTime) / totalRequests;
                    service.setAverageResponseTime(newAvg);
                }

                serviceDefinitionRepository.save(service);

                // Record detailed metrics
                recordMetrics(serviceId, endpoint, responseTime, isError,
                    request.getMethod(), statusCode);

                log.debug("Recorded metrics for service: {} - response time: {}ms, status: {}",
                    serviceId, responseTime, statusCode);
            }
        }
    }

    /**
     * Records detailed metrics asynchronously.
     */
    private void recordMetrics(String serviceId, String endpoint, long responseTime,
                              boolean isError, String httpMethod, int statusCode) {

        try {
            // Create tags for additional context
            Map<String, Object> tags = new HashMap<>();
            tags.put("endpoint", endpoint);
            tags.put("httpMethod", httpMethod);
            tags.put("statusCode", statusCode);
            tags.put("isError", isError);

            // Record response time metric
            metricsCollectionService.recordMetric(
                serviceId,
                ServiceMetric.MetricType.RESPONSE_TIME,
                responseTime,
                tags
            );

            // Record request count metric
            metricsCollectionService.recordMetric(
                serviceId,
                ServiceMetric.MetricType.REQUEST_COUNT,
                1.0,
                tags
            );

            // Record error rate if applicable
            if (isError) {
                metricsCollectionService.recordMetric(
                    serviceId,
                    ServiceMetric.MetricType.ERROR_RATE,
                    100.0,  // 100% error for this request
                    tags
                );
            }

        } catch (Exception e) {
            log.error("Failed to record metrics for service: {}", serviceId, e);
        }
    }

    /**
     * Extracts service name from request path.
     */
    private String extractServiceName(String path) {
        // Try REST endpoint pattern first: /api/v1/services/{serviceName}/**
        Matcher restMatcher = SERVICE_PATH_PATTERN.matcher(path);
        if (restMatcher.matches()) {
            return restMatcher.group(1);
        }

        // Try SOAP endpoint pattern: /ws/{serviceName}
        Matcher soapMatcher = SOAP_PATH_PATTERN.matcher(path);
        if (soapMatcher.matches()) {
            return soapMatcher.group(1);
        }

        return null;
    }
}
