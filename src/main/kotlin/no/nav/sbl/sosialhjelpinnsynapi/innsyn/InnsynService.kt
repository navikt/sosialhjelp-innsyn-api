package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class InnsynService(val fiksClient: FiksClient) {

    fun hentDigisosSak(soknadId: String): DigisosSak {
        return fiksClient.hentDigisosSak(soknadId)
    }
}