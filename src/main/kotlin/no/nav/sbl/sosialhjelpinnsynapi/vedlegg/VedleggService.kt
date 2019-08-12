package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.EttersendtInfoNAV
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.springframework.stereotype.Component

@Component
class VedleggService(private val fiksClient: FiksClient,
                     private val dokumentlagerClient: DokumentlagerClient,
                     private val clientProperties: ClientProperties) {

    fun hentAlleVedlegg(fiksDigisosId: String): List<VedleggResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")

        val soknadVedlegg = hentSoknadVedlegg(digisosSak.originalSoknadNAV)

        val ettersendteVedlegg = hentEttersendteVedlegg(digisosSak.ettersendtInfoNAV)

        return soknadVedlegg.plus(ettersendteVedlegg)
    }

    private fun hentSoknadVedlegg(originalSoknadNAV: OriginalSoknadNAV): List<VedleggResponse> {
        val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(originalSoknadNAV.vedleggMetadata)

        if (jsonVedleggSpesifikasjon.vedlegg.isEmpty()) {
            return emptyList()
        }

        return jsonVedleggSpesifikasjon.vedlegg
                .filter { "LastetOpp" == it.status }
                .flatMap {
                    it.filer.map { fil ->
                        VedleggResponse(
                                fil.filnavn,
                                hentStorrelse(fil.filnavn, originalSoknadNAV.vedlegg),
                                hentUrl(fil.filnavn, originalSoknadNAV.vedlegg),
                                it.type,
                                unixToLocalDateTime(originalSoknadNAV.timestampSendt))
                    }
                }
    }

    private fun hentEttersendteVedlegg(ettersendtInfoNAV: EttersendtInfoNAV): List<VedleggResponse> {
        return ettersendtInfoNAV.ettersendelser.flatMap {
            val jsonVedleggSpesifikasjon = hentVedleggSpesifikasjon(it.vedleggMetadata)
            jsonVedleggSpesifikasjon.vedlegg
                    .filter { vedlegg -> "LastetOpp" == vedlegg.status }
                    .flatMap { vedlegg ->
                        vedlegg.filer
                                .map { fil ->
                                    VedleggResponse(
                                            fil.filnavn,
                                            hentStorrelse(fil.filnavn, it.vedlegg),
                                            hentUrl(fil.filnavn, it.vedlegg),
                                            vedlegg.type,
                                            unixToLocalDateTime(it.timestampSendt))
                                }
                    }
        }
    }

    private fun hentVedleggSpesifikasjon(dokumentlagerId: String): JsonVedleggSpesifikasjon {
        return dokumentlagerClient.hentDokument(dokumentlagerId, JsonVedleggSpesifikasjon::class.java) as JsonVedleggSpesifikasjon
    }

    private fun hentStorrelse(filnavn: String, list: List<DokumentInfo>): Long {
        val first = list.first { it.filnavn == filnavn }
        return first.storrelse
    }

    private fun hentUrl(filnavn: String, list: List<DokumentInfo>): String {
        val first = list.first { it.filnavn == filnavn }
        return hentDokumentlagerUrl(clientProperties, first.dokumentlagerDokumentId)
    }
}