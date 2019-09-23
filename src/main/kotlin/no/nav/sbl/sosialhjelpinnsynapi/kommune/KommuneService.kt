package no.nav.sbl.sosialhjelpinnsynapi.kommune

import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KommuneService(private val fiksClient: FiksClient) {

    private val log: Logger = LoggerFactory.getLogger(KommuneService::class.java)

    fun hentKommuneStatus(kommunenummer: String): KommuneStatus {
        val kommuneInfo = fiksClient.hentKommuneInfo(kommunenummer)

        return when {
            !kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus -> IKKE_PA_FIKS_ELLER_INNSYN
            kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus -> KUN_PA_FIKS
            kommuneInfo.kanMottaSoknader && kommuneInfo.kanOppdatereStatus -> PA_FIKS_OG_INNSYN
            else -> {
                log.error("Noe feil skjedde her")
                throw RuntimeException("Noe feil skjedde her")
            }
        }
    }
}

enum class KommuneStatus {
    IKKE_PA_FIKS_ELLER_INNSYN,
    KUN_PA_FIKS,
    PA_FIKS_OG_INNSYN
}