package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock | local")
@Component
class NorgClientMock : NorgClient {

    private val innsynMap = mutableMapOf<String, NavEnhet>()

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return innsynMap.getOrElse(enhetsnr, {
            val default = NavEnhet(
                    enhetId = 100000367,
                    navn = "NAV Longyearbyen",
                    enhetNr = enhetsnr,
                    antallRessurser = 20,
                    status = "AKTIV",
                    aktiveringsdato = "1982-04-21",
                    nedleggelsesdato = "null"
            )
            innsynMap[enhetsnr] = default
            default
        })
    }

    override fun ping() {
        // no-op
    }

    fun postNavEnhet(enhetsnr: String, navenhet: NavEnhet) {
        innsynMap[enhetsnr] = navenhet
    }
}