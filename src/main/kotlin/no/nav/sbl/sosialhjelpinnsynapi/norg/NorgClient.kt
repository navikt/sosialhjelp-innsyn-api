package no.nav.sbl.sosialhjelpinnsynapi.norg

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet

interface NorgClient {

    fun hentNavEnhet(enhetsnr: String): NavEnhet

    fun postNavEnhet(enhetsnr: String, navenhet: NavEnhet)
}
