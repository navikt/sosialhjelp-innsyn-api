package no.nav.sosialhjelp.innsyn.app.exceptions

import com.fasterxml.jackson.annotation.JsonValue

enum class FrontendErrorType(
    @get:JsonValue val value: String,
) {
    UNEXPECTED_ERROR("unexpected_error"),
    FIKS_ERROR("fiks_error"),
    NORG_ERROR("norg_error"),
    FILOPPLASTING_ERROR("FILOPPLASTING_ERROR"),
    PDL_ERROR("pdl_error"),
    TILGANG_ERROR("tilgang_error"),
    NOT_FOUND_ERROR("not_found_error"),
    UNAVAILABLE_ERROR("unavailable_error"),
    KLAGE_IKKE_TILGJENGELIG("klage_ikke_tilgjengelig"),
    KLAGE_MOTTAKER_ERROR("klage_mottaker_error"),
    KLAGE_FOR_LANG_TEKST("klage_for_lang_tekst"),
    KLAGE_MANGLER_TEKST_OG_VEDLEGG("klage_mangler_tekst_og_vedlegg"),
    TOKEN_UTLOPT("Token utløpt"),
}

class FrontendErrorMessage(
    val type: FrontendErrorType,
    val message: String?,
)
