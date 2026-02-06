package no.nav.sosialhjelp.innsyn.klage

import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.klage.fiks.DokumentInfoDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksEttersendelseDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageDto
import no.nav.sosialhjelp.innsyn.klage.fiks.MandatoryFilesForKlage
import no.nav.sosialhjelp.innsyn.klage.fiks.MellomlagerService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import tools.jackson.module.kotlin.jacksonObjectMapper

interface KlageService {
    suspend fun sendKlage(
        jsonKlage: JsonKlage,
        klagePdf: FilForOpplasting,
    )

    suspend fun hentKlager(fiksDigisosId: UUID): List<KlageRef>

    suspend fun hentKlage(
        fiksDigisosId: UUID,
        klageId: UUID,
    ): KlageDto?

    suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        navEksternRefId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentsForKlage

    suspend fun sendEttersendelse(
        fiksDigisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
    )
}

@Service
class KlageServiceImpl(
    private val klageClient: FiksKlageClient,
    private val mellomlagerService: MellomlagerService,
    private val clientProperties: ClientProperties,
    private val krypteringService: KrypteringService,
) : KlageService {
    override suspend fun sendKlage(
        jsonKlage: JsonKlage,
        klagePdf: FilForOpplasting,
    ) {
        klageClient.sendKlage(
            digisosId = UUID.fromString(jsonKlage.digisosId),
            klageId = UUID.fromString(jsonKlage.klageId),
            vedtakId = UUID.fromString(jsonKlage.vedtakId),
            MandatoryFilesForKlage(
                klageJson = jacksonObjectMapper().writeValueAsString(jsonKlage),
                klagePdf = klagePdf.encryptData(),
                vedleggJson = createJsonVedleggSpec(UUID.fromString(jsonKlage.klageId)),
            ),
        )
    }

    override suspend fun sendEttersendelse(
        fiksDigisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
    ) {
        createJsonVedleggSpec(ettersendelseId, klageId)
            .also {
                if (it.noFiles()) error("Ingen vedlegg for ettersendelse av Klage")

                klageClient.sendEttersendelse(
                    digisosId = fiksDigisosId,
                    klageId = klageId,
                    ettersendelseId = ettersendelseId,
                    vedleggJson = it,
                )
            }
    }

    override suspend fun hentKlager(fiksDigisosId: UUID): List<KlageRef> =
        klageClient.hentKlager(fiksDigisosId).map {
            KlageRef(it.klageId, it.vedtakId)
        }

    override suspend fun hentKlage(
        fiksDigisosId: UUID,
        klageId: UUID,
    ): KlageDto? {
        val fiksKlage =
            klageClient.hentKlager(digisosId = fiksDigisosId).find { it.klageId == klageId }
                ?: return null

        val klagePdf = fiksKlage.klageDokument.toVedleggResponse(fiksKlage.getTidspunktSendt())
        val opplastedeVedlegg = fiksKlage.vedlegg.map { it.toVedleggResponse(fiksKlage.getTidspunktSendt()) }

        return KlageDto(
            digisosId = fiksDigisosId,
            klageId = fiksKlage.klageId,
            vedtakId = fiksKlage.vedtakId,
            klagePdf = klagePdf,
            opplastedeVedlegg = opplastedeVedlegg,
            ettersendelser = fiksKlage.ettersendtInfoNAV?.ettersendelser?.map { it.toEttersendelseDto() } ?: emptyList(),
            timestampSendt = fiksKlage.sendtKvittering.sendtStatus.timestamp,
        )
    }

    override suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        navEksternRefId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentsForKlage {
        val allFiles = rawFiles.asFlow().toList()

        return mellomlagerService.processDocumentUpload(navEksternRefId, allFiles)
    }

    private suspend fun FilForOpplasting.encryptData(): FilForOpplasting =
        withContext(Dispatchers.Default) {
            withTimeout(60.seconds) {
                krypteringService
                    .krypter(data, this@withContext)
                    .let { encrypted -> copy(data = encrypted) }
            }
        }

    private suspend fun createJsonVedleggSpec(
        navEksternRefId: UUID,
        klageId: UUID = navEksternRefId,
    ): JsonVedleggSpesifikasjon {
        val allMetadata =
            runCatching { mellomlagerService.getAllDocumentMetadataForRef(navEksternRefId) }
                .getOrElse { ex ->
                    when (ex) {
                        is NotFoundException -> emptyList()
                        else -> throw ex
                    }
                }

        // TODO Hva forventes her i kontekst av klage?
        return JsonVedlegg()
            .withType(resolveType(navEksternRefId, klageId))
            .withStatus(if (allMetadata.isNotEmpty()) "LASTET_OPP" else "INGEN_VEDLEGG")
            .withHendelseType(JsonVedlegg.HendelseType.BRUKER)
            .withHendelseReferanse(navEksternRefId.toString())
            .withKlageId(klageId.toString())
            .withFiler(allMetadata.map { JsonFiler().withFilnavn(it.filnavn) })
            .let { JsonVedleggSpesifikasjon().withVedlegg(listOf(it)) }
    }

    private fun resolveType(
        navEksternRefId: UUID,
        klageId: UUID,
    ): String = if (navEksternRefId == klageId) "klage" else "klage_ettersendelse"

    private fun DokumentInfoDto.toVedleggResponse(tidspunktSendt: LocalDateTime) =
        VedleggResponse(
            filnavn = filnavn,
            storrelse = storrelse,
            url = hentDokumentlagerUrl(clientProperties, dokumentlagerDokumentId.toString()),
            type = "klage_pdf",
            tilleggsinfo = null,
            datoLagtTil = tidspunktSendt,
        )

    private fun FiksEttersendelseDto.toEttersendelseDto() =
        EttersendelseDto(
            navEksternRefId = navEksternRefId,
            vedlegg = vedlegg.map { it.toVedleggResponse(unixToLocalDateTime(timestampSendt)) },
            timestampSendt = timestampSendt,
        )
}

private fun JsonVedleggSpesifikasjon.noFiles(): Boolean = vedlegg.flatMap { it.filer }.isEmpty()

private fun FiksKlageDto.getTidspunktSendt() = sendtKvittering.sendtStatus.timestamp.let { unixToLocalDateTime(it) }
