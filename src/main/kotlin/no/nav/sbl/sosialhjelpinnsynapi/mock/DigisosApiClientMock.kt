package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class DigisosApiClientMock(private val fiksClientMock: FiksClientMock) : DigisosApiClient {
    override fun oppdaterDigisosSak(fiksDigisosId:String?, jsonDigisosSoker: JsonDigisosSoker) : String?{
       // fiksClientMock.postDigisosSak(digisosSak)
        return ""
    }
}
