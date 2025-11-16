package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.ServiceMetric;
import com.dynamic.xsd.repository.ServiceMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for collecting and analyzing time-series metrics.
 *
 * Features:
 * - Async metric recording (non-blocking)
 * - Aggregation windows (1m, 5m, 15m, 1h, 1d)
 * - Statistical analysis (avg, min, max, percentiles)
 * - Tag-based filtering
 * - Retention management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectionService {

    private final ServiceMetricRepository metricRepository;

    /**
     * Records a metric asynchronously.
     *
     * @param serviceId Service definition ID
     * @param metricType Type of metric
     * @param value Metric value
     */
    @Async
    @Transactional
    public void recordMetricAsync(String serviceId, ServiceMetric.MetricType metricType, double value) {
        recordMetric(serviceId, metricType, value, null, null);
    }

    /**
     * Records a metric with tags.
     *
     * @param serviceId Service definition ID
     * @param metricType Type of metric
     * @param value Metric value
     * @param tags Additional metadata
     */
    @Transactional
    public void recordMetric(String serviceId, ServiceMetric.MetricType metricType, double value,
                            Map<String, Object> tags) {
        recordMetric(serviceId, metricType, value, tags, null);
    }

    /**
     * Records a metric with complete information.
     *
     * @param serviceId Service definition ID
     * @param metricType Type of metric
     * @param value Metric value
     * @param tags Additional metadata
     * @param aggregationWindow Aggregation window (1m, 5m, etc.)
     */
    @Transactional
    public void recordMetric(String serviceId, ServiceMetric.MetricType metricType, double value,
                            Map<String, Object> tags, String aggregationWindow) {
        try {
            ServiceMetric metric = ServiceMetric.builder()
                .serviceDefinitionId(serviceId)
                .metricType(metricType)
                .metricValue(value)
                .tags(tags)
                .aggregationWindow(aggregationWindow)
                .build();

            metricRepository.save(metric);

            log.debug("Recorded metric: {} = {} for service {}", metricType, value, serviceId);
        } catch (Exception e) {
            log.error("Failed to record metric: {} for service {}", metricType, serviceId, e);
        }
    }

    /**
     * Records response time metric.
     *
     * @param serviceId Service ID
     * @param responseTimeMs Response time in milliseconds
     */
    @Async
    public void recordResponseTime(String serviceId, long responseTimeMs) {
        recordMetricAsync(serviceId, ServiceMetric.MetricType.RESPONSE_TIME, responseTimeMs);
    }

    /**
     * Records request count metric.
     *
     * @param serviceId Service ID
     * @param count Request count
     */
    @Async
    public void recordRequestCount(String serviceId, int count) {
        recordMetricAsync(serviceId, ServiceMetric.MetricType.REQUEST_COUNT, count);
    }

    /**
     * Records error rate metric.
     *
     * @param serviceId Service ID
     * @param errorRate Error percentage (0-100)
     */
    @Async
    public void recordErrorRate(String serviceId, double errorRate) {
        recordMetricAsync(serviceId, ServiceMetric.MetricType.ERROR_RATE, errorRate);
    }

    /**
     * Gets metrics for a service within a time range.
     *
     * @param serviceId Service ID
     * @param start Start time
     * @param end End time
     * @return List of metrics
     */
    public List<ServiceMetric> getMetrics(String serviceId, LocalDateTime start, LocalDateTime end) {
        return metricRepository.findByServiceDefinitionIdAndTimestampBetween(serviceId, start, end);
    }

    /**
     * Gets metrics of a specific type.
     *
     * @param serviceId Service ID
     * @param metricType Metric type
     * @param start Start time
     * @param end End time
     * @return List of metrics
     */
    public List<ServiceMetric> getMetricsByType(String serviceId, ServiceMetric.MetricType metricType,
                                               LocalDateTime start, LocalDateTime end) {
        return metricRepository.findByServiceDefinitionIdAndMetricTypeAndTimestampBetween(
            serviceId, metricType, start, end);
    }

    /**
     * Gets metric statistics.
     *
     * @param serviceId Service ID
     * @param metricType Metric type
     * @param start Start time
     * @param end End time
     * @return Metric statistics
     */
    public MetricStatistics getStatistics(String serviceId, ServiceMetric.MetricType metricType,
                                        LocalDateTime start, LocalDateTime end) {

        List<ServiceMetric> metrics = getMetricsByType(serviceId, metricType, start, end);

        if (metrics.isEmpty()) {
            return MetricStatistics.builder()
                .metricType(metricType)
                .count(0)
                .average(0.0)
                .min(0.0)
                .max(0.0)
                .sum(0.0)
                .build();
        }

        List<Double> values = metrics.stream()
            .map(ServiceMetric::getMetricValue)
            .sorted()
            .collect(Collectors.toList());

        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double average = sum / values.size();
        double min = values.get(0);
        double max = values.get(values.size() - 1);

        return MetricStatistics.builder()
            .metricType(metricType)
            .count(values.size())
            .average(average)
            .min(min)
            .max(max)
            .sum(sum)
            .median(calculatePercentile(values, 50))
            .p95(calculatePercentile(values, 95))
            .p99(calculatePercentile(values, 99))
            .build();
    }

    /**
     * Gets average metric value.
     *
     * @param serviceId Service ID
     * @param metricType Metric type
     * @param start Start time
     * @param end End time
     * @return Average value
     */
    public Double getAverageMetric(String serviceId, ServiceMetric.MetricType metricType,
                                  LocalDateTime start, LocalDateTime end) {
        Double avg = metricRepository.getAverageMetric(serviceId, metricType, start, end);
        return avg != null ? avg : 0.0;
    }

    /**
     * Gets time-series data aggregated by window.
     *
     * @param serviceId Service ID
     * @param metricType Metric type
     * @param start Start time
     * @param end End time
     * @param windowMinutes Aggregation window in minutes
     * @return Time-series data points
     */
    public List<TimeSeriesDataPoint> getTimeSeries(String serviceId, ServiceMetric.MetricType metricType,
                                                  LocalDateTime start, LocalDateTime end, int windowMinutes) {

        List<ServiceMetric> metrics = getMetricsByType(serviceId, metricType, start, end);
        Map<LocalDateTime, List<Double>> buckets = new TreeMap<>();

        // Group metrics into time buckets
        for (ServiceMetric metric : metrics) {
            LocalDateTime bucket = metric.getTimestamp()
                .withSecond(0)
                .withNano(0)
                .minusMinutes(metric.getTimestamp().getMinute() % windowMinutes);

            buckets.computeIfAbsent(bucket, k -> new ArrayList<>())
                .add(metric.getMetricValue());
        }

        // Calculate aggregated values for each bucket
        return buckets.entrySet().stream()
            .map(entry -> {
                List<Double> values = entry.getValue();
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

                return TimeSeriesDataPoint.builder()
                    .timestamp(entry.getKey())
                    .value(avg)
                    .min(min)
                    .max(max)
                    .count(values.size())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Deletes old metrics.
     *
     * @param retentionDays Number of days to retain
     * @return Number of deleted records
     */
    @Transactional
    public int deleteOldMetrics(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        log.info("Deleting metrics older than {} days (before {})", retentionDays, cutoffDate);

        List<ServiceMetric> oldMetrics = metricRepository.findAll().stream()
            .filter(m -> m.getTimestamp().isBefore(cutoffDate))
            .collect(Collectors.toList());

        int count = oldMetrics.size();

        if (count > 0) {
            metricRepository.deleteAll(oldMetrics);
            log.info("Deleted {} old metric entries", count);
        }

        return count;
    }

    /**
     * Calculates percentile value.
     */
    private double calculatePercentile(List<Double> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }

        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    /**
     * Metric statistics response.
     */
    @lombok.Builder
    @lombok.Data
    public static class MetricStatistics {
        private ServiceMetric.MetricType metricType;
        private int count;
        private double average;
        private double min;
        private double max;
        private double sum;
        private double median;
        private double p95;  // 95th percentile
        private double p99;  // 99th percentile
    }

    /**
     * Time-series data point.
     */
    @lombok.Builder
    @lombok.Data
    public static class TimeSeriesDataPoint {
        private LocalDateTime timestamp;
        private double value;   // Average value
        private double min;
        private double max;
        private int count;      // Number of data points in this bucket
    }
}
