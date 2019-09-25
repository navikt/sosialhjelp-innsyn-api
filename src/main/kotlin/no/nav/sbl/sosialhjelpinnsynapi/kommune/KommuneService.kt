package no.nav.sbl.sosialhjelpinnsynapi.kommune

import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.FIKS_OG_INNSYN
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.KUN_FIKS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KommuneService(private val fiksClient: FiksClient) {

    private val log: Logger = LoggerFactory.getLogger(KommuneService::class.java)

    fun hentKommuneStatus(kommunenummer: String): KommuneStatus {
        val kommuneInfo = fiksClient.hentKommuneInfo(kommunenummer)

        return when {
            kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus -> KUN_FIKS
            kommuneInfo.kanMottaSoknader && kommuneInfo.kanOppdatereStatus -> FIKS_OG_INNSYN
            else -> {
                log.warn("Forsøkte å hente kommunestatus, men scenariet er ikke dekket")
                throw RuntimeException("KommuneStatus scenario er ikke dekket")
            }
        }
    }
}

enum class KommuneStatus {
    IKKE_FIKS_ELLER_INNSYN,
    KUN_FIKS,
    FIKS_OG_INNSYN,
    KUN_INNSYN
}