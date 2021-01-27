package no.nav.sbl.sosialhjelpinnsynapi.redis

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.DigisosSakTilhorerAnnenBrukerException
import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.IOException

interface RedisService {
    val defaultTimeToLiveSeconds: Long
    fun get(key: String, requestedClass: Class<out Any>): Any?
    fun put(key: String, value: ByteArray, timeToLiveSeconds: Long = defaultTimeToLiveSeconds)
}

@Profile("!no-redis")
@Component
class RedisServiceImpl(
        private val redisStore: RedisStore,
        cacheProperties: CacheProperties
) : RedisService {

    override val defaultTimeToLiveSeconds = cacheProperties.timeToLiveSeconds

    override fun get(key: String, requestedClass: Class<out Any>): Any? {
        val get: ByteArray? = redisStore.get(key) // Redis har konfigurert timout for disse.
        return if (get != null) {
            try {
                val obj = objectMapper.readValue(get, requestedClass)
                valider(obj)
                log.debug("Hentet ${requestedClass.simpleName} fra cache, key=$key")
                obj
            } catch (e: IOException) {
                log.warn("Fant key=$key i cache, men value var ikke ${requestedClass.simpleName}")
                null
            } catch (e: DigisosSakTilhorerAnnenBrukerException) {
                log.warn("DigisosSak i cache tilhører en annen bruker enn brukeren fra token.")
                return null
            }
        } else {
            null
        }
    }

    override fun put(key: String, value: ByteArray, timeToLiveSeconds: Long) {
        val set = redisStore.set(key, value, timeToLiveSeconds)
        if (set == null) {
            log.warn("Cache put feilet eller fikk timeout")
        } else if (set == "OK") {
            log.debug("Cache put OK $key")
        }
    }

    /**
     * Kaster feil hvis det finnes additionalProperties på mappet objekt.
     * Tyder på at noe feil har skjedd ved mapping.
     */
    private fun valider(obj: Any?) {
        when {
            obj is DigisosSak && obj.sokerFnr != getUserIdFromToken() -> throw DigisosSakTilhorerAnnenBrukerException("DigisosSak tilhører annen bruker")
            obj is JsonDigisosSoker && obj.additionalProperties.isNotEmpty() -> throw IOException("JsonDigisosSoker har ukjente properties - må tilhøre ett annet objekt. Cache-value tas ikke i bruk")
            obj is JsonSoknad && obj.additionalProperties.isNotEmpty() -> throw IOException("JsonSoknad har ukjente properties - må tilhøre ett annet objekt. Cache-value tas ikke i bruk")
            obj is JsonVedleggSpesifikasjon && obj.additionalProperties.isNotEmpty() -> throw IOException("JsonVedleggSpesifikasjon har ukjente properties - må tilhøre ett annet objekt. Cache-value tas ikke i bruk")
        }
    }

    companion object {
        private val log by logger()
    }
}

@Profile("no-redis")
@Component
class RedisServiceMock : RedisService {
    override val defaultTimeToLiveSeconds: Long = 1

    override fun get(key: String, requestedClass: Class<out Any>): Any? {
        return null
    }

    override fun put(key: String, value: ByteArray, timeToLiveSeconds: Long) {

    }
}