package no.nav.sbl.sosialhjelpinnsynapi.digisosapi


import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.stereotype.Component

@Component
interface DigisosApiClient {
    fun opprettDigisosSak(fiksOrgId: String): String?
    fun oppdaterDigisosSak(digisosSak: DigisosSak)
}
