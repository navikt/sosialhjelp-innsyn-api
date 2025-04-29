package no.nav.sosialhjelp.innsyn.digisosapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

interface DokumentlagerClient {
    suspend fun getDokumentlagerPublicKeyX509Certificate(): X509Certificate
}

@Component
class DokumentlagerClientImpl(
    private val fiksWebClient: WebClient,
    private val texasClient: TexasClient,
) : DokumentlagerClient {
    private var cachedPublicKey: X509Certificate? = null

    override suspend fun getDokumentlagerPublicKeyX509Certificate(): X509Certificate {
        cachedPublicKey?.let { return it }

        return withContext(Dispatchers.IO) {
            val publicKey =
                runCatching {
                    fiksWebClient
                        .get()
                        .uri(FiksPaths.PATH_DOKUMENTLAGER_PUBLICKEY)
                        .accept(APPLICATION_JSON)
                        .header(AUTHORIZATION, BEARER + texasClient.getMaskinportenToken())
                        .retrieve()
                        .awaitBody<ByteArray>()
                }.onFailure {
                    if (it is WebClientResponseException) {
                        log.warn("Fiks - getDokumentlagerPublicKey feilet - ${it.statusCode} ${it.statusText}", it)
                        when {
                            it.statusCode.is4xxClientError -> throw FiksClientException(it.statusCode.value(), it.message, it)
                            else -> throw FiksServerException(it.statusCode.value(), it.message, it)
                        }
                    }
                }.getOrThrow()

            log.info("Hentet public key for dokumentlager")

            try {
                val certificateFactory = CertificateFactory.getInstance("X.509")
                (certificateFactory.generateCertificate(ByteArrayInputStream(publicKey)) as X509Certificate).also {
                    cachedPublicKey = it
                }
            } catch (e: CertificateException) {
                throw IllegalStateException(e)
            }
        }
    }

    companion object {
        private val log by logger()
    }
}
