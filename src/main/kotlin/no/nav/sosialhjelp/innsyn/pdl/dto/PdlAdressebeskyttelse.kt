package no.nav.sosialhjelp.innsyn.pdl.dto

import java.io.Serializable

data class PdlAdressebeskyttelse(
    val gradering: PdlGradering,
) : Serializable
