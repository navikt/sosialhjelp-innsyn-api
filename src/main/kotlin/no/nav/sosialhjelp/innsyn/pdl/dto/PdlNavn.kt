package no.nav.sosialhjelp.innsyn.pdl.dto

import java.io.Serializable

data class PdlNavn (
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) : Serializable
