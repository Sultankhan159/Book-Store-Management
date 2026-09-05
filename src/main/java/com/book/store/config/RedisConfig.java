package com.book.store.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        if (!isRedisAvailable()) {
            log.warn("Redis not reachable on {}:{}. Distributed locking operating in local mutex fallback mode.", redisHost, redisPort);
            return null;
        }
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectTimeout(1000)
                .setTimeout(1000)
                .setRetryAttempts(0);
        try {
            return Redisson.create(config);
        } catch (Exception e) {
            log.warn("Redis not available on {}:{}. Distributed locking operating in fallback mode.", redisHost, redisPort);
            return null;
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> connectionFactoryProvider) {
        if (isRedisAvailable()) {
            try {
                RedisConnectionFactory connectionFactory = connectionFactoryProvider.getIfAvailable();
                if (connectionFactory != null) {
                    log.info("Redis is available at {}:{}. Initializing RedisCacheManager.", redisHost, redisPort);
                    RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofMinutes(10))
                            .disableCachingNullValues()
                            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

                    return RedisCacheManager.builder(connectionFactory)
                            .cacheDefaults(cacheConfig)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Failed to initialize RedisCacheManager: {}. Falling back to in-memory cache.", e.getMessage());
            }
        }
        log.info("Redis not reachable at {}:{}. Operating with in-memory ConcurrentMapCacheManager.", redisHost, redisPort);
        return new ConcurrentMapCacheManager();
    }

    private boolean isRedisAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(redisHost, redisPort), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resilient Cache Error Handler:
     * If Redis is temporarily down or unreachable, cache operations fail silently
     * and fallback to querying the database directly rather than failing the client request.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis Cache GET failed for key: '{}' in cache: '{}'. Falling back to database. Error: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis Cache PUT failed for key: '{}' in cache: '{}'. Error: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis Cache EVICT failed for key: '{}' in cache: '{}'. Error: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis Cache CLEAR failed for cache: '{}'. Error: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
