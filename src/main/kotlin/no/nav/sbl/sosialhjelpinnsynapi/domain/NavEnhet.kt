package no.nav.sbl.sosialhjelpinnsynapi.domain

data class NavEnhet(
        val enhetId: Int,
        val navn: String,
        val enhetNr: Int,
        val status: String,
        val antallRessurser: Int,
        val aktiveringsdato: String,
        val nedleggelsesdato: String?
)
