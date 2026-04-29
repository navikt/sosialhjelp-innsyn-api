package no.nav.sosialhjelp.innsyn.digisosapi

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class FiksConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun fiksWebClient(
        webClientBuilder: WebClient.Builder,
        fiksHttpClient: HttpClient,
    ): WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(fiksHttpClient))
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(sosialhjelpJsonMapper))
                it.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(sosialhjelpJsonMapper))
                it.customCodecs().register(MultipartMixedReader())
            }.defaultHeader(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            .defaultHeader(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            .build()
}

/**
 * DefaultPartHttpMessageReader only supports multipart/form-data. This subclass extends it
 * to also accept multipart/mixed, which is used by the Fiks bulk-document endpoint.
 */
private class MultipartMixedReader : DefaultPartHttpMessageReader() {
    private val supportedTypes = super.getReadableMediaTypes() + MediaType.MULTIPART_MIXED

    override fun getReadableMediaTypes(): List<MediaType> = supportedTypes

    override fun canRead(
        elementType: ResolvableType,
        mediaType: MediaType?,
    ): Boolean =
        super.canRead(elementType, mediaType) ||
            (
                super.canRead(elementType, null) &&
                    (mediaType == null || MediaType.MULTIPART_MIXED.isCompatibleWith(mediaType))
            )
}
