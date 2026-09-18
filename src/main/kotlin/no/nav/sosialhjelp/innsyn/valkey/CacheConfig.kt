package no.nav.sosialhjelp.innsyn.valkey

import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapperBuilder
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
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.fromSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JavaType
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.time.Duration

// TODO: Migrer til å bruke Valkey på ordentlig. Vi kommer ikke til å kunne bruke nye valkey-features før dette er gjort
//   Vi bruker valkey, men behandler den som en redis-instans (bruker ikke valkey-features).
@Configuration
@Profile("!mock-redis")
@EnableCaching
class CacheConfig : CachingConfigurer {
    override fun errorHandler() = CustomCacheErrorHandler

    @Bean
    fun cacheManager(
        connectionFactory: RedisConnectionFactory,
        cacheConfigs: List<InnsynApiCacheConfig>,
    ): CacheManager =
        RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(
                RedisCacheConfiguration
                    .defaultCacheConfig()
                    .entryTtl(CacheDefaults.defaultTTL)
                    .serializeValuesWith(CacheDefaults.valueSerializationPair)
                    .serializeKeysWith(CacheDefaults.keySerializationPair),
            ).enableStatistics()
            .enableCreateOnMissingCache()
            .withInitialCacheConfigurations(cacheConfigs.associate { it.cacheName to it.getConfig() })
            .build()
}

private val cacheTypeValidator =
    BasicPolymorphicTypeValidator
        .builder()
        .allowIfSubType("no.nav.")
        .allowIfSubType("java.util.")
        .allowIfSubType("java.time.")
        .build()

internal val cacheMapper =
    sosialhjelpJsonMapperBuilder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

internal inline fun <reified T> cacheValueType(): JavaType = cacheMapper.typeFactory.constructType(object : TypeReference<T>() {}.type)

internal fun <T : Any> cacheValueSerializer(valueType: JavaType): RedisSerializer<T> = JacksonJsonRedisSerializer(cacheMapper, valueType)

internal val genericCacheValueSerializer =
    GenericJacksonJsonRedisSerializer
        .builder(::sosialhjelpJsonMapperBuilder)
        .enableDefaultTyping(cacheTypeValidator)
        .enableSpringCacheNullValueSupport()
        .build()

private object CacheDefaults {
    val defaultTTL: Duration = Duration.ofMinutes(1L)
    val keySerializationPair =
        fromSerializer(StringRedisSerializer()).keySerializationPair
    val valueSerializationPair =
        fromSerializer(genericCacheValueSerializer).valueSerializationPair
}

abstract class InnsynApiCacheConfig(
    val cacheName: String,
    valueType: JavaType? = null,
    private val ttl: Duration? = null,
) {
    private val valueSerializationPair =
        fromSerializer(valueType?.let(::cacheValueSerializer) ?: genericCacheValueSerializer).valueSerializationPair

    open fun getConfig(): RedisCacheConfiguration =
        RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(ttl ?: CacheDefaults.defaultTTL)
            .serializeValuesWith(valueSerializationPair)
            .serializeKeysWith(CacheDefaults.keySerializationPair)
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
