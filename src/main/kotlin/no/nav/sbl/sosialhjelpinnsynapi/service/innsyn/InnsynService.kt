package no.nav.sbl.sosialhjelpinnsynapi.service.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneService
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.stereotype.Component

@Component
class InnsynService(
        private val fiksClient: FiksClient,
        private val kommuneService: KommuneService
) {

    fun hentJsonDigisosSoker(digisosId: String, digisosSokerMetadata: String?, token: String): JsonDigisosSoker? {
        return when {
            kommuneService.erInnsynDeaktivertForKommune(digisosId, token) -> log.info("Kommune har deaktivert innsyn -> henter ikke innsynsdata").let { null }
            digisosSokerMetadata != null -> fiksClient.hentDokument(digisosId, digisosSokerMetadata, JsonDigisosSoker::class.java, token) as JsonDigisosSoker
            else -> null
        }
    }

    fun hentOriginalSoknad(digisosId: String, originalSoknadNAVMetadata: String?, token: String): JsonSoknad? {
        return when {
            originalSoknadNAVMetadata != null -> fiksClient.hentDokument(digisosId, originalSoknadNAVMetadata, JsonSoknad::class.java, token) as JsonSoknad
            else -> null
        }
    }

    companion object {
        private val log by logger()
    }
}