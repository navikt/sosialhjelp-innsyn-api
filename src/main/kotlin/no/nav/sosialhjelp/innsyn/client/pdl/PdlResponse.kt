package no.nav.sosialhjelp.innsyn.client.pdl

interface PdlResponse {
    val errors: List<PdlError>?
}

data class PdlPersonResponse(
    override val errors: List<PdlError>?,
    val data: PdlHentPerson?
) : PdlResponse

data class PdlIdenterResponse(
    override val errors: List<PdlError>?,
    val data: PdlHentIdenter?
) : PdlResponse

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String
)

data class PdlHentIdenter(
    val hentIdenter: PdlIdenter?
)

data class PdlIdenter(
    val identer: List<PdlIdent>
)

data class PdlIdent(
    val ident: String,
)

data class PdlHentPerson(
    val hentPerson: PdlPerson?
)

data class PdlPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val navn: List<PdlNavn>
)

data class PdlNavn(
    val fornavn: String?
)

data class Adressebeskyttelse(
    val gradering: Gradering
)

enum class Gradering {
    STRENGT_FORTROLIG_UTLAND, // kode 6 (utland)
    STRENGT_FORTROLIG, // kode 6
    FORTROLIG, // kode 7
    UGRADERT
}

fun PdlPerson.isKode6Or7(): Boolean {
    return adressebeskyttelse.any {
        it.isKode6() || it.isKode7()
    }
}

fun Adressebeskyttelse.isKode6(): Boolean {
    return this.gradering == Gradering.STRENGT_FORTROLIG || this.gradering == Gradering.STRENGT_FORTROLIG_UTLAND
}

fun Adressebeskyttelse.isKode7(): Boolean {
    return this.gradering == Gradering.FORTROLIG
}
