package no.nav.sbl.sosialhjelpinnsynapi.kommune

import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.IKKE_INNSYN
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.INNSYN
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log: Logger = LoggerFactory.getLogger(KommuneService::class.java)

@Component
class KommuneService(private val fiksClient: FiksClient) {


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