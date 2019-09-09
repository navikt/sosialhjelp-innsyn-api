package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.EttersendtInfoNAV
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private const val LASTET_OPP_STATUS = "LastetOpp"

@Component
class VedleggService(private val fiksClient: FiksClient,
                     private val dokumentlagerClient: DokumentlagerClient) {

    fun hentAlleVedlegg(fiksDigisosId: String): List<InternalVedlegg> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")

        val soknadVedlegg = hentSoknadVedlegg(digisosSak.originalSoknadNAV)
        val ettersendteVedlegg = hentEttersendteVedlegg(digisosSak.ettersendtInfoNAV)

        return soknadVedlegg.plus(ettersendteVedlegg)
    }

    private fun hentSoknadVedlegg(originalSoknadNAV: OriginalSoknadNAV): List<InternalVedlegg> {
        val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(originalSoknadNAV.vedleggMetadata)

        if (jsonVedleggSpesifikasjon.vedlegg.isEmpty()) {
            return emptyList()
        }

        return jsonVedleggSpesifikasjon.vedlegg
                .filter { vedlegg -> LASTET_OPP_STATUS == vedlegg.status }
                .map { vedlegg ->
                    InternalVedlegg(
                            vedlegg.type,
                            vedlegg.tilleggsinfo,
                            matchDokumentInfoAndJsonFiler(originalSoknadNAV.vedlegg, vedlegg.filer),
                            unixToLocalDateTime(originalSoknadNAV.timestampSendt)
                    )
                }
    }

    fun hentEttersendteVedlegg(ettersendtInfoNAV: EttersendtInfoNAV): List<InternalVedlegg> {
        return ettersendtInfoNAV.ettersendelser
                .flatMap { ettersendelse ->
                    val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(ettersendelse.vedleggMetadata)
                    jsonVedleggSpesifikasjon.vedlegg
                            .filter { vedlegg -> LASTET_OPP_STATUS == vedlegg.status }
                            .map { vedlegg ->
                                InternalVedlegg(
                                        vedlegg.type,
                                        vedlegg.tilleggsinfo,
                                        matchDokumentInfoAndJsonFiler(ettersendelse.vedlegg, vedlegg.filer),
                                        unixToLocalDateTime(ettersendelse.timestampSendt)
                                )
                            }
                }
    }

    private fun hentVedleggSpesifikasjon(dokumentlagerId: String): JsonVedleggSpesifikasjon {
        return dokumentlagerClient.hentDokument(dokumentlagerId, JsonVedleggSpesifikasjon::class.java) as JsonVedleggSpesifikasjon
    }

    private fun matchDokumentInfoAndJsonFiler(dokumentInfoList: List<DokumentInfo>, jsonFiler: List<JsonFiler>): List<DokumentInfo> {
        return jsonFiler
                .flatMap { fil ->
                    dokumentInfoList
                            .filter { it.filnavn == fil.filnavn }
                }
    }

    data class InternalVedlegg(
            val type: String,
            val tilleggsinfo: String?,
            val dokumentInfoList: List<DokumentInfo>,
            val tidspunktLastetOpp: LocalDateTime
    )
}