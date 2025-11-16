package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.RateLimitBucket;
import com.dynamic.xsd.repository.RateLimitBucketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for rate limiting using token bucket algorithm.
 *
 * Token Bucket Algorithm:
 * - Each user/service has a bucket with max tokens
 * - Tokens are consumed on each request
 * - Tokens refill at a constant rate
 * - Request blocked if insufficient tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitBucketRepository bucketRepository;

    // Default configuration
    private static final int DEFAULT_MAX_TOKENS = 100;
    private static final int DEFAULT_REFILL_RATE = 10; // tokens per second
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    /**
     * Checks if request is allowed for a user.
     *
     * @param username Username to check
     * @return true if request allowed, false if rate limited
     */
    @Transactional
    public boolean allowRequest(String username) {
        return allowRequest(username, RateLimitBucket.BucketType.USER, 1);
    }

    /**
     * Checks if request is allowed for a user with custom cost.
     *
     * @param username Username to check
     * @param tokenCost Number of tokens to consume
     * @return true if request allowed, false if rate limited
     */
    @Transactional
    public boolean allowRequest(String username, int tokenCost) {
        return allowRequest(username, RateLimitBucket.BucketType.USER, tokenCost);
    }

    /**
     * Checks if request is allowed for an identifier with specific bucket type.
     *
     * @param identifier User ID, service name, IP address, or "global"
     * @param bucketType Type of bucket (USER, SERVICE, IP, GLOBAL)
     * @param tokenCost Number of tokens to consume
     * @return true if request allowed, false if rate limited
     */
    @Transactional
    public boolean allowRequest(String identifier, RateLimitBucket.BucketType bucketType, int tokenCost) {
        log.debug("Checking rate limit for {} ({}) - cost: {}", identifier, bucketType, tokenCost);

        RateLimitBucket bucket = getOrCreateBucket(identifier, bucketType);

        // Refill tokens based on time elapsed
        refillTokens(bucket);

        // Check if enough tokens available
        if (bucket.getTokensRemaining() >= tokenCost) {
            bucket.setTokensRemaining(bucket.getTokensRemaining() - tokenCost);
            bucketRepository.save(bucket);
            log.debug("Request allowed for {} - tokens remaining: {}", identifier, bucket.getTokensRemaining());
            return true;
        }

        log.info("Request rate limited for {} ({}) - tokens remaining: {}, required: {}",
            identifier, bucketType, bucket.getTokensRemaining(), tokenCost);
        return false;
    }

    /**
     * Checks rate limit status without consuming tokens.
     *
     * @param identifier Identifier to check
     * @param bucketType Bucket type
     * @return RateLimitStatus with current state
     */
    public RateLimitStatus checkStatus(String identifier, RateLimitBucket.BucketType bucketType) {
        Optional<RateLimitBucket> bucketOpt = bucketRepository.findByIdentifierAndBucketType(identifier, bucketType);

        if (bucketOpt.isEmpty()) {
            return RateLimitStatus.builder()
                .identifier(identifier)
                .bucketType(bucketType)
                .tokensRemaining(DEFAULT_MAX_TOKENS)
                .maxTokens(DEFAULT_MAX_TOKENS)
                .refillRate(DEFAULT_REFILL_RATE)
                .resetAt(LocalDateTime.now().plusSeconds(DEFAULT_WINDOW_SECONDS))
                .build();
        }

        RateLimitBucket bucket = bucketOpt.get();
        refillTokens(bucket);

        return RateLimitStatus.builder()
            .identifier(identifier)
            .bucketType(bucketType)
            .tokensRemaining(bucket.getTokensRemaining())
            .maxTokens(bucket.getMaxTokens())
            .refillRate(bucket.getRefillRate())
            .resetAt(bucket.getWindowResetAt())
            .lastRefillAt(bucket.getLastRefillAt())
            .build();
    }

    /**
     * Resets rate limit for an identifier.
     *
     * @param identifier Identifier to reset
     * @param bucketType Bucket type
     */
    @Transactional
    public void resetRateLimit(String identifier, RateLimitBucket.BucketType bucketType) {
        log.info("Resetting rate limit for {} ({})", identifier, bucketType);
        bucketRepository.deleteByIdentifierAndBucketType(identifier, bucketType);
    }

    /**
     * Updates rate limit configuration for an identifier.
     *
     * @param identifier Identifier to configure
     * @param bucketType Bucket type
     * @param maxTokens Maximum tokens
     * @param refillRate Tokens per second
     */
    @Transactional
    public void configureRateLimit(String identifier, RateLimitBucket.BucketType bucketType,
                                   int maxTokens, int refillRate) {
        log.info("Configuring rate limit for {} ({}) - max: {}, rate: {}",
            identifier, bucketType, maxTokens, refillRate);

        RateLimitBucket bucket = getOrCreateBucket(identifier, bucketType);
        bucket.setMaxTokens(maxTokens);
        bucket.setRefillRate(refillRate);
        bucket.setTokensRemaining(maxTokens);
        bucket.setLastRefillAt(LocalDateTime.now());
        bucket.setWindowResetAt(LocalDateTime.now().plusSeconds(DEFAULT_WINDOW_SECONDS));
        bucketRepository.save(bucket);
    }

    /**
     * Gets or creates a rate limit bucket.
     */
    private RateLimitBucket getOrCreateBucket(String identifier, RateLimitBucket.BucketType bucketType) {
        return bucketRepository.findByIdentifierAndBucketType(identifier, bucketType)
            .orElseGet(() -> {
                log.info("Creating new rate limit bucket for {} ({})", identifier, bucketType);
                RateLimitBucket newBucket = RateLimitBucket.builder()
                    .identifier(identifier)
                    .bucketType(bucketType)
                    .tokensRemaining(DEFAULT_MAX_TOKENS)
                    .maxTokens(DEFAULT_MAX_TOKENS)
                    .refillRate(DEFAULT_REFILL_RATE)
                    .lastRefillAt(LocalDateTime.now())
                    .windowResetAt(LocalDateTime.now().plusSeconds(DEFAULT_WINDOW_SECONDS))
                    .build();
                return bucketRepository.save(newBucket);
            });
    }

    /**
     * Refills tokens based on elapsed time.
     */
    private void refillTokens(RateLimitBucket bucket) {
        LocalDateTime now = LocalDateTime.now();
        Duration elapsed = Duration.between(bucket.getLastRefillAt(), now);
        long secondsElapsed = elapsed.getSeconds();

        if (secondsElapsed > 0) {
            int tokensToAdd = (int) (secondsElapsed * bucket.getRefillRate());
            int newTokenCount = Math.min(
                bucket.getTokensRemaining() + tokensToAdd,
                bucket.getMaxTokens()
            );

            log.debug("Refilling tokens for {} - elapsed: {}s, adding: {}, new total: {}",
                bucket.getIdentifier(), secondsElapsed, tokensToAdd, newTokenCount);

            bucket.setTokensRemaining(newTokenCount);
            bucket.setLastRefillAt(now);

            // Reset window if expired
            if (now.isAfter(bucket.getWindowResetAt())) {
                bucket.setWindowResetAt(now.plusSeconds(DEFAULT_WINDOW_SECONDS));
                bucket.setTokensRemaining(bucket.getMaxTokens());
                log.debug("Window reset for {}", bucket.getIdentifier());
            }
        }
    }

    /**
     * Rate limit status response.
     */
    @lombok.Builder
    @lombok.Data
    public static class RateLimitStatus {
        private String identifier;
        private RateLimitBucket.BucketType bucketType;
        private int tokensRemaining;
        private int maxTokens;
        private int refillRate;
        private LocalDateTime resetAt;
        private LocalDateTime lastRefillAt;

        public double getCapacityPercent() {
            if (maxTokens == 0) return 0;
            return (tokensRemaining * 100.0) / maxTokens;
        }

        public boolean isNearLimit() {
            return getCapacityPercent() < 20;
        }

        public boolean isExhausted() {
            return tokensRemaining == 0;
        }
    }
}
