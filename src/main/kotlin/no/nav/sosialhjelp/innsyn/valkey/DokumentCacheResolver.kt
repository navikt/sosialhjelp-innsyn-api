package no.nav.sosialhjelp.innsyn.valkey

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.interceptor.CacheOperationInvocationContext
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!mock-redis")
class DokumentCacheResolverConfig {
    @Bean
    fun dokumentCacheResolver(cacheManager: CacheManager): CacheResolver = DokumentCacheResolver(cacheManager)
}

internal class DokumentCacheResolver(
    private val cacheManager: CacheManager,
) : CacheResolver {
    private val log by logger()

    override fun resolveCaches(context: CacheOperationInvocationContext<*>): Collection<Cache> {
        val requestedClass =
            context.args.filterIsInstance<Class<*>>().singleOrNull()
                ?: error(
                    "Expected requested document class as the only argument of type Class<*> to cacheable method, but got: ${context.args}",
                )
        val cacheName = cacheNameFor(requestedClass)

        val cache = cacheManager.getCache(cacheName)
        if (cache == null) {
            log.warn("Missing cache configuration for $cacheName")
        }
        return listOfNotNull(cache)
    }
}

fun cacheNameFor(requestedClass: Class<*>): String =
    when (requestedClass) {
        JsonDigisosSoker::class.java -> JsonDigisosSokerCacheConfig.CACHE_NAME
        JsonSoknad::class.java -> JsonSoknadCacheConfig.CACHE_NAME
        JsonVedleggSpesifikasjon::class.java -> JsonVedleggSpesifikasjonCacheConfig.CACHE_NAME
        else -> error("Unsupported document type for cache: ${requestedClass.name}")
    }
