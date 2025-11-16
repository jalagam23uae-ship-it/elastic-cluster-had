package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.domain.entity.ServiceDefinition.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ServiceDefinition entity.
 */
@Repository
public interface ServiceDefinitionRepository extends JpaRepository<ServiceDefinition, String> {

    Optional<ServiceDefinition> findByServiceName(String serviceName);

    List<ServiceDefinition> findByStatus(ServiceStatus status);

    Page<ServiceDefinition> findByStatus(ServiceStatus status, Pageable pageable);

    List<ServiceDefinition> findBySchemaId(String schemaId);

    @Query("SELECT s FROM ServiceDefinition s WHERE s.status = 'DEPLOYED'")
    List<ServiceDefinition> findAllDeployed();

    @Query("SELECT COUNT(s) FROM ServiceDefinition s WHERE s.status = :status")
    long countByStatus(ServiceStatus status);

    @Query("SELECT SUM(s.totalRequests) FROM ServiceDefinition s WHERE s.status = 'DEPLOYED'")
    Long getTotalRequestsCount();

    @Query("SELECT AVG(s.averageResponseTime) FROM ServiceDefinition s WHERE s.status = 'DEPLOYED'")
    Double getAverageResponseTime();

    List<ServiceDefinition> findTop10ByOrderByDeployedAtDesc();

    Optional<ServiceDefinition> findFirstBySchemaId(String schemaId);
}
