package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.KommuneStatus.*
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DigisosApiService(private val digisosApiClient: DigisosApiClient,
                        private val fiksClient: FiksClient) {

    private val log: Logger = LoggerFactory.getLogger(DigisosApiService::class.java)

    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        return digisosApiClient.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
    }

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