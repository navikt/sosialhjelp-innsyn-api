package no.nav.sosialhjelp.innsyn.tilgang.pdl

interface PdlClient {
    suspend fun hentPerson(
        ident: String,
        token: String,
    ): PdlHentPerson?

    suspend fun hentIdenter(
        ident: String,
        token: String,
    ): List<String>

    fun ping()
}

