package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Collections


interface DokumentlagerClient {
    fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate
}

@Profile("!mock")
@Component
class DokumentlagerClientImpl(
    clientProperties: ClientProperties,
    private val restTemplate: RestTemplate
) : DokumentlagerClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    override fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate {
        val headers = IntegrationUtils.forwardHeaders()
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