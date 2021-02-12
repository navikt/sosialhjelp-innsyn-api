package no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg

import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.forwardHeaders
import no.nav.sbl.sosialhjelpinnsynapi.utils.isRunningInProd
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.runAsyncWithMDC
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors


@Profile("!mock")
@Component
class KrypteringServiceImpl(
        clientProperties: ClientProperties,
        private val restTemplate: RestTemplate
) : KrypteringService {

    @Value("\${spring.profiles.active}")
    private val activeProfile: String? = null

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    private val executor = Executors.newFixedThreadPool(4)

    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, token: String, digisosId: String): InputStream {
        val kryptering = CMSKrypteringImpl()
        val certificate = getDokumentlagerPublicKeyX509Certificate(token)

        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringFuture = runAsyncWithMDC(Runnable {
                try {
                    log.debug("Starter kryptering, digisosId=$digisosId")
                    if (!isRunningInProd() && activeProfile?.contains("mock-alt") == true) {
                        IOUtils.copy(fileInputStream, pipedOutputStream)
                    } else {
                        kryptering.krypterData(pipedOutputStream, fileInputStream, certificate, Security.getProvider("BC"))
                    }
                    log.debug("Ferdig med kryptering, digisosId=$digisosId")
                } catch (e: Exception) {
                    log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", e)
                    throw IllegalStateException("An error occurred during encryption", e)
                } finally {
                    try {
                        log.debug("Lukker kryptering OutputStream, digisosId=$digisosId")
                        pipedOutputStream.close()
                        log.debug("OutputStream for kryptering er lukket, digisosId=$digisosId")
                    } catch (e: IOException) {
                        log.error("Lukking av Outputstream for kryptering feilet", e)
                    }
                }
            }, executor)
            krypteringFutureList.add(krypteringFuture)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return pipedInputStream
    }

    private fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate {
        val headers = forwardHeaders()
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
            throw FiksClientException(e.rawStatusCode, e.message, e)
        } catch (e: HttpServerErrorException) {
            log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksServerException(e.rawStatusCode, e.message, e)
        } catch (e: Exception) {
            throw FiksException(e.message, e)
        }
    }

    companion object {
        private val log by logger()
    }
}
