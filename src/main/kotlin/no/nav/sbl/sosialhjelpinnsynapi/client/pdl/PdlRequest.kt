package no.nav.sbl.sosialhjelpinnsynapi.client.pdl

data class PdlRequest(
        val query: String,
        val variables: Variables
)

data class Variables(
        val ident: String
)