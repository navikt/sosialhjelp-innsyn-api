package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class DigisosApiClientMock : DigisosApiClient {
    override fun postDigisosSakMedInnsyn(digisosSak: DigisosSak) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun postDigisosSakMedInnsynNy(digisosSak: DigisosSak) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
