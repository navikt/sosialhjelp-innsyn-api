package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.stereotype.Component

@Component
class DigisosApiService(private val digisosApiClient: DigisosApiClient) {

    fun opprettDigisosSak(fiksOrgId: String): String? {
       return digisosApiClient.opprettDigisosSak(fiksOrgId)
    }

    fun oppdaterDigisosSak(digisosSak: DigisosSak) {
        digisosApiClient.oppdaterDigisosSak(digisosSak)
    }
}