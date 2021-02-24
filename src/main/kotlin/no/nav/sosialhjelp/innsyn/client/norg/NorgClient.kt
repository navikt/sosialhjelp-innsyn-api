package no.nav.sosialhjelp.innsyn.client.norg

import no.nav.sosialhjelp.innsyn.domain.NavEnhet

interface NorgClient {

    fun hentNavEnhet(enhetsnr: String): NavEnhet

    fun ping()
}
