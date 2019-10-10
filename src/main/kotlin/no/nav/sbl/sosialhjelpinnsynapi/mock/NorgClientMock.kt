package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.defaultNAVEnhet
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock | local")
@Component
class NorgClientMock : NorgClient {

    private val innsynMap = mutableMapOf<String, NavEnhet>()

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return innsynMap.getOrElse(enhetsnr, {
            val default = defaultNAVEnhet
            innsynMap[enhetsnr] = default
            default
        })
    }

    fun postNavEnhet(enhetsnr: String, navenhet: NavEnhet) {
        innsynMap[enhetsnr] = navenhet
    }
}