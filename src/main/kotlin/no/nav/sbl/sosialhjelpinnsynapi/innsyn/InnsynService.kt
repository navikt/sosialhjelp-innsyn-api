package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class InnsynService(private val fiksClient: FiksClient,
                    private val dokumentlagerClient: DokumentlagerClient) {

    fun hentJsonDigisosSoker(soknadId: String, token: String): JsonDigisosSoker? {
        val digisosSak = fiksClient.hentDigisosSak(soknadId, token)
        return if (digisosSak.digisosSoker != null) {
            dokumentlagerClient.hentDokument(digisosSak.digisosSoker.metadata, JsonDigisosSoker::class.java) as JsonDigisosSoker
        } else {
            null
        }
    }

    fun hentOriginalSoknad(soknadId: String): JsonSoknad {
        val digisosSak = fiksClient.hentDigisosSak(soknadId, "Token")
        return dokumentlagerClient.hentDokument(digisosSak.originalSoknadNAV!!.metadata, JsonSoknad::class.java) as JsonSoknad
    }

    // Returnerer UNIX tid med millisekunder
    fun hentInnsendingstidspunktForOriginalSoknad(soknadId: String): Long {
        val digisosSak = fiksClient.hentDigisosSak(soknadId, "Token")
        return digisosSak.originalSoknadNAV!!.timestampSendt
    }
}