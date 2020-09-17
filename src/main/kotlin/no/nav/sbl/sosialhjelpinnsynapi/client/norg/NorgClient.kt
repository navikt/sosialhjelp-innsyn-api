package no.nav.sbl.sosialhjelpinnsynapi.client.norg

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet

interface NorgClient {

    fun hentNavEnhet(enhetsnr: String): NavEnhet

    fun ping()
}
