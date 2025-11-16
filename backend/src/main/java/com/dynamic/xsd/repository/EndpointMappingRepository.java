package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.EndpointMapping;
import com.dynamic.xsd.domain.entity.EndpointMapping.EndpointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for EndpointMapping entity.
 */
@Repository
public interface EndpointMappingRepository extends JpaRepository<EndpointMapping, String> {

    List<EndpointMapping> findByServiceDefinitionId(String serviceDefinitionId);

    List<EndpointMapping> findByType(EndpointType type);

    List<EndpointMapping> findByServiceDefinitionIdAndType(String serviceDefinitionId, EndpointType type);

    @Query("SELECT e FROM EndpointMapping e WHERE e.serviceDefinition.serviceName = :serviceName")
    List<EndpointMapping> findByServiceName(String serviceName);

    @Query("SELECT COUNT(e) FROM EndpointMapping e WHERE e.type = :type AND e.active = true")
    long countActiveByType(EndpointType type);
}
