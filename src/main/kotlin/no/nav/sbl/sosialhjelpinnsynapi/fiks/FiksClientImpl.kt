package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.core.JsonProcessingException
import kotlinx.coroutines.runBlocking
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.lagNavEksternRefId
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.filformatObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.util.Collections.singletonList


@Profile("!mock")
@Component
class FiksClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate,
                     private val idPortenService: IdPortenService,
                     private val redisStore: RedisStore) : FiksClient {

    companion object {
        val log by logger()
        const val CACHE_TIME_TO_LIVE_SECONDS: Long = 20
    }

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    override fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak {
        log.info("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        return when {
            useCache -> hentDigisosSakFraCache(digisosId, token)
            else -> hentDigisosSakFraFiks(digisosId, token)
        }
    }

    private fun hentDigisosSakFraCache(digisosId: String, token: String): DigisosSak {
        val get: String? = redisStore.get(digisosId)
        if (get != null) {
            try {
                val obj = objectMapper.readValue(get, DigisosSak::class.java)
                log.info("Hentet digisosSak fra cache, digisosId=$digisosId")
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
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)

            log.info("Hentet DigisosSak fra Fiks, digisosId=$digisosId")
            return objectMapper.readValue(response.body!!, DigisosSak::class.java)
        } catch (e: HttpStatusCodeException) {
            log.warn("Fiks - hentDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentDigisosSak feilet", e)
            throw FiksException(null, e.message, e)
        }
    }

    /**
     * Brukes for å hente json-filer som er definert i filformat. Dermed brukes filformatObjectMapper
     */
    override fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any {
        val get: String? = redisStore.get(dokumentlagerId)
        if (get != null) {
            try {
                val obj = filformatObjectMapper.readValue(get, requestedClass)
                valider(obj)
                log.info("Hentet ${requestedClass.simpleName} dokument fra cache, dokumentlagerId=$dokumentlagerId")
                return obj
            } catch (e: IOException) {
                log.warn("Fant key=$dokumentlagerId i cache, men value var ikke ${requestedClass.simpleName}")
            }
        }

        log.info("Forsøker å hente dokument fra $baseUrl/digisos/api/v1/soknader/nav/$digisosId/dokumenter/$dokumentlagerId")

        val headers = setIntegrasjonHeaders(token)
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId/dokumenter/$dokumentlagerId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)

            log.info("Hentet dokument (${requestedClass.simpleName}) fra Fiks, dokumentlagerId=$dokumentlagerId")
            val dokument = filformatObjectMapper.readValue(response.body!!, requestedClass)
            cachePut(dokumentlagerId, filformatObjectMapper.writeValueAsString(dokument))
            return dokument

        } catch (e: HttpStatusCodeException) {
            log.warn("Fiks - hentDokument feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentDokument feilet", e)
            throw FiksException(null, e.message, e)
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

    /**
     * lagrer digisosSak i cache i 20s
     */
    private fun cachePut(key: String, value: String) {
        val set = redisStore.set(key, value, CACHE_TIME_TO_LIVE_SECONDS)
        if (set == null) {
            log.warn("Cache put feilet eller fikk timeout")
        } else if (set == "OK") {
            log.info("Cache put OK $key")
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = setIntegrasjonHeaders(token)
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<String>>())

            return response.body!!.map { s: String -> objectMapper.readValue(s, DigisosSak::class.java) }

        } catch (e: HttpStatusCodeException) {
            log.warn("Fiks - hentAlleDigisosSaker feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentAlleDigisosSaker feilet", e)
            throw FiksException(null, e.message, e)
        }
    }

    override fun hentKommuneInfo(kommunenummer: String): KommuneInfo {
        val virksomhetsToken = runBlocking { idPortenService.requestToken() }

        val headers = setIntegrasjonHeaders("Bearer ${virksomhetsToken.token}")

        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/nav/kommune/$kommunenummer", HttpMethod.GET, HttpEntity<Nothing>(headers), KommuneInfo::class.java)

            return response.body!!

        } catch (e: HttpStatusCodeException) {
            log.warn("Fiks - hentKommuneInfo feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentKommuneInfo feilet", e)
            throw FiksException(null, e.message, e)
        }
    }

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, soknadId: String, token: String) {
        val headers = setIntegrasjonHeaders(token)
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        body.add("vedlegg.json", createHttpEntityOfString(serialiser(vedleggSpesifikasjon), "vedlegg.json"))

        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add("vedleggSpesifikasjon:$fileId", createHttpEntityOfString(serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))
            body.add("dokument:$fileId", createHttpEntityOfFile(file, "dokument:$fileId"))
        }

        val digisosSak = hentDigisosSakFraFiks(soknadId, token)
        val kommunenummer = digisosSak.kommunenummer
        val navEksternRefId = lagNavEksternRefId(digisosSak)

        val requestEntity = HttpEntity(body, headers)
        try {
            val path = "$baseUrl/digisos/api/v1/soknader/$kommunenummer/$soknadId/$navEksternRefId"
            restTemplate.exchange(path, HttpMethod.POST, requestEntity, String::class.java)

            log.info("Ettersendelse sendt til Fiks")

        } catch (e: HttpStatusCodeException) {
            log.warn(e.responseBodyAsString)
            log.warn("Opplasting av ettersendelse feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Opplasting av ettersendelse feilet", e)
            throw FiksException(null, e.message, e)
        }

    }

    private fun createHttpEntityOfString(body: String, name: String): HttpEntity<Any> {
        return createHttpEntity(body, name, null, "text/plain;charset=UTF-8")
    }

    private fun createHttpEntityOfFile(file: FilForOpplasting, name: String): HttpEntity<Any> {
        return createHttpEntity(InputStreamResource(file.fil), name, file.filnavn, "application/octet-stream")
    }

    private fun createHttpEntity(body: Any, name: String, filename: String?, contentType: String): HttpEntity<Any> {
        val headerMap = LinkedMultiValueMap<String, String>()
        val builder: ContentDisposition.Builder = ContentDisposition
                .builder("form-data")
                .name(name)
        val contentDisposition: ContentDisposition = if (filename == null) builder.build() else builder.filename(filename).build()

        headerMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        headerMap.add(HttpHeaders.CONTENT_TYPE, contentType)
        return HttpEntity(body, headerMap)
    }

    private fun serialiser(@NonNull metadata: Any): String {
        try {
            return objectMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
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
}

data class VedleggMetadata(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long
)