package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraDokumentlagerId
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.springframework.stereotype.Component

@Component
class VedleggService(private val fiksClient: FiksClient,
                     private val dokumentlagerClient: DokumentlagerClient,
                     private val clientProperties: ClientProperties) {

    // TODO:
    //  - Skal vedleggoversikt vise _alle_ bruker-innsendte vedlegg (ifm med innsending av søknad + ettersendelser i form av oppgaver i innsyn)? Antar ja

    fun hentAlleVedlegg(fiksDigisosId: String): List<VedleggResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")

        if (digisosSak.ettersendtInfoNAV.ettersendelser.isEmpty()) {
            return emptyList()
        }

        val vedleggResponses = digisosSak.ettersendtInfoNAV.ettersendelser
                .flatMap {
                    it.vedlegg.map {vedlegg -> VedleggResponse(
                            vedlegg.filnavn,
                            vedlegg.storrelse.toLong(),
                            hentUrlFraDokumentlagerId(clientProperties, vedlegg.dokumentlagerDokumentId),
                            "beskrivelse", // Hvor kommer beskrivelse fra?
                            unixToLocalDateTime(it.timestampSendt)) }
                }

        // Havner ettersendte vedlegg ifm oppgaver i innsyn "samme sted" eller ett annet sted i DigisosSak'en?

        return vedleggResponses
    }

    // Hvis alle vedlegg er no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonVedlegg, kan kanskje tittel benyttes
    private fun hentBeskrivelseFraInnsynVedlegg(dokumentlagerId: String): String {
        val vedlegg = dokumentlagerClient.hentDokument(dokumentlagerId, JsonVedlegg::class.java) as JsonVedlegg
        return vedlegg.tittel
    }
}