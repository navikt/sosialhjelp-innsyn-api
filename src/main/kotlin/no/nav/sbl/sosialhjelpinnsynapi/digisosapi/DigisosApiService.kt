package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import org.springframework.stereotype.Component

@Component
class DigisosApiService(private val digisosApiClient: DigisosApiClient) {

    fun oppdaterDigisosSak(fiksDigisosId: String?, jsonDigisosSoker: JsonDigisosSoker): String? {
        return digisosApiClient.oppdaterDigisosSak(fiksDigisosId, jsonDigisosSoker)
    }
}