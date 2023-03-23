package no.nav.sosialhjelp.innsyn.kommuneinfo.dto

data class KommuneRequest(
    val query: String,
    val variables: Variables
) {
    constructor(query: String, kommunenummer: String) : this(query, Variables(kommunenummer))
}

data class Variables(
    val kommunenummer: String
)
