package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class InnsynService(val fiksClient: FiksClient) {

    fun execute(soknadId: Long): String {
//        fiksClient.hentDigisosSak(soknadId)
        return "ok"
    }
}