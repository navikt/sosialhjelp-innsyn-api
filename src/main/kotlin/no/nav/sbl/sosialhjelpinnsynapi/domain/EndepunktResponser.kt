package no.nav.sbl.sosialhjelpinnsynapi.domain

data class SoknadStatusResponse(
        val status: SoknadStatus
)

enum class SoknadStatus {
    SENDT,
    MOTTATT,
    UNDER_BEHANDLING,
    FERDIGBEHANDLET
}

data class SaksStatusResponse(
        val tittel: String,
        val status: UtfallEllerSaksStatus,
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