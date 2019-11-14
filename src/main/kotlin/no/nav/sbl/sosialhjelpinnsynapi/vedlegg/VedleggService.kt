package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.NedlastingFilnavnMismatchException
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.EttersendtInfoNAV
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime

const val LASTET_OPP_STATUS = "LastetOpp"
const val VEDLEGG_KREVES_STATUS = "VedleggKreves"

@Component
class VedleggService(private val fiksClient: FiksClient) {

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

        var filIndex = 0
        return jsonVedleggSpesifikasjon.vedlegg
                .filter { vedlegg -> vedlegg.status == status }
                .map { vedlegg ->
                    val currentFilIndex = filIndex
                    filIndex += vedlegg.filer.size
                    filIndex = filIndex.coerceAtMost(originalSoknadNAV.vedlegg.size)
                    val dokumentInfoList = originalSoknadNAV.vedlegg.subList(currentFilIndex, filIndex)
                    if (!filenamesMatchInDokumentInfoAndFiles(dokumentInfoList, vedlegg.filer)) {
                        throw NedlastingFilnavnMismatchException("Det er mismatch mellom nedlastede filer og metadata", null)
                    }
                    InternalVedlegg(
                            vedlegg.type,
                            vedlegg.tilleggsinfo,
                            dokumentInfoList,
                            unixToLocalDateTime(originalSoknadNAV.timestampSendt)
                    )
                }
    }

    fun hentEttersendteVedlegg(fiksDigisosId: String, ettersendtInfoNAV: EttersendtInfoNAV?, token: String): List<InternalVedlegg> {
        return ettersendtInfoNAV?.ettersendelser
                ?.flatMap { ettersendelse ->
                    var filIndex = 0
                    val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(fiksDigisosId, ettersendelse.vedleggMetadata, token)
                    jsonVedleggSpesifikasjon.vedlegg
                            .filter { vedlegg -> LASTET_OPP_STATUS == vedlegg.status }
                            .map { vedlegg ->
                                val currentFilIndex = filIndex
                                filIndex += vedlegg.filer.size
                                filIndex = filIndex.coerceAtMost(ettersendelse.vedlegg.size)
                                val dokumentInfoList = ettersendelse.vedlegg.subList(currentFilIndex, filIndex)
                                if (!filenamesMatchInDokumentInfoAndFiles(dokumentInfoList, vedlegg.filer)) {
                                    throw NedlastingFilnavnMismatchException("Det er mismatch mellom nedlastede filer og metadata", null)
                                }
                                InternalVedlegg(
                                        vedlegg.type,
                                        vedlegg.tilleggsinfo,
                                        dokumentInfoList,
                                        unixToLocalDateTime(ettersendelse.timestampSendt)
                                )
                            }
                } ?: emptyList()
    }

    private fun hentVedleggSpesifikasjon(fiksDigisosId: String, dokumentlagerId: String, token: String): JsonVedleggSpesifikasjon {
        return fiksClient.hentDokument(fiksDigisosId, dokumentlagerId, JsonVedleggSpesifikasjon::class.java, token) as JsonVedleggSpesifikasjon
    }

    private fun filenamesMatchInDokumentInfoAndFiles(dokumentInfoList: List<DokumentInfo>, files: List<JsonFiler>): Boolean {
        return dokumentInfoList.size == files.size &&
                dokumentInfoList.filterIndexed{ idx, it -> it.filnavn == files[idx].filnavn }.size == dokumentInfoList.size
    }

    data class InternalVedlegg(
            val type: String,
            val tilleggsinfo: String?,
            val dokumentInfoList: List<DokumentInfo>,
            val tidspunktLastetOpp: LocalDateTime
    )
}