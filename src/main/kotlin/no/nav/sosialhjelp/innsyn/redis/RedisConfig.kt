package no.nav.sosialhjelp.innsyn.redis

import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory.createRedisConfiguration
import java.time.Duration

private const val TIMEOUT_SECONDS: Long = 1

@Configuration
@Profile("!mock-redis")
@EnableCaching
class RedisConfig(
    @Value("\${redis_host}") private val host: String,
    @Value("\${redis_port}") private val port: Int,
    @Value("\${redis_password}") private val password: String,
    @Value("\${redis_username}") private val username: String,
    @Value("\${innsyn.cache.time_to_live_seconds}") private val defaultTTL: Long,
    @Value("\${innsyn.cache.dokument_cache_time_to_live_seconds}") private val dokumentTTL: Long,
) {
    @Bean
    fun connectionFactory(): RedisConnectionFactory {
        val redisURI =
            RedisURI
                .builder()
                .withHost(host)
                .withPort(port)
                .withAuthentication(username, password.toCharArray())
                .withTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .withSsl(true)
                .build()
        val redisConfig = createRedisConfiguration(redisURI)
        return LettuceConnectionFactory(redisConfig)
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val defaults = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(defaultTTL))
        val cacheManager =
            RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults)
                .withCacheConfiguration("dokument", defaults.entryTtl(Duration.ofSeconds(dokumentTTL)))
                .withCacheConfiguration("navenhet", defaults.entryTtl(Duration.ofHours(1))).build()
        return cacheManager
    }
}
