package no.nav.sosialhjelp.innsyn.tilgang.pdl

data class PdlRequest(
    val query: String,
    val variables: Variables,
)

data class Variables(
    val ident: String,
)
