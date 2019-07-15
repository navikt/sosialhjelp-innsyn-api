package no.nav.sbl.sosialhjelpinnsynapi.domain

data class SoknadsStatusResponse(
        val status: SoknadsStatus
)

data class SaksStatusResponse(
        val tittel: String,
        val status: UtfallEllerSaksStatus?,
        val vedtaksfilUrlList: List<String>?
)

enum class UtfallEllerSaksStatus {
    UNDER_BEHANDLING, IKKE_INNSYN, FEILREGISTRERT, INNVILGET, DELVIS_INNVILGET, AVSLATT, AVVIST, OMGJORT
}

data class HendelseResponse(
        val tidspunkt: String,
        val beskrivelse: String,
        val filUrl: String?
)

data class OppgaveResponse(
        val innsendelsesfrist: String,
        val dokumenttype: String,
        val tilleggsinformasjon: String?
)

data class VedleggOpplastingResponse(
        val filnavn: String?,
        val storrelse: Long
)

data class VedleggSendtResponse ( // TODO endre
        val filnavn: String,
        val storrelse: Long
)