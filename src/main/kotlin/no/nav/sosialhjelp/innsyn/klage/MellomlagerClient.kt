package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import org.apache.commons.io.IOUtils
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

interface MellomlagerClient {
    fun getDocumentMetadataForRef(navEksternId: String): MellomlagringDto?

    suspend fun uploadDocuments(
        navEksternId: UUID,
        filerForOpplasting: List<FilForOpplasting>,
    ): MellomlagringDto

    fun deleteAllDocumentsForRef(navEksternId: String)

    fun getDocument(
        navEksternId: String,
        digisosDokumentId: String,
    ): ByteArray

    fun deleteDocument(
        navEksternId: String,
        digisosDokumentId: String,
    )
}

@Profile("!local")
@Component
class FiksMellomlagerClient(
    private val mellomlagerWebClient: WebClient,
    private val texasClient: TexasClient,
    private val krypteringService: KrypteringService,
    private val dokumentlagerClient: DokumentlagerClient,
): MellomlagerClient {


    override fun getDocumentMetadataForRef(navEksternId: String): MellomlagringDto? {
        TODO("Not yet implemented")
    }

    override suspend fun uploadDocuments(
        navEksternId: UUID,
        filerForOpplasting: List<FilForOpplasting>
    ): MellomlagringDto {

        val body = createBodyForUpload(krypterFiler(filerForOpplasting))

        return mellomlagerWebClient.post()
            .uri(MELLOMLAGRING_PATH, navEksternId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${getMaskinportenToken()}")
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .bodyToMono<MellomlagringDto>()
            .doOnError(WebClientResponseException::class.java) {
                logger.error(
                    "Feil ved opplasting av dokumenter til mellomlager for navEksternId: $navEksternId",
                    it
                )
            }
            .block()
            ?: throw FiksClientException(
                500,
                "MellomlagringDto er null ved opplasting av dokumenter: $navEksternId",
                null
            )
    }



    override fun deleteAllDocumentsForRef(navEksternId: String) {
        TODO("Not yet implemented")
    }

    override fun getDocument(navEksternId: String, digisosDokumentId: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deleteDocument(navEksternId: String, digisosDokumentId: String) {
        TODO("Not yet implemented")
    }

    private suspend fun krypterFiler(filerForOpplasting: List<FilForOpplasting>): List<FilForOpplasting> {

        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()

        return withContext(Dispatchers.Default) {
            filerForOpplasting.map { fil ->
                fil.copy(data = krypteringService.krypter(fil.data, certificate, this))
            }
        }
    }

    private suspend fun getMaskinportenToken() = texasClient.getMaskinportenToken()

    companion object {
        private const val MELLOMLAGRING_PATH = "digisos/api/v1/mellomlagring/{navEksternRefId}"
        private const val MELLOMLAGRING_DOKUMENT_PATH = "digisos/api/v1/mellomlagring/{navEksternRefId}/{digisosDokumentId}"

        private val logger by logger()
    }

}

// TODO Enkel implementasjon for lokal testing - fjernes når mock-alt-api er klart
@Profile("local")
@Component
class LocalMellomlagerClient : MellomlagerClient {
    override fun getDocumentMetadataForRef(navEksternId: String): MellomlagringDto? {
        TODO("Not yet implemented")
    }

    override suspend fun uploadDocuments(
        navEksternId: UUID,
        filerForOpplasting: List<FilForOpplasting>
    ): MellomlagringDto {
        TODO("Not yet implemented")
    }

    override fun deleteAllDocumentsForRef(navEksternId: String) {
        TODO("Not yet implemented")
    }

    override fun getDocument(navEksternId: String, digisosDokumentId: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deleteDocument(navEksternId: String, digisosDokumentId: String) {
        TODO("Not yet implemented")
    }


    companion object {
        val mellomlager = mutableMapOf<UUID, List<FilForOpplasting>>()
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

data class MellomlagringDto(
    val navEksternRefId: UUID,
    val mellomlagringMetadataList: List<MellomlagringDokumentInfo>,
)

data class MellomlagringDokumentInfo(
    val filnavn: String,
    val filId: UUID,
    val storrelse: Long,
    val mimetype: String,
)

private fun createHttpEntityOfString(
    body: String,
    name: String,
): HttpEntity<Any> {
    return createHttpEntity(body, name, null, "text/plain;charset=UTF-8")
}

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

private fun createJsonFilMetadata(objectFilForOpplasting: FilForOpplasting): String {
    return try {
        jacksonObjectMapper().writeValueAsString(
            FilMetadata(
                filnavn = objectFilForOpplasting.filnavn?.value ?: error("Filnavn mangler"),
                mimetype = objectFilForOpplasting.mimetype ?: "application/octet-stream",
                storrelse = objectFilForOpplasting.storrelse,
            )
        )
    } catch (e: JsonProcessingException) {
        throw IllegalStateException(e)
    }
}

private fun createHttpEntityOfFile(
    file: FilForOpplasting,
    name: String,
): HttpEntity<Any> {
    return createHttpEntity(
        body = ByteArrayResource(IOUtils.toByteArray(file.data)),
        name = name,
        filename = file.filnavn?.value,
        contentType = "application/octet-stream"
    )
}

private fun createBodyForUpload(filerForOpplasting: List<FilForOpplasting>): LinkedMultiValueMap<String, Any> {
    val body = LinkedMultiValueMap<String, Any>()
    filerForOpplasting.forEachIndexed { index, fil ->
        body.add(
            "metadata$index",
            createHttpEntityOfString(createJsonFilMetadata(fil),"metadata$index")
        )
        body.add(
            fil.filnavn?.value ?: error("Filnavn mangler"),
            createHttpEntityOfFile(fil, fil.filnavn.value)
        )
    }

    return body
}
