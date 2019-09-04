package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import org.springframework.stereotype.Component

@Component
class DigisosApiService(private val digisosApiClient: DigisosApiClient) {

    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        return digisosApiClient.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
    }
}