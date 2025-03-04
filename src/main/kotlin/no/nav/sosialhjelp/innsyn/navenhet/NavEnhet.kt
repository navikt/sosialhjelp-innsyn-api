package no.nav.sosialhjelp.innsyn.navenhet

import java.io.Serializable

data class NavEnhet(
    val enhetId: Int,
    val navn: String,
    val enhetNr: String,
    val status: String,
    val antallRessurser: Int,
    val aktiveringsdato: String,
    val nedleggelsesdato: String?,
) : Serializable
