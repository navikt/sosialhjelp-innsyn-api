package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import kotlinx.coroutines.runBlocking
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.client.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksNotFoundException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.common.retry
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.redis.CacheProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.feilmeldingUtenFnr
import no.nav.sbl.sosialhjelpinnsynapi.utils.lagNavEksternRefId
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.toFiksErrorResponse
import no.nav.sbl.sosialhjelpinnsynapi.utils.typeRef
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.util.Collections.singletonList


@Profile("!mock")
@Component
class FiksClientImpl(
        clientProperties: ClientProperties,
        private val restTemplate: RestTemplate,
        private val idPortenService: IdPortenService,
        private val redisStore: RedisStore,
        private val cacheProperties: CacheProperties,
        private val retryProperties: FiksRetryProperties
) : FiksClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    override fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak {
        log.debug("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        return when {
            useCache -> hentDigisosSakFraCache(digisosId, token)
            else -> hentDigisosSakFraFiks(digisosId, token)
        }
    }

    fun hentDigisosSakFraCache(digisosId: String, token: String): DigisosSak {
        val get: String? = redisStore.get(digisosId) // Redis har konfigurert timout for disse.
        if (get != null) {
            try {
                val obj = objectMapper.readValue(get, DigisosSak::class.java)
                log.debug("Hentet digisosSak fra cache, digisosId=$digisosId")
                return obj
            } catch (e: IOException) {
                log.warn("Fant key=$digisosId i cache, men value var ikke DigisosSak")
            }
        }

        // kunne ikke finne digisosSak i cache. Henter fra Fiks og lagrer til cache
        val digisosSak = hentDigisosSakFraFiks(digisosId, token)
        cachePut(digisosId, objectMapper.writeValueAsString(digisosSak))
        return digisosSak
    }

    private fun hentDigisosSakFraFiks(digisosId: String, token: String): DigisosSak {
        val headers = setIntegrasjonHeaders(token)
        try {
            val urlTemplate = "$baseUrl/digisos/api/v1/soknader/{digisosId}"
            val response = restTemplate.exchange(urlTemplate, HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java, digisosId)

            log.debug("Hentet DigisosSak fra Fiks, digisosId=$digisosId")
            val body = response.body!!
            return objectMapper.readValue(body, DigisosSak::class.java)
        } catch (e: HttpClientErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDigisosSak feilet - $errorMessage - $fiksErrorResponse", e)
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw FiksNotFoundException(e.statusCode, errorMessage, e)
            }
            throw FiksClientException(e.statusCode, e.message, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDigisosSak feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksServerException(e.statusCode, errorMessage, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentDigisosSak feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    override fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any {
        val get: String? = redisStore.get(dokumentlagerId) // Redis har konfigurert timout for disse.
        if (get != null) {
            try {
                val obj = objectMapper.readValue(get, requestedClass)
                valider(obj)
                log.info("Hentet dokument (${requestedClass.simpleName}) fra cache, dokumentlagerId=$dokumentlagerId")
                return obj
            } catch (e: IOException) {
                log.warn("Fant key=$dokumentlagerId i cache, men value var ikke ${requestedClass.simpleName}")
            }
        }

        log.debug("Forsøker å hente dokument fra $baseUrl/digisos/api/v1/soknader/nav/$digisosId/dokumenter/$dokumentlagerId")

        val headers = setIntegrasjonHeaders(token)
        try {
            val urlTemplate = "$baseUrl/digisos/api/v1/soknader/{digisosId}/dokumenter/{dokumentlagerId}"
            val response = restTemplate.exchange(
                    urlTemplate,
                    HttpMethod.GET,
                    HttpEntity<Nothing>(headers),
                    String::class.java,
                    mapOf("digisosId" to digisosId, "dokumentlagerId" to dokumentlagerId))

            log.info("Hentet dokument (${requestedClass.simpleName}) fra Fiks, dokumentlagerId=$dokumentlagerId")
            val dokument = objectMapper.readValue(response.body!!, requestedClass)
            cachePut(dokumentlagerId, objectMapper.writeValueAsString(dokument))
            return dokument

        } catch (e: HttpClientErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDokument feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksClientException(e.statusCode, errorMessage, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDokument feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksServerException(e.statusCode, errorMessage, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentDokument feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
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

    private fun cachePut(key: String, value: String) {
        val set = redisStore.set(key, value, cacheProperties.timeToLiveSeconds)
        if (set == null) {
            log.warn("Cache put feilet eller fikk timeout")
        } else if (set == "OK") {
            log.debug("Cache put OK $key")
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = setIntegrasjonHeaders(token)
        try {

            return runBlocking {
                retry(
                        attempts = retryProperties.attempts,
                        initialDelay = retryProperties.initialDelay,
                        maxDelay = retryProperties.maxDelay,
                        retryableExceptions = *arrayOf(HttpServerErrorException::class)
                ) {
                    val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/soknader", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<DigisosSak>>())
                    response.body.orEmpty()
                }
            }

        } catch (e: HttpClientErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentAlleDigisosSaker feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksClientException(e.statusCode, errorMessage, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentAlleDigisosSaker feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksServerException(e.statusCode, errorMessage, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentAlleDigisosSaker feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    override fun hentKommuneInfo(kommunenummer: String): KommuneInfo {
        val get: String? = redisStore.get(kommunenummer) // Redis har konfigurert timout for disse.
        if (get != null) {
            try {
                val obj = objectMapper.readValue(get, KommuneInfo::class.java)
                log.info("Hentet kommuneInfo fra cache, kommunenummer=$kommunenummer")
                return obj
            } catch (e: IOException) {
                log.warn("Fant key=$kommunenummer i cache, men value var ikke KommuneInfo")
            }
        }
        val virksomhetsToken = runBlocking { idPortenService.requestToken() }
        val headers = setIntegrasjonHeaders("Bearer ${virksomhetsToken.token}")

        try {
            val urlTemplate = "$baseUrl/digisos/api/v1/nav/kommuner/{kommunenummer}"
            val kommuneInfo = runBlocking {
                retry(
                        attempts = retryProperties.attempts,
                        initialDelay = retryProperties.initialDelay,
                        maxDelay = retryProperties.maxDelay,
                        retryableExceptions = *arrayOf(HttpServerErrorException::class)
                ) {
                    val response = restTemplate.exchange(urlTemplate, HttpMethod.GET, HttpEntity<Nothing>(headers), KommuneInfo::class.java, kommunenummer)
                    response.body!!
                }
            }
            cachePut(kommunenummer, objectMapper.writeValueAsString(kommuneInfo))

            return kommuneInfo

        } catch (e: HttpClientErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfo feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksClientException(e.statusCode, errorMessage, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfo feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksServerException(e.statusCode, errorMessage, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentKommuneInfo feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    override fun hentKommuneInfoForAlle(): List<KommuneInfo> {
        val virksomhetsToken = runBlocking { idPortenService.requestToken() }

        val headers = setIntegrasjonHeaders("Bearer ${virksomhetsToken.token}")

        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/nav/kommuner", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<KommuneInfo>>())
            return response.body!!

        } catch (e: HttpClientErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfoForAlle feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksClientException(e.statusCode, errorMessage, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
            val errorMessage = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfoForAlle feilet - $errorMessage - $fiksErrorResponse", e)
            throw FiksServerException(e.statusCode, errorMessage, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentKommuneInfo feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    private fun setIntegrasjonHeaders(token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)
        return headers
    }

    companion object {
        private val log by logger()
    }
}
