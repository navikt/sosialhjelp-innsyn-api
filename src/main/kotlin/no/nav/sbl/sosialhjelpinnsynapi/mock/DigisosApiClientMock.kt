package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Profile("mock")
@Component
class DigisosApiClientMock(private val fiksClientMock: FiksClientMock) : DigisosApiClient {
    override fun oppdaterDigisosSak(digisosSak: DigisosSak) {
        fiksClientMock.postDigisosSak(digisosSak)
    }

    override fun opprettDigisosSak(fiksOrgId: String): String {
        return UUID.randomUUID().toString()
    }

}
