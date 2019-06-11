package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class InnsynService(private val fiksClient: FiksClient,
                    private val dokumentlagerClient: DokumentlagerClient) {

    fun hentDigisosSak(soknadId: String): DigisosSak {
        val digisosSak = fiksClient.hentDigisosSak(soknadId)

//        Hent digisos_soker.json, og map om?
        val string = dokumentlagerClient.hentDokument(digisosSak.digisosSoker.metadata)

        return digisosSak
    }
}