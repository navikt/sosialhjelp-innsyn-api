package no.nav.sosialhjelp.innsyn.tilgang.pdl

import java.io.Serializable

interface PdlResponse {
    val errors: List<PdlError>?
}

data class PdlPersonResponse(
    override val errors: List<PdlError>?,
    val data: PdlHentPerson?,
) : PdlResponse

data class PdlIdenterResponse(
    override val errors: List<PdlError>?,
    val data: PdlHentIdenter?,
) : PdlResponse

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension,
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?,
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String,
)

data class PdlHentIdenter(
    val hentIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>,
)

data class PdlIdent(
    val ident: String,
)

data class PdlHentPerson(
    val hentPerson: PdlPersonOld?,
) : Serializable

data class PdlPersonOld(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val navn: List<PdlNavn>,
) : Serializable

data class PdlNavn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
) : Serializable

data class Adressebeskyttelse(
    val gradering: Gradering,
) : Serializable

enum class Gradering : Serializable {
    STRENGT_FORTROLIG_UTLAND, // kode 6 (utland)
    STRENGT_FORTROLIG, // kode 6
    FORTROLIG, // kode 7
    UGRADERT,
}

fun PdlPersonOld.isKode6Or7(): Boolean =
    adressebeskyttelse.any {
        it.isKode6() || it.isKode7()
    }

fun Adressebeskyttelse.isKode6(): Boolean =
    this.gradering == Gradering.STRENGT_FORTROLIG || this.gradering == Gradering.STRENGT_FORTROLIG_UTLAND

fun Adressebeskyttelse.isKode7(): Boolean = this.gradering == Gradering.FORTROLIG
