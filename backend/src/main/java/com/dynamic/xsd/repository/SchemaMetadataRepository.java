package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.entity.SchemaMetadata.SchemaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SchemaMetadata entity.
 */
@Repository
public interface SchemaMetadataRepository extends JpaRepository<SchemaMetadata, String> {

    Optional<SchemaMetadata> findByServiceName(String serviceName);

    boolean existsByServiceName(String serviceName);

    List<SchemaMetadata> findByStatus(SchemaStatus status);

    Page<SchemaMetadata> findByStatus(SchemaStatus status, Pageable pageable);

    Page<SchemaMetadata> findByServiceNameContainingIgnoreCase(String serviceName, Pageable pageable);

    @Query("SELECT s FROM SchemaMetadata s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR LOWER(s.serviceName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SchemaMetadata> findByFilters(SchemaStatus status, String search, Pageable pageable);

    @Query("SELECT COUNT(s) FROM SchemaMetadata s WHERE s.status = :status")
    long countByStatus(SchemaStatus status);

    @Query("SELECT s FROM SchemaMetadata s WHERE s.uploadedBy = :username")
    List<SchemaMetadata> findByUploadedBy(String username);

    List<SchemaMetadata> findTop10ByOrderByUploadedAtDesc();
}
