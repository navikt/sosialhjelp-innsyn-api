package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.Ettersendelse
import no.nav.sosialhjelp.innsyn.app.exceptions.NedlastingFilnavnMismatchException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime

const val LASTET_OPP_STATUS = "LastetOpp"
const val VEDLEGG_KREVES_STATUS = "VedleggKreves"

@Component
class VedleggService(
    private val fiksService: FiksService,
) {
    suspend fun hentAlleOpplastedeVedlegg(
        digisosSak: DigisosSak,
        model: InternalDigisosSoker,
    ): List<InternalVedlegg> {
        val soknadVedlegg = hentSoknadVedleggMedStatus(LASTET_OPP_STATUS, digisosSak)
        val ettersendteVedlegg = hentEttersendteVedlegg(digisosSak, model)

        return soknadVedlegg.plus(ettersendteVedlegg)
    }

    suspend fun hentSoknadVedleggMedStatus(
        status: String,
        digisosSak: DigisosSak,
    ): List<InternalVedlegg> {
        val originalSoknadNAV = digisosSak.originalSoknadNAV ?: return emptyList()
        val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(digisosSak, originalSoknadNAV.vedleggMetadata)

        if (jsonVedleggSpesifikasjon.vedlegg.isEmpty()) {
            return emptyList()
        }

        val alleVedlegg =
            jsonVedleggSpesifikasjon.vedlegg
                .filter { vedlegg -> vedlegg.status == status }
                .map { vedlegg ->
                    InternalVedlegg(
                        vedlegg.type,
                        vedlegg.tilleggsinfo,
                        vedlegg.hendelseType,
                        vedlegg.hendelseReferanse,
                        matchDokumentInfoAndJsonFiler(originalSoknadNAV.vedlegg, vedlegg.filer).toMutableList(),
                        unixToLocalDateTime(originalSoknadNAV.timestampSendt),
                        null,
                    )
                }
        return kombinerAlleLikeVedlegg(alleVedlegg)
    }

    suspend fun hentEttersendteVedlegg(
        fiksDigisosId: String,
        hendelseReferanse: String,
    ): List<InternalVedlegg> {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val ettersendteVedlegg =
            digisosSak.ettersendtInfoNAV
                ?.ettersendelser
                ?.hentVedleggSpesifikasjon(digisosSak)
                ?.flatMap { (ettersendelse, jsonVedleggSpesifikasjon) ->
                    jsonVedleggSpesifikasjon.vedlegg
                        .filter { it.hendelseReferanse == hendelseReferanse && LASTET_OPP_STATUS == it.status }
                        .map { vedlegg ->
                            InternalVedlegg(
                                vedlegg.type,
                                vedlegg.tilleggsinfo,
                                vedlegg.hendelseType,
                                vedlegg.hendelseReferanse,
                                vedlegg.filer.map { fil -> ettersendelse.vedlegg.find { it.filnavn == fil.filnavn }!! }.toMutableList(),
                                unixToLocalDateTime(ettersendelse.timestampSendt),
                                null,
                            )
                        }
                } ?: emptyList()

        return kombinerAlleLikeVedlegg(ettersendteVedlegg)
    }

    private suspend fun List<Ettersendelse>.hentVedleggSpesifikasjon(digisosSak: DigisosSak) =
        coroutineScope {
            this@hentVedleggSpesifikasjon
                .associateWith {
                    async {
                        hentVedleggSpesifikasjon(digisosSak, it.vedleggMetadata)
                    }
                }.mapValues { (_, value) -> value.await() }
        }

    suspend fun hentEttersendteVedlegg(
        digisosSak: DigisosSak,
        model: InternalDigisosSoker,
    ): List<InternalVedlegg> {
        val alleVedlegg =
            digisosSak.ettersendtInfoNAV
                ?.ettersendelser
                ?.hentVedleggSpesifikasjon(digisosSak)
                ?.flatMap { (ettersendelse, jsonVedleggSpesifikasjon) ->
                    var filIndex = 0
                    jsonVedleggSpesifikasjon.vedlegg
                        .filter { vedlegg -> LASTET_OPP_STATUS == vedlegg.status }
                        .map { vedlegg ->
                            val currentFilIndex = filIndex
                            filIndex += vedlegg.filer.size
                            val filtrerteEttersendelsesVedlegg =
                                ettersendelse.vedlegg
                                    .filter { ettersendelseVedlegg -> ettersendelseVedlegg.filnavn != "ettersendelse.pdf" }
                            val dokumentInfoList: MutableList<DokumentInfo>
                            if (filIndex > filtrerteEttersendelsesVedlegg.size) {
                                log.error(
                                    "Det er mismatch mellom nedlastede filer og metadata. " +
                                        "Det er flere filer enn vi har Metadata! " +
                                        "Filer: $filIndex Metadata: ${filtrerteEttersendelsesVedlegg.size}",
                                )
                                dokumentInfoList = vedlegg.filer.map { DokumentInfo(it.filnavn, "Error", -1) }.toMutableList()
                            } else {
                                dokumentInfoList = filtrerteEttersendelsesVedlegg.subList(currentFilIndex, filIndex).toMutableList()

                                if (!filenamesMatchInDokumentInfoAndFiles(dokumentInfoList, vedlegg.filer)) {
                                    val exception =
                                        NedlastingFilnavnMismatchException("Det er mismatch mellom nedlastede filer og metadata", null)
                                    log.error("Mismatch", exception)
                                }
                            }
                            InternalVedlegg(
                                vedlegg.type,
                                vedlegg.tilleggsinfo,
                                vedlegg.hendelseType,
                                vedlegg.hendelseReferanse,
                                dokumentInfoList,
                                unixToLocalDateTime(ettersendelse.timestampSendt),
                                hentInnsendelsesfristFraOppgave(model, vedlegg),
                            )
                        }
                } ?: emptyList()

        return kombinerAlleLikeVedlegg(alleVedlegg)
    }

    private suspend fun hentVedleggSpesifikasjon(
        digisosSak: DigisosSak,
        dokumentlagerId: String,
    ): JsonVedleggSpesifikasjon = fiksService.getDocument(digisosSak.fiksDigisosId, dokumentlagerId, JsonVedleggSpesifikasjon::class.java)

    private fun matchDokumentInfoAndJsonFiler(
        dokumentInfoList: List<DokumentInfo>,
        jsonFiler: List<JsonFiler>,
    ): List<DokumentInfo> =
        jsonFiler
            .flatMap { fil ->
                dokumentInfoList
                    .filter { it.filnavn == fil.filnavn }
            }

    private fun filenamesMatchInDokumentInfoAndFiles(
        dokumentInfoList: List<DokumentInfo>,
        files: List<JsonFiler>,
    ): Boolean =
        dokumentInfoList.size == files.size &&
            dokumentInfoList
                .filterIndexed { idx, it ->
                    sanitizeFileName(it.filnavn) == sanitizeFileName(files[idx].filnavn)
                }.size == dokumentInfoList.size

    private fun hentInnsendelsesfristFraOppgave(
        model: InternalDigisosSoker,
        vedlegg: JsonVedlegg,
    ): LocalDateTime? =
        model.oppgaver
            .sortedByDescending { it.innsendelsesfrist }
            .firstOrNull { it.tittel == vedlegg.type && it.tilleggsinfo == vedlegg.tilleggsinfo }
            ?.innsendelsesfrist

    companion object {
        private val log by logger()
    }
}

data class InternalVedlegg(
    val type: String,
    val tilleggsinfo: String?,
    val hendelseType: JsonVedlegg.HendelseType?,
    val hendelseReferanse: String?,
    val dokumentInfoList: MutableList<DokumentInfo>,
    val tidspunktLastetOpp: LocalDateTime,
    val innsendelsesfrist: LocalDateTime?,
)
