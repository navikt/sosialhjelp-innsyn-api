package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.sbl.sosialhjelpinnsynapi.config.KommuneStatus.*
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KommuneService(private val fiksClient: FiksClient) {

    /*
    TODO:
        Vi må vite om en brukers tilhørende kommune er på fiks-løsningen og om de er på innsyn.
     */

    private val log: Logger = LoggerFactory.getLogger(KommuneService::class.java)

    fun hentKommuneStatus(kommunenummer: String, token: String): KommuneStatus {
        val kommuneInfo = fiksClient.hentKommuneInfo(kommunenummer, token)

        return when {
            false -> IKKE_PA_FIKS_ELLER_INNSYN
            kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus -> KUN_PA_FIKS
            kommuneInfo.kanMottaSoknader && kommuneInfo.kanOppdatereStatus -> PA_FIKS_OG_INNSYN
            else -> {
                // something is wrong
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