package no.nav.sosialhjelp.innsyn.digisosapitest.dto

data class FilOpplastingResponse(
    val filnavn: String,
    val dokumentlagerDokumentId: String,
    val storrelse: Long,
)
