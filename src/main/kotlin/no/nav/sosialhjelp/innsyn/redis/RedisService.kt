package no.nav.sosialhjelp.innsyn.redis

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.exceptions.DigisosSakTilhorerAnnenBrukerException
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.redis.RedisService.Companion.pakkUtRedisData
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.maskerFnr
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

interface RedisService {
    val defaultTimeToLiveSeconds: Long
    fun <T : Any> get(key: String, requestedClass: Class<out T>): T?
    fun put(key: String, value: ByteArray, timeToLiveSeconds: Long = defaultTimeToLiveSeconds)
    fun delete(key: String)

    companion object {
        fun <T : Any> pakkUtRedisData(bytes: ByteArray?, requestedClass: Class<out T>, key: String, log: Logger): T? =
            if (bytes != null) {
                try {
                    if (requestedClass == String::class.java) {
                        log.debug("Hentet ${requestedClass.simpleName} fra cache, key=${key.maskerFnr}")
                        String(bytes, StandardCharsets.UTF_8) as T
                    } else {
                        val obj = objectMapper.readValue(bytes, requestedClass)
                        valider(obj)
                        log.debug("Hentet ${requestedClass.simpleName} fra cache, key=${key.maskerFnr}")
                        obj
                    }
                } catch (e: IOException) {
                    log.warn("Fant key=${key.maskerFnr} i cache, men value var ikke ${requestedClass.simpleName}")
                    null
                } catch (e: DigisosSakTilhorerAnnenBrukerException) {
                    log.warn("DigisosSak i cache tilhører en annen bruker enn brukeren fra token.")
                    null
                }
            } else {
                log.debug("Fant ikke key=${key.maskerFnr}")
                null
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
    }
}

@Profile("!mock-redis")
@Component
class RedisServiceImpl(
    private val redisStore: RedisStore,
    cacheProperties: CacheProperties
) : RedisService {

    override val defaultTimeToLiveSeconds = cacheProperties.timeToLiveSeconds

    override fun <T : Any> get(key: String, requestedClass: Class<out T>): T? {
        val get: ByteArray? = redisStore.get(key) // Redis har konfigurert timout for disse.
        return pakkUtRedisData(get, requestedClass, key, log)
    }

    override fun put(key: String, value: ByteArray, timeToLiveSeconds: Long) {
        val set = redisStore.set(key, value, timeToLiveSeconds)
        if (set == null) {
            log.warn("Cache put feilet eller fikk timeout")
        } else if (set == "OK") {
            log.debug("Cache put OK ${key.maskerFnr}")
        }
    }

    override fun delete(key: String) {
        val delete = redisStore.delete(key)
        if (!delete) {
            log.warn("Cache delete feilet eller fikk timeout")
        } else {
            log.debug("Cache delete OK ${key.maskerFnr}")
        }
    }

    companion object {
        private val log by logger()
    }
}

@Profile("mock-redis")
@Component
class RedisServiceMock : RedisService {
    override val defaultTimeToLiveSeconds: Long = 1000
    val mockMap = HashMap<String, ByteArray>()
    val expiryMap = HashMap<String, LocalDateTime>()

    override fun <T : Any> get(key: String, requestedClass: Class<out T>): T? {
        val get: ByteArray? = mockMap[key]
        if (get != null) {
            val expiryTime = expiryMap[key]
            if (expiryTime == null || expiryTime.isBefore(LocalDateTime.now())) {
                return null
            }
        }
        return pakkUtRedisData(get, requestedClass, key, log)
    }

    override fun put(key: String, value: ByteArray, timeToLiveSeconds: Long) {
        mockMap[key] = value
        log.debug("redis set key=$key, value=$value")
        expiryMap[key] = LocalDateTime.now().plusSeconds(timeToLiveSeconds)
    }

    override fun delete(key: String) {
        if (mockMap.containsKey(key)) {
            mockMap.remove(key)
            log.debug("Slettet key=${key.maskerFnr}")
        }
        expiryMap.remove(key)
    }

    companion object {
        private val log by logger()
    }
}
