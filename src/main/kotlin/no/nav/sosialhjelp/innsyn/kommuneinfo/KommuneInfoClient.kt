package no.nav.sosialhjelp.innsyn.kommuneinfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksPaths.PATH_ALLE_KOMMUNEINFO
import no.nav.sosialhjelp.innsyn.digisosapi.FiksPaths.PATH_KOMMUNEINFO
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import kotlin.coroutines.cancellation.CancellationException

@Component
class KommuneInfoClient(
    private val texasClient: TexasClient,
    clientProperties: ClientProperties,
    webClientBuilder: WebClient.Builder,
    httpClient: HttpClient,
) {
    // TODO Sparer litt ved å cache på denne istedet
    suspend fun getAll(): List<KommuneInfo> =
        withContext(Dispatchers.IO) {
            kotlin
                .runCatching {
                    kommuneInfoWebClient
                        .get()
                        .uri(PATH_ALLE_KOMMUNEINFO)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(AUTHORIZATION, texasClient.getMaskinportenToken().withBearer())
                        .retrieve()
                        .awaitBody<List<KommuneInfo>>()
                }.onFailure {
                    if (it is CancellationException) currentCoroutineContext().ensureActive()
                    log.warn("Fiks - hentKommuneInfoForAlle feilet", it)
                    if (it is WebClientResponseException) {
                        when {
                            it.statusCode.is4xxClientError -> throw FiksClientException(it.statusCode.value(), it.message, it)
                            else -> throw FiksServerException(it.statusCode.value(), it.message, it)
                        }
                    }
                }.getOrNull()
                ?: emptyList()
        }

    @Cacheable("kommuneinfo")
    suspend fun getKommuneInfo(kommunenummer: String): KommuneInfo =
        withContext(Dispatchers.IO) {
            kotlin
                .runCatching {
                    kommuneInfoWebClient
                        .get()
                        .uri(PATH_KOMMUNEINFO, kommunenummer)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(AUTHORIZATION, texasClient.getMaskinportenToken().withBearer())
                        .retrieve()
                        .awaitBody<KommuneInfo>()
                }.onFailure {
                    if (it is CancellationException) currentCoroutineContext().ensureActive()
                    log.warn("Fiks - hentKommuneInfoForAlle feilet for kommune=$kommunenummer", it)
                    if (it is WebClientResponseException) {
                        when {
                            it.statusCode.is4xxClientError -> throw FiksClientException(it.statusCode.value(), it.message, it)
                            else -> throw FiksServerException(it.statusCode.value(), it.message, it)
                        }
                    }
                }.getOrThrow()
        }

    private val kommuneInfoWebClient: WebClient =
        webClientBuilder
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
            .defaultHeader(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            .defaultHeader(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            .build()

    companion object {
        private val log by logger()
    }
}
