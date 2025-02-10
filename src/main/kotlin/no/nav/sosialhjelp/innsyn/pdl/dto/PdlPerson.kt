package no.nav.sosialhjelp.innsyn.pdl.dto

import java.io.Serializable

data class PdlPerson(
    val adressebeskyttelse: List<PdlAdressebeskyttelse>,
    val navn: List<PdlNavn>
) : Serializable
