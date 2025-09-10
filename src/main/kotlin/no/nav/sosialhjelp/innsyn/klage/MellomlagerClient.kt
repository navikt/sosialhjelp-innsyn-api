package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.utils.logger
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
import java.security.cert.X509Certificate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

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
        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()

        return withContext(Dispatchers.Default) {
            withTimeout(10.seconds) {
                val krypterteFiler = krypterFiler(certificate, filerForOpplasting, this)
                logger.info("*** DONE ENCRYPTING FILES, UPLOADING TO MELLOMLAGER")

                logger.info("*** CREATE BODY FOR UPLOAD -> NavEksternRef: $navEksternId")

                withContext(Dispatchers.IO) {
                    val body = createBodyForUpload(krypterteFiler)

                    runCatching {
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
            }
        }
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

    private suspend fun krypterFiler(
        certificate: X509Certificate,
        filerForOpplasting: List<FilForOpplasting>,
        coroutineScope: CoroutineScope,
    ): List<FilForOpplasting> =
        filerForOpplasting.map { fil ->
            fil.copy(data = krypteringService.krypter(fil.data, certificate, coroutineScope))
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
        private val logger by logger()

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
    val contentDisposition =
        ContentDisposition
            .builder("form-data")
            .run {
                name(name)
                if (filename != null) filename(filename)
                build()
            }

    return LinkedMultiValueMap<String, String>()
        .apply {
            add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            add(HttpHeaders.CONTENT_TYPE, contentType)
        }.let { headerMap -> HttpEntity(body, headerMap) }
}

private fun createJsonFilMetadata(objectFilForOpplasting: FilForOpplasting): String =
    jacksonObjectMapper().writeValueAsString(
        FilMetadata(
            filnavn = objectFilForOpplasting.filnavn?.value ?: error("Filnavn mangler"),
            mimetype = objectFilForOpplasting.mimetype ?: "application/octet-stream",
            storrelse = objectFilForOpplasting.storrelse,
        ),
    )

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

private fun createBodyForUpload(filerForOpplasting: List<FilForOpplasting>): LinkedMultiValueMap<String, Any> =
    LinkedMultiValueMap<String, Any>()
        .apply {
            filerForOpplasting.forEachIndexed { index, fil ->
                add(
                    "metadata$index",
                    createHttpEntityOfString(createJsonFilMetadata(fil), "metadata$index"),
                )
                add(
                    fil.filnavn?.value ?: error("Filnavn mangler"),
                    createHttpEntityOfFile(fil, fil.filnavn.value),
                )
            }
        }

private fun String.toFiksError() = objectMapper.readValue<MellomlagerResponse.FiksError>(this)

sealed interface MellomlagerResponse {
    data class MellomlagringDto(
        val navEksternRefId: UUID,
        val mellomlagringMetadataList: List<MellomlagringDokumentInfo>,
    ) : MellomlagerResponse

    data class FiksError(
        val timestamp: Long?,
        val status: Int?,
        val error: String?,
        val errorId: UUID?,
        val path: String?,
        val originalPath: String?,
        val message: String?,
        val errorCode: String?,
        val errorJson: String?,
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
