package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.*
import java.security.Security
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors


@Profile("!mock")
@Component
class KrypteringServiceImpl(clientProperties: ClientProperties,
                            private val restTemplate: RestTemplate) : KrypteringService, AutoCloseable {

    companion object {
        val log by logger()
    }

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord
    private val executor = Executors.newFixedThreadPool(4)
    private var certificate: X509Certificate? = null
    private var securityProvider = Security.getProvider("BC")
    private val kryptering = CMSKrypteringImpl()

    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, token: String, filDigisosId: String): InputStream {

        if (certificate == null) {
            certificate = getDokumentlagerPublicKeyX509Certificate(token)
        }
        if (securityProvider == null) {
            securityProvider = Security.getProvider("BC")
        }

        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringFuture = CompletableFuture.runAsync(Runnable {
                try {
                    log.info("Starter kryptering, filDigisosId=$filDigisosId")
                    kryptering.krypterData(pipedOutputStream, fileInputStream, certificate, securityProvider)
                    log.info("Ferdig med kryptering, filDigisosId=$filDigisosId")
                } catch (e: Exception) {
                    log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream, filDigisosId=$filDigisosId", e)
                    throw IllegalStateException("An error occurred during encryption", e)
                } finally {
                    try {
                        log.info("Lukker kryptering OutputStream, filDigisosId=$filDigisosId")
                        pipedOutputStream.close()
                        fileInputStream.close()
                        log.info("OutputStream for kryptering er lukket, filDigisosId=$filDigisosId")
                    } catch (e: IOException) {
                        log.error("Lukking av Outputstream for kryptering feilet, filDigisosId=$filDigisosId", e)
                    }
                }
            }, executor)
            krypteringFutureList.add(krypteringFuture)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return pipedInputStream
    }

    fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.AUTHORIZATION, token)
        headers.set(IntegrationUtils.HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(IntegrationUtils.HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)

        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/dokumentlager-public-key", org.springframework.http.HttpMethod.GET, HttpEntity<Nothing>(headers), ByteArray::class.java)
            log.info("Hentet public key for dokumentlager")
            val publicKey = response.body
            try {
                val certificateFactory = CertificateFactory.getInstance("X.509")

                return certificateFactory.generateCertificate(ByteArrayInputStream(publicKey)) as X509Certificate

            } catch (e: CertificateException) {
                throw RuntimeException(e)
            }
        } catch (e: HttpClientErrorException) {
            log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksClientException(e.statusCode, e.message, e)
        } catch (e: HttpServerErrorException) {
            log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksServerException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            throw FiksException(e.message, e)
        }
    }

    override fun close() {
        executor.shutdownNow()
    }
}