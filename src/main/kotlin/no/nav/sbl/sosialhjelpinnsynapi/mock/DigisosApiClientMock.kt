package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Profile("mock")
@Component
class DigisosApiClientMock(private val fiksClientMock: FiksClientMock) : DigisosApiClient {
    override fun oppdaterDigisosSak(fiksDigisosId:String?, hendelser:List<JsonHendelse>) : String?{
       // fiksClientMock.postDigisosSak(digisosSak)
        return ""
    }
}
