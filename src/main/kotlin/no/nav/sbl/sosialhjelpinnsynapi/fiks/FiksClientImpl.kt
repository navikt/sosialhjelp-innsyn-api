package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.lagNavEksternRefId
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.InputStreamContentProvider
import org.eclipse.jetty.client.util.InputStreamResponseListener
import org.eclipse.jetty.client.util.MultiPartContentProvider
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.util.Collections.singletonList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.eclipse.jetty.http.HttpMethod as JettyHttpMethod

private val log = LoggerFactory.getLogger(FiksClientImpl::class.java)

@Profile("!mock")
@Component
class FiksClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : FiksClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        val headers = setPersonIntegrasjonHeaders(token)

        log.info("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.info("Hentet DigisosSak $digisosId fra Fiks")
                return objectMapper.readValue(response.body!!, DigisosSak::class.java)
            } else {
                log.warn("Noe feilet ved kall til Fiks")
                throw FiksException(response.statusCode, "something went wrong")
            }
        } catch (e: RestClientResponseException) {
            throw FiksException(HttpStatus.valueOf(e.rawStatusCode), e.responseBodyAsString)
        }
    }

    override fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any {
        val headers = setPersonIntegrasjonHeaders(token)

        log.info("Forsøker å hente dokument fra $baseUrl/digisos/api/v1/soknader/nav/$digisosId/dokumenter/$dokumentlagerId")
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId/dokumenter/$dokumentlagerId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.info("Hentet dokument (${requestedClass.simpleName}) fra fiks, dokumentlagerId $dokumentlagerId")
                return objectMapper.readValue(response.body!!, requestedClass)
            } else {
                log.warn("Noe feilet ved kall til Fiks")
                throw FiksException(response.statusCode, "something went wrong")
            }
        } catch (e: RestClientResponseException) {
            throw FiksException(HttpStatus.valueOf(e.rawStatusCode), e.responseBodyAsString)
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = setPersonIntegrasjonHeaders(token)
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<String>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!.map { s: String -> objectMapper.readValue(s, DigisosSak::class.java) }
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw FiksException(response.statusCode, "something went wrong")
        }
    }

    override fun hentKommuneInfo(kommunenummer: String): KommuneInfo {
        val response = restTemplate.getForEntity("$baseUrl/digisos/api/v1/nav/kommune/$kommunenummer", KommuneInfo::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til fiks")
            throw FiksException(response.statusCode, "something went wrong")
        }
    }

    override fun postFiles(files: List<FilForOpplasting>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, soknadId: String, token: String) {
        val headers = setPersonIntegrasjonHeaders(token)
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        body.add("vedlegg.json", createHttpEntityOfString(serialiser(vedleggSpesifikasjon), "vedlegg.json"))

        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add("vedleggSpesifikasjon:$fileId", createHttpEntityOfString(serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))
            body.add("dokument:$fileId", createHttpEntityOfFile(file, "dokument:$fileId"))
        }

        val digisosSak = hentDigisosSak(soknadId, token)
        val kommunenummer = digisosSak.kommunenummer
        val navEksternRefId = lagNavEksternRefId(digisosSak)

        val requestEntity = HttpEntity(body, headers)
        try {
            val response = restTemplate.exchange(
                    "$baseUrl/digisos/api/v1/soknader/$kommunenummer/$soknadId/$navEksternRefId",
                    HttpMethod.POST,
                    requestEntity,
                    String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.info("Sendte ettersendelse til Fiks")
            } else if (response.statusCode.is4xxClientError) {
                log.warn("Opplasting av ettersendelse feilet")
                throw FiksException(response.statusCode, "Opplasting til Fiks feilet")
            }
        } catch (e: HttpClientErrorException) {
            e.printStackTrace()
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
        val contentDisposition = ContentDisposition
                .builder("form-data")
                .name(name)
                .filename(filename)
                .build()

        headerMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        headerMap.add(HttpHeaders.CONTENT_TYPE, contentType)
        return HttpEntity(body, headerMap)
    }

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, soknadId: String, token: String) {
        val contentProvider = MultiPartContentProvider()
        contentProvider.addFieldPart("vedlegg.json", StringContentProvider(serialiser(vedleggSpesifikasjon)), null)

        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            contentProvider.addFieldPart("vedleggSpesifikasjon:$fileId", StringContentProvider(serialiser(vedleggMetadata)), null)
            contentProvider.addFilePart("dokument:$fileId", file.filnavn, InputStreamContentProvider(file.fil), null)
        }

        contentProvider.close()

        val digisosSak = hentDigisosSak(soknadId, token)
        val kommunenummer = digisosSak.kommunenummer
        val navEksternRefId = lagNavEksternRefId(digisosSak)
        val path = "/digisos/api/v1/soknader/$kommunenummer/$soknadId/$navEksternRefId"
        val listener = InputStreamResponseListener()
        val sslContextFactory = SslContextFactory.Client()
        val client = HttpClient(sslContextFactory)
        client.isFollowRedirects = false
        client.start()
        val request = client.newRequest(baseUrl)

        request.header(AUTHORIZATION, token)
                .header(HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
                .header(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)
                .method(JettyHttpMethod.POST)
                .path(path)
                .content(contentProvider)
                .send(listener)

        try {
            val response = listener.get(30L, TimeUnit.SECONDS)
            val httpStatus = HttpStatus.valueOf(response.status)
            if (httpStatus.is2xxSuccessful) {
                log.info("Sendte ettersendelse til Fiks")
            } else if (httpStatus.is4xxClientError) {
                log.warn("Opplasting av ettersendelse feilet")
                throw FiksException(httpStatus, "Opplasting til Fiks feilet")
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Feil under invokering av api", e)
        } catch (e: TimeoutException) {
            throw RuntimeException("Feil under invokering av api", e)
        } catch (e: ExecutionException) {
            throw RuntimeException("Feil under invokering av api", e)
        } catch (e: IOException) {
            throw RuntimeException("Feil under invokering av api", e)
        } finally {
            client.stop()
        }
    }

    private fun serialiser(@NonNull metadata: Any): String {
        try {
            return objectMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
        }
    }

    private fun setPersonIntegrasjonHeaders(token: String): HttpHeaders {
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