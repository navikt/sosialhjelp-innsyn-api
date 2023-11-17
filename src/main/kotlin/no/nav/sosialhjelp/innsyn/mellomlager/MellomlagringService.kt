package no.nav.sosialhjelp.innsyn.mellomlager

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.coroutineScope
import no.nav.sosialhjelp.api.fiks.ErrorMessage
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.app.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.digisosapi.toHttpEntity
import no.nav.sosialhjelp.innsyn.klage.InputKlage
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import java.io.InputStream

const val MELLOMLAGRING_PATH = "abc"

@Component
class MellomlagringService(webClientBuilder: WebClient.Builder, private val maskinportenClient: MaskinportenClient) {

    val log by logger()

    val webClient = webClientBuilder.baseUrl("abc").build()

    suspend fun lastOppVedlegg(fiksDigisosId: String, file: FilOpplasting) {
        val multipartData = LinkedMultiValueMap<String, Any>()

        multipartData.add("metadata", objectMapper.writeValueAsString(file.metadata).toHttpEntity("metadata"))
        multipartData.add(file.metadata.filnavn, file.toHttpEntity(file.metadata.filnavn))
        try {
            webClient.post()
                .uri(MELLOMLAGRING_PATH, fiksDigisosId)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(multipartData))
                .header(HttpHeaders.AUTHORIZATION, BEARER + maskinportenClient.getToken())
                .retrieve()
                .awaitBody<String>()
        } catch (e: Exception) {
            log.warn("Noe feilet under mellomlagring av vedlegg", e)
        }
    }

    fun lastOppKlage(fiksDigisosId: String, klage: InputKlage) {
//        val serializedKlage = objectMapper.writeValueAsString(klage).toHttpEntity("klage.json")
//
//        val multipartData = LinkedMultiValueMap<String, Any>()
//        multipartData.add("metadata", )
//        webClient.post()
//            .uri(MELLOMLAGRING_PATH, fiksDigisosId)
//            .accept(MediaType.APPLICATION_JSON)
//            .body(BodyInserters.fromMultipartData("m", ""))
//            .header(HttpHeaders.AUTHORIZATION, BEARER + maskinportenClient.getToken())
//            .retrieve()
//            .bodyToMono<String>()
//            .block() ?: throw FiksException("MellomlagringDto er null?", null)
    }

    fun getMellomLagredeVedlegg(fiksDigisosId: String): List<InternalVedlegg> {
        val responseString: String
        try {
            responseString = webClient.get()
                .uri(MELLOMLAGRING_PATH, fiksDigisosId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, BEARER + maskinportenClient.getToken())
                .retrieve()
                .bodyToMono<String>()
                .block() ?: throw FiksException("MellomlagringDto er null?", null)
        } catch (e: WebClientResponseException) {
            if (e is WebClientResponseException.BadRequest) {
                val errorMessage = objectMapper.readValue<ErrorMessage>(e.responseBodyAsString)
                val message = errorMessage.message
                if (message != null && message.contains("Fant ingen data i basen knytter til angitt id'en")) {
                    return emptyList()
                }
            }
            log.warn("Fiks - getMellomlagredeVedlegg feilet - ${e.responseBodyAsString}", e)
            throw e
        }
        return objectMapper.readValue(responseString)
    }
}

data class FilOpplasting(
    val metadata: FilMetadata,
    val data: InputStream,
)

data class FilMetadata(
    val filnavn: String,
    val mimetype: String,
    val storrelse: Long,
)
