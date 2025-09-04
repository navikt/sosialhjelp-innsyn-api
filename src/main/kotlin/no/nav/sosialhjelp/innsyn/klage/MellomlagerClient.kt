package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import org.apache.commons.io.IOUtils
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

interface MellomlagerClient {
    suspend fun getDocumentMetadataForRef(navEksternId: UUID): MellomlagerResponse

    suspend fun getDocument(
        navEksternId: UUID,
        digisosDokumentId: UUID,
    ): MellomlagerResponse

    suspend fun uploadDocuments(
        navEksternId: UUID,
        filerForOpplasting: List<FilForOpplasting>,
    ): MellomlagerResponse

    suspend fun deleteAllDocumentsForRef(navEksternId: UUID): MellomlagerResponse

    suspend fun deleteDocument(
        navEksternId: UUID,
        digisosDokumentId: UUID,
    ): MellomlagerResponse
}

@Profile("!local")
@Component
class FiksMellomlagerClient(
    private val mellomlagerWebClient: WebClient,
    private val texasClient: TexasClient,
    private val krypteringService: KrypteringService,
    private val dokumentlagerClient: DokumentlagerClient,
) : MellomlagerClient {
    override suspend fun getDocumentMetadataForRef(navEksternId: UUID): MellomlagerResponse =
        runCatching {
            mellomlagerWebClient
                .get()
                .uri(MELLOMLAGRING_PATH, navEksternId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${getMaskinportenToken()}")
                .retrieve()
                .bodyToMono<MellomlagerResponse.MellomlagringDto>()
                .block()
                ?: error("MellomlagringDto er null")
        }.getOrElse { ex -> handleClientError(ex, "get metadata") }

    override suspend fun uploadDocuments(
        navEksternId: UUID,
        filerForOpplasting: List<FilForOpplasting>,
    ): MellomlagerResponse {
        val body = createBodyForUpload(krypterFiler(filerForOpplasting))

        return runCatching {
            mellomlagerWebClient
                .post()
                .uri(MELLOMLAGRING_PATH, navEksternId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${getMaskinportenToken()}")
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono<MellomlagerResponse.MellomlagringDto>()
                .block()
                ?: error("MellomlagringDto is null")
        }.getOrElse { ex -> handleClientError(ex, "upload documents") }
    }

    override suspend fun getDocument(
        navEksternId: UUID,
        digisosDokumentId: UUID,
    ): MellomlagerResponse =
        runCatching {
            mellomlagerWebClient
                .get()
                .uri(MELLOMLAGRING_DOKUMENT_PATH, navEksternId, digisosDokumentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${getMaskinportenToken()}")
                .retrieve()
                .bodyToMono<ByteArray>()
                .block()
                ?.let { data -> MellomlagerResponse.ByteArrayResponse(data) }
                ?: error("Document data is null")
        }.getOrElse { ex -> handleClientError(ex, "get document") }

    override suspend fun deleteAllDocumentsForRef(navEksternId: UUID): MellomlagerResponse =
        runCatching {
            mellomlagerWebClient
                .delete()
                .uri(MELLOMLAGRING_PATH, navEksternId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${getMaskinportenToken()}")
                .retrieve()
                .toBodilessEntity()
                .block()
                .let { MellomlagerResponse.EmptyResponse }
        }.getOrElse { ex -> handleClientError(ex, "delete all documents") }

    override suspend fun deleteDocument(
        navEksternId: UUID,
        digisosDokumentId: UUID,
    ): MellomlagerResponse =
        runCatching {
            mellomlagerWebClient
                .delete()
                .uri(MELLOMLAGRING_DOKUMENT_PATH, navEksternId, digisosDokumentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${getMaskinportenToken()}")
                .retrieve()
                .toBodilessEntity()
                .block()
                .let { MellomlagerResponse.EmptyResponse }
        }.getOrElse { ex -> handleClientError(ex, "delete document") }

    private suspend fun krypterFiler(filerForOpplasting: List<FilForOpplasting>): List<FilForOpplasting> {
        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()

        return withContext(Dispatchers.Default) {
            filerForOpplasting.map { fil ->
                fil.copy(data = krypteringService.krypter(fil.data, certificate, this))
            }
        }
    }

    private fun handleClientError(
        exception: Throwable,
        context: String? = null,
    ): MellomlagerResponse.FiksError =
        when (exception) {
            is WebClientResponseException -> exception.responseBodyAsString.toFiksError()
            else -> throw MellomlagerClientException("Unexpected error in $context", exception)
        }

    private suspend fun getMaskinportenToken() = texasClient.getMaskinportenToken().value

    companion object {
        private const val MELLOMLAGRING_PATH = "digisos/api/v1/mellomlagring/{navEksternRefId}"
        private const val MELLOMLAGRING_DOKUMENT_PATH = "digisos/api/v1/mellomlagring/{navEksternRefId}/{digisosDokumentId}"
    }
}

data class MellomlagerClientException(
    override val message: String,
    override val cause: Throwable,
) : RuntimeException(message, cause)

data class FilMetadata(
    val filnavn: String,
    val mimetype: String,
    val storrelse: Long,
)

private fun createHttpEntityOfString(
    body: String,
    name: String,
): HttpEntity<Any> = createHttpEntity(body, name, null, "text/plain;charset=UTF-8")

fun createHttpEntity(
    body: Any,
    name: String,
    filename: String?,
    contentType: String,
): HttpEntity<Any> {
    val headerMap = LinkedMultiValueMap<String, String>()
    val builder: ContentDisposition.Builder =
        ContentDisposition
            .builder("form-data")
            .name(name)
    val contentDisposition: ContentDisposition =
        if (filename == null) builder.build() else builder.filename(filename).build()

    headerMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
    headerMap.add(HttpHeaders.CONTENT_TYPE, contentType)
    return HttpEntity(body, headerMap)
}

private fun createJsonFilMetadata(objectFilForOpplasting: FilForOpplasting): String =
    try {
        jacksonObjectMapper().writeValueAsString(
            FilMetadata(
                filnavn = objectFilForOpplasting.filnavn?.value ?: error("Filnavn mangler"),
                mimetype = objectFilForOpplasting.mimetype ?: "application/octet-stream",
                storrelse = objectFilForOpplasting.storrelse,
            ),
        )
    } catch (e: JsonProcessingException) {
        throw IllegalStateException(e)
    }

private fun createHttpEntityOfFile(
    file: FilForOpplasting,
    name: String,
): HttpEntity<Any> =
    createHttpEntity(
        body = ByteArrayResource(IOUtils.toByteArray(file.data)),
        name = name,
        filename = file.filnavn?.value,
        contentType = "application/octet-stream",
    )

private fun createBodyForUpload(filerForOpplasting: List<FilForOpplasting>): LinkedMultiValueMap<String, Any> {
    val body = LinkedMultiValueMap<String, Any>()
    filerForOpplasting.forEachIndexed { index, fil ->
        body.add(
            "metadata$index",
            createHttpEntityOfString(createJsonFilMetadata(fil), "metadata$index"),
        )
        body.add(
            fil.filnavn?.value ?: error("Filnavn mangler"),
            createHttpEntityOfFile(fil, fil.filnavn.value),
        )
    }

    return body
}

private fun String.toFiksError() = objectMapper.readValue<MellomlagerResponse.FiksError>(this)

sealed interface MellomlagerResponse {
    data class MellomlagringDto(
        val navEksternRefId: UUID,
        val mellomlagringMetadataList: List<MellomlagringDokumentInfo>,
    ) : MellomlagerResponse

    data class FiksError(
        val timestamp: Int,
        val status: Int,
        val error: String,
        val errorId: UUID,
        val path: String,
        val originalPath: String,
        val message: String,
        val errorCode: String,
        val errorJson: String,
    ) : MellomlagerResponse

    class ByteArrayResponse(
        val data: ByteArray,
    ) : MellomlagerResponse

    object EmptyResponse : MellomlagerResponse
}

data class MellomlagringDokumentInfo(
    val filnavn: String,
    val filId: UUID,
    val storrelse: Long,
    val mimetype: String,
)
