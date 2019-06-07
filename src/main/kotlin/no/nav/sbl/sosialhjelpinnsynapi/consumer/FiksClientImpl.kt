package no.nav.sbl.sosialhjelpinnsynapi.consumer

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Primary
@Profile("!mock")
@Component
class FiksClientImpl: FiksClient {

    override fun getInnsynForSoknad(soknadId: Long): JsonDigisosSoker? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
