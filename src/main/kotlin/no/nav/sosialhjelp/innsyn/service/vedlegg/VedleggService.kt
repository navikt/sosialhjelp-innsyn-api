package no.nav.sosialhjelp.innsyn.service.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.common.NedlastingFilnavnMismatchException
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime

const val LASTET_OPP_STATUS = "LastetOpp"
const val VEDLEGG_KREVES_STATUS = "VedleggKreves"

@Component
class VedleggService(
    private val fiksClient: FiksClient
) {

    fun hentAlleOpplastedeVedlegg(fiksDigisosId: String, token: String): List<InternalVedlegg> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)

        val soknadVedlegg = hentSoknadVedleggMedStatus(LASTET_OPP_STATUS, fiksDigisosId, digisosSak.originalSoknadNAV, token)
        val ettersendteVedlegg = hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)

        return soknadVedlegg.plus(ettersendteVedlegg)
    }

    fun hentSoknadVedleggMedStatus(status: String, fiksDigisosId: String, originalSoknadNAV: OriginalSoknadNAV?, token: String): List<InternalVedlegg> {
        if (originalSoknadNAV == null) {
            return emptyList()
        }
        val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(fiksDigisosId, originalSoknadNAV.vedleggMetadata, token)

        if (jsonVedleggSpesifikasjon.vedlegg.isEmpty()) {
            return emptyList()
        }

        return jsonVedleggSpesifikasjon.vedlegg
            .filter { vedlegg -> vedlegg.status == status }
            .map { vedlegg ->
                InternalVedlegg(
                    vedlegg.type,
                    vedlegg.tilleggsinfo,
                    vedlegg.hendelseType,
                    vedlegg.hendelseReferanse,
                    matchDokumentInfoAndJsonFiler(originalSoknadNAV.vedlegg, vedlegg.filer),
                    unixToLocalDateTime(originalSoknadNAV.timestampSendt)
                )
            }
    }

    fun hentEttersendteVedlegg(fiksDigisosId: String, ettersendtInfoNAV: EttersendtInfoNAV?, token: String): List<InternalVedlegg> {
        return ettersendtInfoNAV
            ?.ettersendelser
            ?.flatMap { ettersendelse ->
                var filIndex = 0
                val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(fiksDigisosId, ettersendelse.vedleggMetadata, token)
                jsonVedleggSpesifikasjon.vedlegg
                    .filter { vedlegg -> LASTET_OPP_STATUS == vedlegg.status }
                    .map { vedlegg ->
                        val currentFilIndex = filIndex
                        filIndex += vedlegg.filer.size
                        val filtrerteEttersendelsesVedlegg = ettersendelse.vedlegg
                            .filter { ettersendelseVedlegg -> ettersendelseVedlegg.filnavn != "ettersendelse.pdf" }
                        val dokumentInfoList: List<DokumentInfo>
                        if (filIndex > filtrerteEttersendelsesVedlegg.size) {
                            log.error(
                                "Det er mismatch mellom nedlastede filer og metadata. " +
                                    "Det er flere filer enn vi har Metadata! " +
                                    "Filer: $filIndex Metadata: ${filtrerteEttersendelsesVedlegg.size}"
                            )
                            dokumentInfoList = vedlegg.filer.map { DokumentInfo(it.filnavn, "Error", -1) }
                        } else {
                            dokumentInfoList = filtrerteEttersendelsesVedlegg.subList(currentFilIndex, filIndex)

                            if (!filenamesMatchInDokumentInfoAndFiles(dokumentInfoList, vedlegg.filer)) {
                                throw NedlastingFilnavnMismatchException("Det er mismatch mellom nedlastede filer og metadata", null)
                            }
                        }
                        InternalVedlegg(
                            vedlegg.type,
                            vedlegg.tilleggsinfo,
                            vedlegg.hendelseType,
                            vedlegg.hendelseReferanse,
                            dokumentInfoList,
                            unixToLocalDateTime(ettersendelse.timestampSendt)
                        )
                    }
            } ?: emptyList()
    }

    private fun hentVedleggSpesifikasjon(fiksDigisosId: String, dokumentlagerId: String, token: String): JsonVedleggSpesifikasjon {
        return fiksClient.hentDokument(fiksDigisosId, dokumentlagerId, JsonVedleggSpesifikasjon::class.java, token) as JsonVedleggSpesifikasjon
    }

    private fun matchDokumentInfoAndJsonFiler(dokumentInfoList: List<DokumentInfo>, jsonFiler: List<JsonFiler>): List<DokumentInfo> {
        return jsonFiler
            .flatMap { fil ->
                dokumentInfoList
                    .filter { it.filnavn == fil.filnavn }
            }
    }

    private fun filenamesMatchInDokumentInfoAndFiles(dokumentInfoList: List<DokumentInfo>, files: List<JsonFiler>): Boolean {
        return dokumentInfoList.size == files.size &&
            dokumentInfoList.filterIndexed { idx, it -> sanitizeFileName(it.filnavn) == sanitizeFileName(files[idx].filnavn) }.size == dokumentInfoList.size
    }

    companion object {
        private val log by logger()
    }
}

data class InternalVedlegg(
    val type: String,
    val tilleggsinfo: String?,
    val hendelseType: JsonVedlegg.HendelseType?,
    val hendelseReferanse: String?,
    val dokumentInfoList: List<DokumentInfo>,
    val tidspunktLastetOpp: LocalDateTime
)
