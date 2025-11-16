package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.RateLimitBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RateLimitBucket entity.
 */
@Repository
public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, String> {

    Optional<RateLimitBucket> findByIdentifierAndBucketType(
        String identifier,
        RateLimitBucket.BucketType bucketType
    );

    List<RateLimitBucket> findByBucketType(RateLimitBucket.BucketType bucketType);

    List<RateLimitBucket> findByIdentifier(String identifier);

    @Modifying
    @Query("DELETE FROM RateLimitBucket r WHERE r.identifier = :identifier AND r.bucketType = :bucketType")
    void deleteByIdentifierAndBucketType(String identifier, RateLimitBucket.BucketType bucketType);

    @Query("SELECT r FROM RateLimitBucket r WHERE r.tokensRemaining = 0")
    List<RateLimitBucket> findExhaustedBuckets();

    @Query("SELECT r FROM RateLimitBucket r WHERE r.tokensRemaining < :threshold")
    List<RateLimitBucket> findBucketsNearLimit(int threshold);

    @Modifying
    @Query("DELETE FROM RateLimitBucket r WHERE r.windowResetAt < :before")
    void deleteExpiredBuckets(LocalDateTime before);

    @Query("SELECT COUNT(r) FROM RateLimitBucket r WHERE r.bucketType = :bucketType")
    long countByBucketType(RateLimitBucket.BucketType bucketType);
}
