package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.ServiceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ServiceMetric entity.
 */
@Repository
public interface ServiceMetricRepository extends JpaRepository<ServiceMetric, String> {

    List<ServiceMetric> findByServiceDefinitionId(String serviceDefinitionId);

    List<ServiceMetric> findByServiceDefinitionIdAndMetricType(
        String serviceDefinitionId,
        ServiceMetric.MetricType metricType
    );

    List<ServiceMetric> findByServiceDefinitionIdAndTimestampBetween(
        String serviceDefinitionId,
        LocalDateTime start,
        LocalDateTime end
    );

    List<ServiceMetric> findByServiceDefinitionIdAndMetricTypeAndTimestampBetween(
        String serviceDefinitionId,
        ServiceMetric.MetricType metricType,
        LocalDateTime start,
        LocalDateTime end
    );

    @Query("SELECT AVG(m.metricValue) FROM ServiceMetric m " +
           "WHERE m.serviceDefinitionId = :serviceId " +
           "AND m.metricType = :metricType " +
           "AND m.timestamp BETWEEN :start AND :end")
    Double getAverageMetric(String serviceId, ServiceMetric.MetricType metricType,
                           LocalDateTime start, LocalDateTime end);

    @Query("SELECT MAX(m.metricValue) FROM ServiceMetric m " +
           "WHERE m.serviceDefinitionId = :serviceId " +
           "AND m.metricType = :metricType " +
           "AND m.timestamp BETWEEN :start AND :end")
    Double getMaxMetric(String serviceId, ServiceMetric.MetricType metricType,
                       LocalDateTime start, LocalDateTime end);

    @Query("SELECT MIN(m.metricValue) FROM ServiceMetric m " +
           "WHERE m.serviceDefinitionId = :serviceId " +
           "AND m.metricType = :metricType " +
           "AND m.timestamp BETWEEN :start AND :end")
    Double getMinMetric(String serviceId, ServiceMetric.MetricType metricType,
                       LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("DELETE FROM ServiceMetric m WHERE m.timestamp < :before")
    void deleteMetricsBefore(LocalDateTime before);

    @Query("SELECT COUNT(m) FROM ServiceMetric m WHERE m.serviceDefinitionId = :serviceId")
    long countByServiceDefinitionId(String serviceId);
}
