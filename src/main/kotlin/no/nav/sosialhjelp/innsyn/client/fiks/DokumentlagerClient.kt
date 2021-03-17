package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


interface DokumentlagerClient {
    fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate
}

@Profile("!mock")
@Component
class DokumentlagerClientImpl(
    private val clientProperties: ClientProperties,
    private val fiksWebClient: WebClient,
) : DokumentlagerClient {

    override fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate {
        val publicKey = fiksWebClient.get()
            .uri(FiksPaths.PATH_DOKUMENTLAGER_PUBLICKEY)
            .headers { it.addAll(IntegrationUtils.fiksHeaders(clientProperties, token)) }
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) {
                it.createException().map { e ->
                    log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
                    FiksClientException(e.rawStatusCode, e.message, e)
                }
            }
            .onStatus(HttpStatus::is5xxServerError) {
                it.createException().map { e ->
                    log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
                    FiksServerException(e.rawStatusCode, e.message, e)
                }
            }
            .bodyToMono<ByteArray>()
            .block()

        log.info("Hentet public key for dokumentlager")

        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            return certificateFactory.generateCertificate(ByteArrayInputStream(publicKey!!)) as X509Certificate
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val log by logger()
    }
}