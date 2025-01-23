package no.nav.sosialhjelp.innsyn.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
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
        val valueSerializationPair =
            JdkSerializationRedisSerializer().let {
                RedisSerializationContext.fromSerializer(it).valueSerializationPair
            }
        val keySerializationPair =
            StringRedisSerializer().let {
                RedisSerializationContext.fromSerializer(it).keySerializationPair
            }
        val defaults =
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(defaultTTL))
                .serializeValuesWith(valueSerializationPair).serializeKeysWith(keySerializationPair)

        // Må eksplisitt konfigurere opp alle caches her for å få metrics på alle
        val cacheManager =
            RedisCacheManager.builder(connectionFactory).enableStatistics().cacheDefaults(defaults).enableCreateOnMissingCache()
                .withCacheConfiguration("dokument", defaults.entryTtl(Duration.ofSeconds(dokumentTTL)))
                .withCacheConfiguration("navenhet", defaults.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("digisosSak", defaults)
                .withCacheConfiguration("kommuneinfo", defaults)
                .withCacheConfiguration("pdlPerson", defaults)
                .withCacheConfiguration("historiske-identer", defaults)
                .build()
        return cacheManager
    }
}
