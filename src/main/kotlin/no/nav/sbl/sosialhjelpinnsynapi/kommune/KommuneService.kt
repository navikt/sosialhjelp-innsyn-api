package no.nav.sbl.sosialhjelpinnsynapi.kommune

import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.IKKE_INNSYN
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.INNSYN
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.stereotype.Component

@Component
class KommuneService(private val fiksClient: FiksClient) {

    companion object {
        val log by logger()
    }

    fun hentKommuneStatus(kommunenummer: String): KommuneStatus {
        val kommuneInfo = fiksClient.hentKommuneInfo(kommunenummer)

        return when {
            !kommuneInfo.kanOppdatereStatus -> IKKE_INNSYN
            kommuneInfo.kanOppdatereStatus -> INNSYN
            else -> {
                log.warn("Forsøkte å hente kommunestatus, men scenariet er ikke dekket")
                throw RuntimeException("KommuneStatus scenario er ikke dekket")
            }
        }
    }
}

enum class KommuneStatus {
    IKKE_INNSYN,
    INNSYN
}