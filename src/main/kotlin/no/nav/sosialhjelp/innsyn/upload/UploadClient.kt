package no.nav.sosialhjelp.innsyn.upload

import kotlinx.coroutines.reactive.awaitSingle
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.util.UUID

@Component
class UploadClient(
    webClientBuilder: WebClient.Builder,
    httpClient: HttpClient,
    private val clientProperties: ClientProperties,
    private val texasClient: TexasClient,
) {
    private val webClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(clientProperties.uploadEndpointUrl)
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(sosialhjelpJsonMapper))
            }.build()

    suspend fun getVedleggJson(
        id: UUID,
        klageId: UUID,
    ): JsonVedleggSpesifikasjon {
        val bearerToken = texasClient.getTokenXToken(clientProperties.uploadAudience, TokenUtils.getToken())
        val spec =
            webClient
                .get()
                .uri("/sosialhjelp/upload/vedlegg/{id}", id.toString())
                .header(HttpHeaders.AUTHORIZATION, bearerToken.withBearer())
                .retrieve()
                .bodyToMono<VedleggSpesifikasjon>()
                .onErrorResume(WebClientResponseException::class.java) { ex ->
                    // Behandle 404 som ingen treff -> tom liste
                    if (ex.statusCode.value() == 404) {
                        Mono.just(VedleggSpesifikasjon(emptyList()))
                    } else {
                        Mono.error(ex)
                    }
                }.awaitSingle()
        return JsonVedleggSpesifikasjon().withVedlegg(
            spec.vedlegg.mapNotNull { vedlegg ->
                if (vedlegg.kategori == null) return@mapNotNull null

                JsonVedlegg()
                    .withType(resolveType(id, klageId))
                    .withStatus(if (vedlegg.filer.isNotEmpty()) "LASTET_OPP" else "INGEN_VEDLEGG")
                    .withHendelseType(JsonVedlegg.HendelseType.BRUKER)
                    .withHendelseReferanse(id.toString())
                    .withKlageId(klageId.toString())
                    .withFiler(vedlegg.filer.map { JsonFiler().withFilnavn(it.filnavn).withSha512(it.sha512) })
            },
        )
    }
}

private fun resolveType(
    navEksternRefId: UUID,
    klageId: UUID,
): String = if (navEksternRefId == klageId) "klage" else "klage_ettersendelse"

data class VedleggSpesifikasjon(
    val vedlegg: List<Vedlegg>,
)

data class Vedlegg(
    val kategori: String? = null,
    val filer: List<Fil>,
)

data class Fil(
    val filnavn: String,
    val sha512: String? = null,
)
