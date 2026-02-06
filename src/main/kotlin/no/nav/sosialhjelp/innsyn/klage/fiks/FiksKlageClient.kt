package no.nav.sosialhjelp.innsyn.klage.fiks

import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

interface FiksKlageClient {
    suspend fun sendKlage(
        digisosId: UUID,
        klageId: UUID,
        vedtakId: UUID,
        files: MandatoryFilesForKlage,
    )

    suspend fun sendEttersendelse(
        digisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
        vedleggJson: JsonVedleggSpesifikasjon,
    )

    suspend fun hentKlager(digisosId: UUID?): List<FiksKlageDto>
}

@Component
class FiksKlageClientImpl(
    private val krypteringService: KrypteringService,
    private val dokumentlagerClient: DokumentlagerClient,
    private val fiksWebClient: WebClient,
) : FiksKlageClient {
    override suspend fun sendKlage(
        digisosId: UUID,
        klageId: UUID,
        vedtakId: UUID,
        files: MandatoryFilesForKlage,
    ) {
        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()

        withContext(Dispatchers.Default) {
            withTimeout(60.seconds) {
                krypteringService
                    .krypter(files.klagePdf.data, certificate, this@withContext)
                    .let { encrypted -> files.klagePdf.copy(data = encrypted) }
                    .let { pdfWithEncryptedData ->
                        createBodyForUpload(
                            klageJson = files.klageJson,
                            vedleggJson = files.vedleggJson,
                            klagePdf = pdfWithEncryptedData,
                        )
                    }
                    .also { body ->
                        doSendKlage(
                            digisosId = digisosId,
                            klageId = klageId,
                            vedtakId = vedtakId,
                            body = body,
                        )
                    }
            }
        }
    }

    private suspend fun doSendKlage(
        digisosId: UUID,
        klageId: UUID,
        vedtakId: UUID,
        body: MultiValueMap<String, HttpEntity<*>>,
    ) {
        val response =
            withContext(Dispatchers.IO) {
                fiksWebClient
                    .post()
                    .uri(SEND_INN_KLAGE_PATH, digisosId, klageId, klageId, vedtakId)
                    .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus({ !it.is2xxSuccessful }) { clientResponse ->
                        clientResponse.bodyToMono(String::class.java).map { errorBody ->
                            RuntimeException("Failed to send klage: ${clientResponse.statusCode()} - $errorBody")
                        }
                    }.toBodilessEntity()
                    .awaitSingleOrNull()
            }
        if (response?.statusCode?.is2xxSuccessful != true) {
            throw RuntimeException("Failed to send klage, status code: ${response?.statusCode}")
        }
    }

    override suspend fun sendEttersendelse(
        digisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
        vedleggJson: JsonVedleggSpesifikasjon,
    ) {
        MultipartBodyBuilder()
            .apply {
                buildPart(
                    "vedlegg.json",
                    "vedlegg.json",
                    MediaType.APPLICATION_JSON,
                    vedleggJson.toJson(),
                )
            }.build()
            .also { body -> doSendEttersendelse(digisosId, klageId, ettersendelseId, body) }
    }

    private suspend fun doSendEttersendelse(
        digisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
        body: MultiValueMap<String, HttpEntity<*>>,
    ) {
        val response =
            withContext(Dispatchers.IO) {
                fiksWebClient
                    .post()
                    .uri(ETTERSENDELSE_PATH, digisosId, ettersendelseId, klageId)
                    .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .onStatus({ !it.is2xxSuccessful }) { clientResponse ->
                        clientResponse.bodyToMono(String::class.java).map { errorBody ->
                            RuntimeException("Failed to send ettersendelse: ${clientResponse.statusCode()} - $errorBody")
                        }
                    }.toBodilessEntity()
                    .awaitSingleOrNull()
            }
        if (response?.statusCode?.is2xxSuccessful != true) {
            throw RuntimeException("Failed to send klage, status code: ${response?.statusCode}")
        }
    }

    // Uten query param vil alle klager for alle digisosIds for person returneres
    override suspend fun hentKlager(digisosId: UUID?): List<FiksKlageDto> {
        val uri = GET_KLAGER_PATH + if (digisosId != null) getKlagerQueryParam(digisosId) else ""
        return doHentKlager(uri)
    }

    private suspend fun doHentKlager(uri: String): List<FiksKlageDto> =
        fiksWebClient
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
            .retrieve()
            .bodyToMono(Array<FiksKlageDto>::class.java)
            .awaitSingleOrNull()
            ?.toList()
            ?: emptyList()

    companion object {
        private const val SEND_INN_KLAGE_PATH = "/digisos/klage/api/v1/{digisosId}/{navEksternRefId}/{klageId}/{vedtakId}"
        private const val GET_KLAGER_PATH = "/digisos/klage/api/v1/klager"
        private const val ETTERSENDELSE_PATH = "/digisos/klage/api/v1/{digisosId}/{navEksternRefId}/{klageId}/vedlegg"

        private fun getKlagerQueryParam(digisosId: UUID) = "?digisosId=$digisosId"
    }
}

private fun createBodyForUpload(
    klageJson: String,
    vedleggJson: JsonVedleggSpesifikasjon,
    klagePdf: FilForOpplasting,
): MultiValueMap<String, HttpEntity<*>> =
    MultipartBodyBuilder()
        .apply {
            buildPart(
                "klage.json",
                "klage.json",
                MediaType.APPLICATION_JSON,
                klageJson,
            )

            buildPart(
                "vedlegg.json",
                "vedlegg.json",
                MediaType.APPLICATION_JSON,
                vedleggJson,
            )

            createMetadataAndPartBodyForKlagePdf(klagePdf)
        }.build()

private fun MultipartBodyBuilder.createMetadataAndPartBodyForKlagePdf(klagePdf: FilForOpplasting) {
    buildPart(
        "metadata",
        null,
        MediaType.APPLICATION_JSON,
        sosialhjelpJsonMapper.writeValueAsString(klagePdf.createMetadataBody()),
    )

    buildPart(
        "klage.pdf",
        "klage.pdf",
        MediaType.APPLICATION_OCTET_STREAM,
        body = InputStreamResource(klagePdf.data),
    )
}

fun MultipartBodyBuilder.buildPart(
    name: String,
    filename: String?,
    contentType: MediaType,
    body: Any,
) {
    part(name, body).headers {
        it.contentType = contentType
        it.contentDisposition =
            ContentDisposition
                .builder("form-data")
                .name(name)
                .apply { if (filename != null) filename(filename) }
                .build()
    }
}

private fun FilForOpplasting.createMetadataBody() =
    FilMetadata(
        filnavn = filnavn?.value ?: error("Mangler filnavn for Metadata"),
        mimetype = mimetype ?: MediaType.APPLICATION_JSON_VALUE,
        storrelse = storrelse,
    )

data class MandatoryFilesForKlage(
    val klageJson: String,
    val vedleggJson: JsonVedleggSpesifikasjon,
    val klagePdf: FilForOpplasting,
)

private fun JsonVedleggSpesifikasjon.toJson(): String = sosialhjelpJsonMapper.writeValueAsString(this)
