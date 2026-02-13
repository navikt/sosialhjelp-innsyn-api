package no.nav.sosialhjelp.innsyn.valkey

import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.lang.RuntimeException
import java.time.Duration

// TODO: Migrer til å bruke Valkey på ordentlig. Vi kommer ikke til å kunne bruke nye valkey-features før dette er gjort
//   Vi bruker valkey, men behandler den som en redis-instans (bruker ikke valkey-features).
@Configuration
@Profile("!mock-redis")
@EnableCaching
class ValkeyConfig(
    @param:Value("\${innsyn.cache.time_to_live_seconds}") private val defaultTTL: Long,
    @param:Value("\${innsyn.cache.dokument_cache_time_to_live_seconds}") private val dokumentTTL: Long,
) : CachingConfigurer {
    override fun errorHandler() = CustomCacheErrorHandler

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
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTTL))
                .serializeValuesWith(valueSerializationPair)
                .serializeKeysWith(keySerializationPair)

        // Må eksplisitt konfigurere opp alle caches her for å få metrics på alle
        return RedisCacheManager
            .builder(connectionFactory)
            .enableStatistics()
            .cacheDefaults(defaults)
            .enableCreateOnMissingCache()
            .withCacheConfiguration("dokument", defaults.entryTtl(Duration.ofSeconds(dokumentTTL)))
            .withCacheConfiguration("navenhet", defaults.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("digisosSak", defaults)
            .withCacheConfiguration("kommuneinfo", defaults)
            // TODO Kan fjernes etter ny deploy
            .withCacheConfiguration("pdlPerson", defaults.entryTtl(Duration.ofDays(1)))
            .withCacheConfiguration("pdlNavn", defaults.entryTtl(Duration.ofDays(1)))
            .withCacheConfiguration("pdlAdressebeskyttelse", defaults.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("pdlHistoriskeIdenter", defaults.entryTtl(Duration.ofDays(1)))
            .withCacheConfiguration("pdlAdressebeskyttelseOld", defaults.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("pdlHistoriskeIdenterOld", defaults.entryTtl(Duration.ofDays(1)))
            .build()
    }
}

object CustomCacheErrorHandler : CacheErrorHandler {
    private val log by logger()

    override fun handleCacheGetError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        if (exception is SerializationException) cache.evict(key)
        log.warn("Couldn't get cache value for key $key in cache ${cache.name}", exception)
    }

    override fun handleCachePutError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
        value: Any?,
    ) {
        log.warn("Couldn't put cache value for key $key in cache ${cache.name}", exception)
    }

    override fun handleCacheEvictError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        log.warn("Couldn't evict cache value for key $key in cache ${cache.name}", exception)
    }

    override fun handleCacheClearError(
        exception: RuntimeException,
        cache: Cache,
    ) {
        log.warn("Couldn't clear cache ${cache.name}", exception)
    }
}
