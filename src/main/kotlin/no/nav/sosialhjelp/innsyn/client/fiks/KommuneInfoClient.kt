package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.client.fiks.FiksPaths.PATH_ALLE_KOMMUNEINFO
import no.nav.sosialhjelp.innsyn.client.fiks.FiksPaths.PATH_KOMMUNEINFO
import no.nav.sosialhjelp.innsyn.client.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.utils.typeRef
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient

@Component
class KommuneInfoClient(
    private val maskinportenClient: MaskinportenClient,
    private val clientProperties: ClientProperties,
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient
) {

    fun getAll(): List<KommuneInfo> {
        return kommuneInfoWebClient.get()
            .uri(PATH_ALLE_KOMMUNEINFO)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, BEARER + maskinportenClient.getToken())
            .header(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            .header(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            .retrieve()
            .bodyToMono(typeRef<List<KommuneInfo>>())
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Fiks - hentKommuneInfoForAlle feilet", e)
                when {
                    e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                    else -> FiksServerException(e.rawStatusCode, e.message, e)
                }
            }
            .block()
            ?: emptyList()
    }

    fun getKommuneInfo(kommunenummer: String): KommuneInfo {
        return kommuneInfoWebClient.get()
            .uri(PATH_KOMMUNEINFO, kommunenummer)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, BEARER + maskinportenClient.getToken())
            .header(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            .header(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            .retrieve()
            .bodyToMono<KommuneInfo>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Fiks - hentKommuneInfoForAlle feilet", e)
                when {
                    e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                    else -> FiksServerException(e.rawStatusCode, e.message, e)
                }
            }
            .block()
            ?: throw RuntimeException("Noe feil skjedde ved henting av KommuneInfo for kommune=$kommunenummer")
    }

    private val kommuneInfoWebClient: WebClient =
        webClientBuilder
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .build()

    companion object {
        private val log by logger()
    }
}
