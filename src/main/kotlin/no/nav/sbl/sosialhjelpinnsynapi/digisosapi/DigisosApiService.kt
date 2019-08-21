package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.stereotype.Component

@Component
class DigisosApiService(private val digisosApiClient: DigisosApiClient) {

    fun oppdaterDigisosSak(fiksDigisosId: String?, hendelser: List<JsonHendelse>) {
        digisosApiClient.oppdaterDigisosSak(fiksDigisosId, hendelser)
    }
}