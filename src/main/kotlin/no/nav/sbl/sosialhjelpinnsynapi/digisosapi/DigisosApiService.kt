package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.stereotype.Component

@Component
class DigisosApiService(private val digisosApiClient: DigisosApiClient) {

    fun opprettDigisosSak(digisosSak: DigisosSak){
        digisosApiClient.postDigisosSakMedInnsynNy(digisosSak)
    }

    fun oppdaterDigisosSak(digisosSak: DigisosSak) {
        digisosApiClient.postDigisosSakMedInnsyn(digisosSak)
    }
}