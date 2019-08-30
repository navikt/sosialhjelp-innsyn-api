package no.nav.sbl.sosialhjelpinnsynapi.digisosapi


import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import org.springframework.stereotype.Component

@Component
interface DigisosApiClient {
    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?
}
