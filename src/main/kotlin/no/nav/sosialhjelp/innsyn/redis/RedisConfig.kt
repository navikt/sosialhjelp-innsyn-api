package no.nav.sosialhjelp.innsyn.redis

import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

@Configuration
@Profile("!mock-redis")
@EnableCaching
class RedisConfig(
    @Value("\${innsyn.cache.time_to_live_seconds}") private val defaultTTL: Long,
    @Value("\${innsyn.cache.dokument_cache_time_to_live_seconds}") private val dokumentTTL: Long,
) {
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val valueSerializationPair = Jackson2JsonRedisSerializer(objectMapper, Any::class.java).let {
            RedisSerializationContext.fromSerializer(it).valueSerializationPair
        }
        val defaults = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(defaultTTL)).serializeValuesWith(valueSerializationPair)
        val cacheManager =
            RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults)
                .withCacheConfiguration("dokument", defaults.entryTtl(Duration.ofSeconds(dokumentTTL)))
                .withCacheConfiguration("navenhet", defaults.entryTtl(Duration.ofHours(1))).build()
        return cacheManager
    }
}
