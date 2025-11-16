package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.EndpointMapping;
import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.domain.enums.EndpointType;
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

    List<EndpointMapping> findByEndpointType(EndpointType endpointType);

    List<EndpointMapping> findByServiceDefinitionIdAndEndpointType(String serviceDefinitionId, EndpointType endpointType);

    @Query("SELECT e FROM EndpointMapping e WHERE e.serviceDefinition.serviceName = :serviceName")
    List<EndpointMapping> findByServiceName(String serviceName);

    @Query("SELECT COUNT(e) FROM EndpointMapping e WHERE e.endpointType = :endpointType AND e.active = true")
    long countActiveByEndpointType(EndpointType endpointType);

    List<EndpointMapping> findByServiceDefinitionAndEndpointType(ServiceDefinition serviceDefinition, EndpointType endpointType);

    long countByServiceDefinition(ServiceDefinition serviceDefinition);

    long countByServiceDefinitionAndEndpointType(ServiceDefinition serviceDefinition, EndpointType endpointType);
}
