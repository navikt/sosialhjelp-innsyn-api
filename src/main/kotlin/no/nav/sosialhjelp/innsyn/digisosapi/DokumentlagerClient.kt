package no.nav.sosialhjelp.innsyn.digisosapi

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Qualifier
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
    private val fiksWebClient: WebClient,
    @Qualifier("maskinportenClient")
    private val maskinportenClient: MaskinportenClient,
) : DokumentlagerClient {
    private var cachedPublicKey: X509Certificate? = null

    override fun getDokumentlagerPublicKeyX509Certificate(): X509Certificate {
        cachedPublicKey?.let { return it }

        val publicKey =
            fiksWebClient.get()
                .uri(FiksPaths.PATH_DOKUMENTLAGER_PUBLICKEY)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + maskinportenClient.getToken())
                .retrieve()
                .bodyToMono<ByteArray>()
                .onErrorMap(WebClientResponseException::class.java) { e ->
                    log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
                    when {
                        e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), e.message, e)
                        else -> FiksServerException(e.statusCode.value(), e.message, e)
                    }
                }
                .block()
                ?: throw BadStateException("Ingen feil, men heller ingen publicKey")

        log.info("Hentet public key for dokumentlager")

        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            return (certificateFactory.generateCertificate(ByteArrayInputStream(publicKey)) as X509Certificate)
                .also { cachedPublicKey = it }
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val log by logger()
    }
}
