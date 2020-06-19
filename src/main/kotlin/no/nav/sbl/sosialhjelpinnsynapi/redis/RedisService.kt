package no.nav.sbl.sosialhjelpinnsynapi.redis

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class RedisService(
        private val redisStore: RedisStore,
        private val cacheProperties: CacheProperties
) {

    fun get(key: String, requestedClass: Class<out Any>): Any? {
        val get: String? = redisStore.get(key) // Redis har konfigurert timout for disse.
        return if (get != null) {
            try {
                val obj = objectMapper.readValue(get, requestedClass)
                valider(obj)
                log.info("Hentet ${requestedClass.simpleName} fra cache, key=$key")
                obj
            } catch (e: IOException) {
                log.info("Fant key=$key i cache, men value var ikke ${requestedClass.simpleName}")
                null
            }
        } else {
            null
        }
    }

    fun put(key: String, value: String) {
        val set = redisStore.set(key, value, cacheProperties.timeToLiveSeconds)
        if (set == null) {
            log.warn("Cache put feilet eller fikk timeout")
        } else if (set == "OK") {
            log.info("Cache put OK $key")
        }
    }

    /**
     * Kaster feil hvis det finnes additionalProperties på mappet objekt.
     * Tyder på at noe feil har skjedd ved mapping.
     */
    private fun valider(obj: Any?) {
        when {
            obj is JsonDigisosSoker && obj.additionalProperties.isNotEmpty() -> throw IOException("JsonDigisosSoker har ukjente properties - må tilhøre ett annet objekt. Cache-value tas ikke i bruk")
            obj is JsonSoknad && obj.additionalProperties.isNotEmpty() -> throw IOException("JsonSoknad har ukjente properties - må tilhøre ett annet objekt. Cache-value tas ikke i bruk")
            obj is JsonVedleggSpesifikasjon && obj.additionalProperties.isNotEmpty() -> throw IOException("JsonVedleggSpesifikasjon har ukjente properties - må tilhøre ett annet objekt. Cache-value tas ikke i bruk")
        }
    }

    companion object {
        private val log by logger()
    }
}