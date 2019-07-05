package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class DigisosApiClientMock(private val fiksClientMock: FiksClientMock) : DigisosApiClient {
    override fun postDigisosSakMedInnsyn(digisosSak: DigisosSak) {
        fiksClientMock.postDigisosSak(digisosSak)
    }

    override fun postDigisosSakMedInnsynNy(digisosSak: DigisosSak) {
        fiksClientMock.postDigisosSak(digisosSak)
    }

}
