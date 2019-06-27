package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class InnsynService(private val fiksClient: FiksClient,
                    private val dokumentlagerClient: DokumentlagerClient) {

    fun hentDigisosSak(soknadId: String): JsonDigisosSoker {
        val digisosSak = fiksClient.hentDigisosSak(soknadId)
        return dokumentlagerClient.hentDokument(digisosSak.digisosSoker.metadata, JsonDigisosSoker::class.java) as JsonDigisosSoker
    }

    fun hentOriginalSoknad(soknadId: String): JsonSoknad {
        val digisosSak = fiksClient.hentDigisosSak(soknadId)
        return dokumentlagerClient.hentDokument(digisosSak.orginalSoknadNAV.metadata, JsonSoknad::class.java) as JsonSoknad
    }

    fun hentInnsendingstidspunktForOriginalSoknad(soknadId: String): Long {
        val digisosSak = fiksClient.hentDigisosSak(soknadId)
        return digisosSak.orginalSoknadNAV.timestampSendt
    }
}