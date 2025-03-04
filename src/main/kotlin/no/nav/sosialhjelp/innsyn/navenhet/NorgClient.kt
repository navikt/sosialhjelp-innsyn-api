package no.nav.sosialhjelp.innsyn.navenhet

interface NorgClient {
    suspend fun hentNavEnhet(enhetsnr: String): NavEnhet
}

