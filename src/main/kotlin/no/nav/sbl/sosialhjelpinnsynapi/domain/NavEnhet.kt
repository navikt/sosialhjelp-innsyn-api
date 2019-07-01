package no.nav.sbl.sosialhjelpinnsynapi.domain

data class NavEnhet(
        val antallRessurser: Int,
        val enhetId: Int,
        val enhetNr: Int,
        val gyldigFra: String,
        val gyldigTil: String?,
        val navn: String,
        val status: String
)
