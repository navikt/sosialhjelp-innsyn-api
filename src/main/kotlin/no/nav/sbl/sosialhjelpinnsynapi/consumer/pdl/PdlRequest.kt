package no.nav.sbl.sosialhjelpinnsynapi.consumer.pdl

data class PdlRequest(
        val query: String,
        val variables: Variables
)

data class Variables(
        val ident: String
)