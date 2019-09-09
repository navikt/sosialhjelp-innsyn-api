package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

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
                .filter { LASTET_OPP_STATUS == it.status }
                .flatMap {
                    it.filer.map { fil ->
                        InternalVedlegg(
                                fil.filnavn,
                                it.type,
                                if (it.tilleggsinfo != null) it.tilleggsinfo else null,
                                originalSoknadNAV.vedlegg,
                                unixToLocalDateTime(originalSoknadNAV.timestampSendt)
                        )
                    }
                }
    }

    fun hentEttersendteVedlegg(ettersendtInfoNAV: EttersendtInfoNAV): List<InternalVedlegg> {
        return ettersendtInfoNAV.ettersendelser.flatMap {
            val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(it.vedleggMetadata)
            jsonVedleggSpesifikasjon.vedlegg
                    .filter { vedlegg -> LASTET_OPP_STATUS == vedlegg.status }
                    .flatMap { vedlegg ->
                        vedlegg.filer
                                .map { fil ->
                                    InternalVedlegg(
                                            fil.filnavn,
                                            vedlegg.type,
                                            if (vedlegg.tilleggsinfo != null) vedlegg.tilleggsinfo else null,
                                            it.vedlegg,
                                            unixToLocalDateTime(it.timestampSendt)
                                    )
                                }
                    }
        }
    }

    private fun hentVedleggSpesifikasjon(dokumentlagerId: String): JsonVedleggSpesifikasjon {
        return dokumentlagerClient.hentDokument(dokumentlagerId, JsonVedleggSpesifikasjon::class.java) as JsonVedleggSpesifikasjon
    }

    data class InternalVedlegg(
            val filnavn: String,
            val type: String,
            val tilleggsinfo: String?,
            val dokumentInfoList: List<DokumentInfo>,
            val tidspunktLastetOpp: LocalDateTime
    )
}