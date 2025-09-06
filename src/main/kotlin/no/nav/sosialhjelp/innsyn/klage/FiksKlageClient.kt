package no.nav.sosialhjelp.innsyn.klage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.toHttpEntity
import no.nav.sosialhjelp.innsyn.utils.objectMapper
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
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

interface FiksKlageClient {
    suspend fun sendKlage(
        digisosId: UUID,
        klageId: UUID,
        vedtakId: UUID,
        files: MandatoryFilesForKlage,
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
        val body =
            createBodyForUpload(
                klageJson = files.klageJson,
                vedleggJson = files.vedleggJson,
                klagePdf = files.klagePdf,
//                klagePdf = files.klagePdf.encryptFiles(),
            )

        withContext(Dispatchers.IO) {
            doSendKlage(
                digisosId = digisosId,
                klageId = klageId,
                vedtakId = vedtakId,
                body = body,
            )
        }
    }

    private suspend fun doSendKlage(
        digisosId: UUID,
        klageId: UUID,
        vedtakId: UUID,
        body: MultiValueMap<String, HttpEntity<*>>,
    ) {
        val response =
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

        if (response?.statusCode?.is2xxSuccessful != true) {
            throw RuntimeException("Failed to send klage, status code: ${response?.statusCode}")
        }
    }

    // Uten query param vil det alle klager for alle digisosIds returneres
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
            .block()
            ?.toList()
            ?: emptyList()

    private suspend fun FilForOpplasting.encryptFiles(): FilForOpplasting {
        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()

        return withContext(Dispatchers.Default) {
            withTimeout(60.seconds) {
                krypteringService.krypter(data, certificate, this)
            }
        }.let { encryptedData -> this.copy(data = encryptedData) }
    }

    companion object {
        private const val SEND_INN_KLAGE_PATH = "/digisos/klage/api/v1/{digisosId}/{navEksternRefId}/{klageId}/{vedtakId}"
        private const val GET_KLAGER_PATH = "/digisos/klage/api/v1/klager"

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
            part("klage.json", klageJson.toHttpEntity("klage.json"))
            part("vedlegg.json", vedleggJson.toJson().toHttpEntity("vedlegg.json"))
            part("klage.pdf", InputStreamResource(klagePdf.data))
                .headers {
                    it.contentType = MediaType.APPLICATION_OCTET_STREAM
                    it.contentDisposition =
                        ContentDisposition
                            .builder("form-data")
                            .name("klage.pdf")
                            .filename(klagePdf.filnavn?.value)
                            .build()
                }
        }.build()

data class MandatoryFilesForKlage(
    val klageJson: String,
    val vedleggJson: JsonVedleggSpesifikasjon,
    val klagePdf: FilForOpplasting,
)

data class FiksKlageDto(
    val fiksOrgId: UUID,
    val klageId: UUID,
    val vedtakId: UUID,
    val navEksternRefId: UUID,
    val klageMetadata: UUID, // id til klage.json i dokumentlager
    val vedleggMetadata: UUID, // id til vedlegg.json (jsonVedleggSpec) i dokumentlager
    val klageDokument: DokumentInfoDto, // id til klage.pdf i dokumentlager
    val trekkKlageInfo: TrekkKlageInfoDto?,
    val sendtKvittering: SendtKvitteringDto,
    val ettersendtInfoNAV: EttersendtInfoNAVDto,
    val trukket: Boolean,
)

data class EttersendtInfoNAVDto(
    val ettersendelser: List<EttersendelseDto>,
)

data class EttersendelseDto(
    val navEksternRefId: UUID,
    val vedleggMetadata: UUID,
    val vedlegg: List<DokumentInfoDto>,
    val timestampSendt: Long,
)

data class DokumentInfoDto(
    val filnavn: String,
    val dokumentlagerDokumentId: UUID,
    val storrelse: Long,
)

data class TrekkKlageInfoDto(
    val navEksternRefId: UUID,
    val trekkPdfMetadata: UUID,
    val vedleggMetadata: UUID,
    val trekkKlageDokument: DokumentInfoDto,
    val vedlegg: List<DokumentInfoDto>,
    val sendtKvittering: SendtKvitteringDto,
)

data class SendtKvitteringDto(
    val sendtKanal: FiksProtokoll,
    val meldingId: UUID,
    val sendtStatus: SendtStatusDto,
    val statusListe: List<SendtStatusDto>,
)

data class SendtStatusDto(
    val status: SendtStatus,
    val timestamp: Long,
)

enum class SendtStatus {
    SENDT,
    BEKREFTET,
    TTL_TIDSAVBRUDD,
    MAX_RETRIESAVBRUDD,
    IKKE_SENDT,
    SVARUT_FEIL,
    STOPPET,
}

enum class FiksProtokoll {
    FIKS_IO,
    SVARUT,
}

private fun JsonVedleggSpesifikasjon.toJson(): String = objectMapper.writeValueAsString(this)
