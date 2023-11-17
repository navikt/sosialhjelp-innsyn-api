package no.nav.sosialhjelp.innsyn.digisosapi

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


@Component
class DokumentlagerClient(
    private val fiksWebClient: WebClient,
    private val maskinportenClient: MaskinportenClient,
) {
    private var cachedPublicKey: X509Certificate? = null
    private val log by logger()
    suspend fun getDokumentlagerPublicKeyX509Certificate(): X509Certificate {
        cachedPublicKey?.let { return it }

        val publicKey = try {
            fiksWebClient.get()
                .uri(FiksPaths.PATH_DOKUMENTLAGER_PUBLICKEY)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + maskinportenClient.getToken())
                .retrieve()
                .awaitBodyOrNull<ByteArray>()
                ?: throw BadStateException("Ingen feil, men heller ingen publicKey")
        } catch (e: WebClientResponseException) {
            log.warn("Fiks - getDokumentlagerPublicKey feilet - ${e.statusCode} ${e.statusText}", e)
            when {
                e.statusCode.is4xxClientError -> throw FiksClientException(e.statusCode.value(), e.message, e)
                else -> throw FiksServerException(e.statusCode.value(), e.message, e)
            }
        }

        log.info("Hentet public key for dokumentlager")

        val certificateFactory = CertificateFactory.getInstance("X.509")
        return (certificateFactory.generateCertificate(ByteArrayInputStream(publicKey)) as X509Certificate)
            .also { cachedPublicKey = it }

    }
}
