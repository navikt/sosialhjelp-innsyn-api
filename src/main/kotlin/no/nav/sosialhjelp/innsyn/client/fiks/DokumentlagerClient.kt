package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.client.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

interface DokumentlagerClient {
    fun getDokumentlagerPublicKeyX509Certificate(): X509Certificate
}

@Component
class DokumentlagerClientImpl(
    private val clientProperties: ClientProperties,
    private val fiksWebClient: WebClient,
    private val maskinportenClient: MaskinportenClient
) : DokumentlagerClient {

    private var cachedPublicKey: X509Certificate? = null

    override fun getDokumentlagerPublicKeyX509Certificate(): X509Certificate {
        cachedPublicKey?.let { return it }

        val publicKey = fiksWebClient.get()
            .uri(FiksPaths.PATH_DOKUMENTLAGER_PUBLICKEY)
            .accept(APPLICATION_JSON)
            .header(AUTHORIZATION, BEARER + maskinportenClient.getToken())
            .header(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            .header(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            .retrieve()
            .bodyToMono<ByteArray>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
                when {
                    e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                    else -> FiksServerException(e.rawStatusCode, e.message, e)
                }
            }
            .block()

        log.info("Hentet public key for dokumentlager")

        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            return (certificateFactory.generateCertificate(ByteArrayInputStream(publicKey!!)) as X509Certificate)
                .also { cachedPublicKey = it }
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val log by logger()
    }
}
