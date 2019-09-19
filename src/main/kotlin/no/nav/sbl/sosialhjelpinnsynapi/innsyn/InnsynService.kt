package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import org.springframework.stereotype.Component

@Component
class InnsynService(private val dokumentlagerClient: DokumentlagerClient) {

    fun hentJsonDigisosSoker(digisosSokerMetadata: String?, token: String): JsonDigisosSoker? {
        return when {
            digisosSokerMetadata != null -> dokumentlagerClient.hentDokument(digisosSokerMetadata, JsonDigisosSoker::class.java, token) as JsonDigisosSoker
            else -> null
        }
    }

    fun hentOriginalSoknad(originalSoknadNAVMetadata: String?, token: String): JsonSoknad? {
        return when {
            originalSoknadNAVMetadata != null -> dokumentlagerClient.hentDokument(originalSoknadNAVMetadata, JsonSoknad::class.java, token) as JsonSoknad
            else -> null
        }
    }
}