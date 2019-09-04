package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.EttersendtInfoNAV
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VedleggHistorikkService(private val fiksClient: FiksClient,
                              private val dokumentlagerClient: DokumentlagerClient) {

    fun hentVedlegg(fiksDigisosId: String): List<Vedlegg> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")

        val soknadVedlegg = hentSoknadVedlegg(digisosSak.originalSoknadNAV)
        val ettersendteVedlegg = hentEttersendteVedlegg(digisosSak.ettersendtInfoNAV)

        return soknadVedlegg.plus(ettersendteVedlegg)
    }

    private fun hentSoknadVedlegg(originalSoknadNAV: OriginalSoknadNAV): List<Vedlegg> {
        val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(originalSoknadNAV.vedleggMetadata)

        if (jsonVedleggSpesifikasjon.vedlegg.isEmpty()) {
            return emptyList()
        }

        return jsonVedleggSpesifikasjon.vedlegg
                .filter { "LastetOpp" == it.status }
                .flatMap { vedlegg ->
                    vedlegg.filer.map {
                        Vedlegg(
                                vedlegg.type,
                                unixToLocalDateTime(originalSoknadNAV.timestampSendt))
                    }
                }
    }

    private fun hentEttersendteVedlegg(ettersendtInfoNAV: EttersendtInfoNAV): List<Vedlegg> {
        return ettersendtInfoNAV.ettersendelser.flatMap { ettersendelse ->
            val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(ettersendelse.vedleggMetadata)
            jsonVedleggSpesifikasjon.vedlegg
                    .filter { vedlegg -> "LastetOpp" == vedlegg.status }
                    .flatMap { vedlegg ->
                        vedlegg.filer
                                .map {
                                    Vedlegg(
                                            vedlegg.type,
                                            unixToLocalDateTime(ettersendelse.timestampSendt))
                                }
                    }
        }
    }

    private fun hentVedleggSpesifikasjon(dokumentlagerId: String): JsonVedleggSpesifikasjon {
        return dokumentlagerClient.hentDokument(dokumentlagerId, JsonVedleggSpesifikasjon::class.java) as JsonVedleggSpesifikasjon
    }

    data class Vedlegg(
            val type: String,
            val tidspunktLastetOpp: LocalDateTime
    )
}