package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraDokumentlagerId
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VedleggService(private val fiksClient: FiksClient,
                     private val clientProperties: ClientProperties) {

    // hent alle vedlegg fra digisosSak

    fun execute(fiksDigisosId: String): List<VedleggResponse> {

        // DigisosSak.DigisosSoker.Dokumenter eller DigisosSak.EttersendtInfoNAV.ettersendelser ???

        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")

        if (digisosSak.digisosSoker == null) {
            return emptyList()
        }

        val vedleggResponses = digisosSak.digisosSoker.dokumenter
                .map {
                    VedleggResponse(
                            it.filnavn,
                            it.storrelse.toLong(),
                            hentUrlFraDokumentlagerId(clientProperties, it.dokumentlagerDokumentId),
                            "beskrivelse", // Hvor kommer beskrivelse fra?
                            LocalDateTime.now() // datoLagtTil - Hvor er dennne satt?
                    )
                }

        return vedleggResponses
    }
}