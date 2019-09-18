package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.core.JsonProcessingException
import no.ks.fiks.streaming.klient.HttpHeader
import no.ks.fiks.streaming.klient.StreamingKlient
import no.ks.fiks.streaming.klient.authentication.PersonIntegrasjonAuthenticationStrategy
import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.InputStreamContentProvider
import org.eclipse.jetty.client.util.InputStreamResponseListener
import org.eclipse.jetty.client.util.MultiPartContentProvider
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Profile
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.Collections.singletonList
import java.util.concurrent.*
import kotlin.collections.ArrayList
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

    private val executor = Executors.newFixedThreadPool(4)

    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)

        log.info("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        if (digisosId == digisos_stub_id) {
            log.info("Hentet stub - digisosId $digisosId")
            return objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        }
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

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)
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

    override fun lastOppNyEttersendelse(files: List<MultipartFile>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, kommunenummer: String, soknadId: String, token: String): String? {
        val krypteringFutureList = Collections.synchronizedList<CompletableFuture<Void>>(ArrayList<CompletableFuture<Void>>(files.size))

        val navEksternRefId = "11000001"

        val contentProvider = MultiPartContentProvider()
        contentProvider.addFieldPart("vedlegg.json", StringContentProvider(serialiser(vedleggSpesifikasjon)), null)

        var fileId = 0
        files.forEach { file ->
            val vedleggMetadata = VedleggMetadata(file.originalFilename, file.contentType, file.size)

            val base64EncodetVedlegg: ByteArray = Base64Utils.encode(file.bytes)
            val byteArrayInputStream = ByteArrayInputStream(base64EncodetVedlegg)

            val kryptering = CMSKrypteringImpl()
            val certificate = getDokumentlagerPublicKeyX509Certificate(token)

            val pipedInputStream = PipedInputStream()
            try {
                val pipedOutputStream = PipedOutputStream(pipedInputStream)
                val krypteringFuture = CompletableFuture.runAsync(Runnable {
                    try {
                        log.debug("Starting encryption...")
                        kryptering.krypterData(pipedOutputStream, byteArrayInputStream, certificate, Security.getProvider("BC"))
                        log.debug("Encryption completed")
                    } catch (e: Exception) {
                        log.error("Encryption failed, setting exception on encrypted InputStream", e)
                        throw IllegalStateException("An error occurred during encryption", e)
                    } finally {
                        try {
                            log.debug("Closing encryption OutputStream")
                            pipedOutputStream.close()
                            log.debug("Encryption OutputStream closed")
                        } catch (e: IOException) {
                            log.error("Failed closing encryption OutputStream", e)
                        }
                    }
                }, executor)
                krypteringFutureList.add(krypteringFuture)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            contentProvider.addFieldPart(String.format("vedleggSpesifikasjon:%s", fileId), StringContentProvider(serialiser(vedleggMetadata)), null)
            contentProvider.addFilePart(String.format("dokument:%s", fileId), file.originalFilename, InputStreamContentProvider(pipedInputStream), null)
            fileId++
        }

        contentProvider.close()

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
            } else {
                val content = IOUtils.toString(listener.inputStream, "UTF-8")
                throw ResponseStatusException(httpStatus, content)
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
        } finally {
            client.stop()
            waitForFutures(krypteringFutureList)
        }

    }

    private fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate {
        val streamingKlient = StreamingKlient(PersonIntegrasjonAuthenticationStrategy(token.substringAfter("Bearer "), UUID.fromString(fiksIntegrasjonid), fiksIntegrasjonpassord))
        val httpHeaders = singletonList(getHttpHeaderRequestId())
        val response = streamingKlient.sendGetRawContentRequest(JettyHttpMethod.GET, "https://api.fiks.test.ks.no/", "/digisos/api/v1/dokumentlager-public-key", httpHeaders)

        val publicKey = response.result
        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")

            return certificateFactory.generateCertificate(ByteArrayInputStream(publicKey)) as X509Certificate

        } catch (e: CertificateException) {
            throw RuntimeException(e)
        }
    }

    private fun getHttpHeaderRequestId(): HttpHeader {
        var requestId = UUID.randomUUID().toString()
        if (MDC.get("requestid") != null) {
            requestId = MDC.get("requestid")
        }
        return HttpHeader.builder().headerName("requestid").headerValue(requestId).build()
    }

    private fun waitForFutures(krypteringFutureList: List<CompletableFuture<Void>>) {
        val allFutures = CompletableFuture.allOf(*krypteringFutureList.toTypedArray())
        try {
            allFutures.get(300, TimeUnit.SECONDS)
        } catch (e: CompletionException) {
            throw IllegalStateException(e.cause)
        } catch (e: ExecutionException) {
            throw IllegalStateException(e)
        } catch (e: TimeoutException) {
            throw IllegalStateException(e)
        } catch (e: InterruptedException) {
            throw IllegalStateException(e)
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