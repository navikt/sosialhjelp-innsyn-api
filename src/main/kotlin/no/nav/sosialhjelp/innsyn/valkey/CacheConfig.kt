package no.nav.sosialhjelp.innsyn.valkey

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import no.nav.sosialhjelp.innsyn.utils.logger
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
import org.springframework.data.redis.serializer.RedisSerializationContext.fromSerializer
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.util.ClassUtils
import tools.jackson.core.TreeNode
import tools.jackson.databind.DatabindContext
import tools.jackson.databind.DefaultTyping
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JavaType
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.databind.jsontype.NamedType
import tools.jackson.databind.jsontype.PolymorphicTypeValidator
import tools.jackson.databind.jsontype.TypeIdResolver
import tools.jackson.databind.jsontype.impl.ClassNameIdResolver
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder
import tools.jackson.module.kotlin.kotlinModule
import java.time.Duration
import java.util.LinkedHashMap
import java.util.LinkedHashSet

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

internal val cacheValueSerializer =
    GenericJacksonJsonRedisSerializer
        .builder {
            JsonSosialhjelpObjectMapper
                .createJsonMapperBuilder()
                .addModule(kotlinModule())
        }.customize {
            it.configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
            it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            it.setDefaultTyping(CacheTypeResolverBuilder(cacheTypeValidator))
        }.enableSpringCacheNullValueSupport()
        .build()

private class CacheTypeResolverBuilder(
    typeValidator: PolymorphicTypeValidator,
) : DefaultTypeResolverBuilder(typeValidator, DefaultTyping.NON_FINAL, "@class") {
    override fun withDefaultImpl(defaultImpl: Class<*>): DefaultTypeResolverBuilder = this

    override fun useForType(javaType: JavaType): Boolean {
        if (javaType.isJavaLangObject) return true

        if (javaType.isEnumType || ClassUtils.isPrimitiveOrWrapper(javaType.rawClass)) return false

        if (javaType.isFinal && !javaType.rawClass.name.startsWith("kotlin.") && javaType.rawClass.packageName.startsWith("java")) {
            return false
        }

        return !TreeNode::class.java.isAssignableFrom(javaType.rawClass)
    }

    override fun idResolver(
        context: DatabindContext,
        baseType: JavaType,
        subtypeValidator: PolymorphicTypeValidator,
        subtypes: Collection<NamedType>,
        forSerialization: Boolean,
        forDeserialization: Boolean,
    ): TypeIdResolver = NormalizingClassNameIdResolver(baseType, subtypes, subtypeValidator)
}

private class NormalizingClassNameIdResolver(
    baseType: JavaType,
    subtypes: Collection<NamedType>,
    typeValidator: PolymorphicTypeValidator,
) : ClassNameIdResolver(baseType, subtypes, typeValidator) {
    override fun _idFrom(
        context: DatabindContext,
        value: Any,
        valueType: Class<*>,
    ): String = super._idFrom(context, value, normalizedCollectionType(value, valueType))

    private fun normalizedCollectionType(
        value: Any,
        valueType: Class<*>,
    ): Class<*> =
        if (!valueType.name.startsWith("kotlin.")) {
            valueType
        } else {
            when (value) {
                is Map<*, *> -> LinkedHashMap::class.java
                is Set<*> -> LinkedHashSet::class.java
                is Collection<*> -> ArrayList::class.java
                else -> valueType
            }
        }
}

private object CacheDefaults {
    val defaultTTL: Duration = Duration.ofMinutes(1L)
    val keySerializationPair =
        fromSerializer(StringRedisSerializer()).keySerializationPair
    val valueSerializationPair =
        fromSerializer(cacheValueSerializer).valueSerializationPair
}

abstract class InnsynApiCacheConfig(
    val cacheName: String,
    private val ttl: Duration? = null,
) {
    open fun getConfig(): RedisCacheConfiguration =
        RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(ttl ?: CacheDefaults.defaultTTL)
            .serializeValuesWith(CacheDefaults.valueSerializationPair)
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
