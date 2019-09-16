package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.core.JsonProcessingException
import io.reactivex.internal.util.NotificationLite.isError
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.InputStreamContentProvider
import org.eclipse.jetty.client.util.InputStreamResponseListener
import org.eclipse.jetty.client.util.MultiPartContentProvider
import org.eclipse.jetty.client.util.StringContentProvider
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Collections.singletonList
import java.util.UUID.randomUUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.eclipse.jetty.http.HttpMethod as JettyHttpMethod


private val log = LoggerFactory.getLogger(FiksClientImpl::class.java)

private const val digisos_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Profile("!mock")
@Component
class FiksClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : FiksClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set("IntegrasjonId", fiksIntegrasjonid)
        headers.set("IntegrasjonPassord", fiksIntegrasjonpassord)

        log.info("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        if (digisosId == digisos_stub_id) {
            log.info("Hentet stub - digisosId $digisosId")
            return objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        }
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet DigisosSak $digisosId fra Fiks")
            return objectMapper.readValue(response.body!!, DigisosSak::class.java)
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set("IntegrasjonId", fiksIntegrasjonid)
        headers.set("IntegrasjonPassord", fiksIntegrasjonpassord)
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<String>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!.map { s: String -> objectMapper.readValue(s, DigisosSak::class.java) }
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun hentKommuneInfo(kommunenummer: String): KommuneInfo {
        val response = restTemplate.getForEntity("$baseUrl/digisos/api/v1/nav/kommune/$kommunenummer", KommuneInfo::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun lastOppNyEttersendelse(files: List<MultipartFile>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, kommunenummer: String, soknadId: String, token: String): String? {
        val navEksternRefId = randomUUID().toString()

        val contentProvider = MultiPartContentProvider()
        contentProvider.addFieldPart("vedlegg.json", StringContentProvider(serialiser(vedleggSpesifikasjon)), null)

        var fileId = 0
        files.forEach { file ->
            val vedleggMetadata = VedleggMetadata(file.originalFilename, file.contentType, file.size)

            val base64EncodetVedlegg: ByteArray = Base64Utils.encode(file.bytes)

            contentProvider.addFieldPart(String.format("vedleggSpesifikasjon:%s", fileId), StringContentProvider(serialiser(vedleggMetadata)), null)
            contentProvider.addFilePart(String.format("dokument:%s", fileId), file.originalFilename, InputStreamContentProvider(ByteArrayInputStream(base64EncodetVedlegg)), null)
            fileId++
        }

        contentProvider.close()

        val path = "/digisos/api/v1/soknader/$kommunenummer/$soknadId/$navEksternRefId"
        val listener = InputStreamResponseListener()
        val client = HttpClient()
        val request = client.newRequest(baseUrl)

        request.header(AUTHORIZATION, "Bearer $token")
                .header("IntegrasjonId", fiksIntegrasjonid)
                .header("IntegrasjonPassord", fiksIntegrasjonpassord)
                .method(JettyHttpMethod.POST)
                .path(path)
                .content(contentProvider)
                .send(listener)

        try {
            val response = listener.get(30L, TimeUnit.SECONDS)
            val status = response.status
            if (isError(status)) {
                val content = IOUtils.toString(listener.inputStream, "UTF-8")
                throw ResponseStatusException(HttpStatus.valueOf(response.status), content)
            }
            return objectMapper.readValue(listener.inputStream, String::class.java)
        } catch (e: InterruptedException) {
            throw RuntimeException("Feil under invokering av api", e)
        } catch (e: TimeoutException) {
            throw RuntimeException("Feil under invokering av api", e)
        } catch (e: ExecutionException) {
            throw RuntimeException("Feil under invokering av api", e)
        } catch (e: IOException) {
            throw RuntimeException("Feil under invokering av api", e)
        }

    }

    private fun serialiser(@NonNull metadata: Any): String {
        try {
            return objectMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
        }

    }
}

data class VedleggMetadata(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long
)