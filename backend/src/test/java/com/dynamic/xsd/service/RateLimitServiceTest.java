package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.RateLimitBucket;
import com.dynamic.xsd.domain.enums.BucketType;
import com.dynamic.xsd.dto.RateLimitStatus;
import com.dynamic.xsd.repository.RateLimitBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimitBucketRepository bucketRepository;

    @InjectMocks
    private RateLimitService rateLimitService;

    private RateLimitBucket testBucket;

    @BeforeEach
    void setUp() {
        testBucket = new RateLimitBucket();
        testBucket.setId("test-bucket-1");
        testBucket.setIdentifier("test-user");
        testBucket.setBucketType(BucketType.USER);
        testBucket.setTokensRemaining(50);
        testBucket.setMaxTokens(100);
        testBucket.setRefillRate(10);
        testBucket.setLastRefillAt(LocalDateTime.now().minusSeconds(5));
        testBucket.setWindowResetAt(LocalDateTime.now().plusMinutes(1));
    }

    @Test
    void testAllowRequest_SufficientTokens_ReturnsTrue() {
        // Arrange
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        boolean allowed = rateLimitService.allowRequest("test-user", BucketType.USER, 10);

        // Assert
        assertTrue(allowed);
        verify(bucketRepository).save(any(RateLimitBucket.class));
    }

    @Test
    void testAllowRequest_InsufficientTokens_ReturnsFalse() {
        // Arrange
        testBucket.setTokensRemaining(5);
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        boolean allowed = rateLimitService.allowRequest("test-user", BucketType.USER, 10);

        // Assert
        assertFalse(allowed);
    }

    @Test
    void testAllowRequest_NewBucket_CreatesAndAllows() {
        // Arrange
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.empty());
        when(bucketRepository.save(any(RateLimitBucket.class)))
                .thenReturn(testBucket);

        // Act
        boolean allowed = rateLimitService.allowRequest("new-user", BucketType.USER, 1);

        // Assert
        assertTrue(allowed);
        verify(bucketRepository, times(2)).save(any(RateLimitBucket.class));
    }

    @Test
    void testRefillTokens_AfterTimeElapsed_RefillsCorrectly() {
        // Arrange
        testBucket.setTokensRemaining(50);
        testBucket.setLastRefillAt(LocalDateTime.now().minusSeconds(10)); // 10 seconds ago
        testBucket.setRefillRate(10); // 10 tokens per second
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        rateLimitService.allowRequest("test-user", BucketType.USER, 1);

        // Assert
        verify(bucketRepository).save(argThat(bucket ->
            bucket.getTokensRemaining() >= 99 && bucket.getTokensRemaining() <= 100
        ));
    }

    @Test
    void testRefillTokens_DoesNotExceedMaxTokens() {
        // Arrange
        testBucket.setTokensRemaining(90);
        testBucket.setMaxTokens(100);
        testBucket.setLastRefillAt(LocalDateTime.now().minusSeconds(100)); // Long time ago
        testBucket.setRefillRate(10);
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        rateLimitService.allowRequest("test-user", BucketType.USER, 1);

        // Assert
        verify(bucketRepository).save(argThat(bucket ->
            bucket.getTokensRemaining() <= bucket.getMaxTokens()
        ));
    }

    @Test
    void testCheckStatus_ExistingBucket_ReturnsCorrectStatus() {
        // Arrange
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        RateLimitStatus status = rateLimitService.checkStatus("test-user", BucketType.USER);

        // Assert
        assertNotNull(status);
        assertEquals("test-user", status.getIdentifier());
        assertEquals(BucketType.USER, status.getBucketType());
        assertEquals(100, status.getMaxTokens());
        assertEquals(10, status.getRefillRate());
        assertFalse(status.isBlocked());
    }

    @Test
    void testResetBucket_ExistingBucket_ResetsSuccessfully() {
        // Arrange
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        rateLimitService.resetBucket("test-user", BucketType.USER);

        // Assert
        verify(bucketRepository).save(argThat(bucket ->
            bucket.getTokensRemaining().equals(bucket.getMaxTokens())
        ));
    }

    @Test
    void testConfigureBucket_UpdatesConfiguration() {
        // Arrange
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        rateLimitService.configureBucket("test-user", BucketType.USER, 200, 20);

        // Assert
        verify(bucketRepository).save(argThat(bucket ->
            bucket.getMaxTokens() == 200 && bucket.getRefillRate() == 20
        ));
    }

    @Test
    void testAllowRequest_ZeroTokenCost_AlwaysAllows() {
        // Arrange
        testBucket.setTokensRemaining(0);
        when(bucketRepository.findByIdentifierAndBucketType(anyString(), any(BucketType.class)))
                .thenReturn(Optional.of(testBucket));

        // Act
        boolean allowed = rateLimitService.allowRequest("test-user", BucketType.USER, 0);

        // Assert
        assertTrue(allowed);
    }
}
