package com.book.store.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(2);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // In-memory fallback if Redis is unreachable or down
    private final ConcurrentHashMap<String, String> localFallbackMap = new ConcurrentHashMap<>();

    public enum IdempotencyStatus {
        ACQUIRED,
        PROCESSING,
        COMPLETED
    }

    /**
     * Atomically check and acquire the idempotency key.
     */
    public IdempotencyStatus checkAndAcquire(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        try {
            if (redisTemplate != null) {
                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", PROCESSING_TTL);
                if (Boolean.TRUE.equals(isNew)) {
                    return IdempotencyStatus.ACQUIRED;
                }
                Object existingStatus = redisTemplate.opsForValue().get(redisKey);
                if ("COMPLETED".equals(existingStatus)) {
                    return IdempotencyStatus.COMPLETED;
                }
                return IdempotencyStatus.PROCESSING;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for idempotency check. Using local fallback. Reason: {}", e.getMessage());
        }

        // Local fallback logic
        String existing = localFallbackMap.putIfAbsent(idempotencyKey, "PROCESSING");
        if (existing == null) {
            return IdempotencyStatus.ACQUIRED;
        } else if ("COMPLETED".equals(existing)) {
            return IdempotencyStatus.COMPLETED;
        }
        return IdempotencyStatus.PROCESSING;
    }

    /**
     * Mark the idempotency key as completed after successful operation.
     */
    public void markCompleted(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(redisKey, "COMPLETED", COMPLETED_TTL);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable to mark idempotency completed: {}", e.getMessage());
        }
        localFallbackMap.put(idempotencyKey, "COMPLETED");
    }

    /**
     * Release key if operation failed so user can retry safely.
     */
    public void release(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(redisKey);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable to release idempotency key: {}", e.getMessage());
        }
        localFallbackMap.remove(idempotencyKey);
    }
}
