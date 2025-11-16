package com.dynamic.xsd.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a rate limit token bucket.
 *
 * Token Bucket Algorithm:
 * - tokens_remaining: Current available tokens
 * - max_tokens: Maximum capacity
 * - refill_rate: Tokens added per second
 * - last_refill_at: Last time tokens were added
 * - window_reset_at: When window resets completely
 */
@Entity
@Table(name = "rate_limit_buckets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"identifier", "bucket_type"}),
    indexes = {
        @Index(name = "idx_rate_limit_identifier", columnList = "identifier"),
        @Index(name = "idx_rate_limit_type", columnList = "bucket_type")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String identifier;  // User ID, service name, IP address, or "global"

    @Enumerated(EnumType.STRING)
    @Column(name = "bucket_type", nullable = false)
    private BucketType bucketType;

    @Column(nullable = false)
    private Integer tokensRemaining;

    @Column(nullable = false)
    private Integer maxTokens;

    @Column(nullable = false)
    private Integer refillRate;  // Tokens per second

    @Column(nullable = false)
    private LocalDateTime lastRefillAt;

    @Column(nullable = false)
    private LocalDateTime windowResetAt;

    public enum BucketType {
        USER,    // Per-user rate limit
        SERVICE, // Per-service rate limit
        IP,      // Per-IP address rate limit
        GLOBAL   // Global platform rate limit
    }
}
