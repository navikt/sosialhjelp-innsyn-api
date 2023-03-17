package no.nav.sosialhjelp.innsyn.vedlegg.dto

import no.nav.sosialhjelp.innsyn.vedlegg.ValidationValues

data class VedleggOpplastingResponse(
    val filnavn: String?,
    val status: ValidationValues
)
