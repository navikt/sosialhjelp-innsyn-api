package no.nav.sbl.sosialhjelpinnsynapi.domain

data class SoknadStatusResponse(
        val status: SoknadStatus,
        val vedtaksinfo: String?
) {
    constructor(status: SoknadStatus) : this(status, null)
}

enum class SoknadStatus {
    SENDT,
    MOTTATT,
    UNDER_BEHANDLING,
    FERDIGBEHANDLET
}

data class SaksStatusResponse(
        val tittel: String,
        val status: UtfallEllerSaksStatus,
        val vedtaksfilUrl: String?,
        val vedtaksfilNavn: String?
)

enum class UtfallEllerSaksStatus {
    UNDER_BEHANDLING, IKKE_INNSYN, INNVILGET, DELVIS_INNVILGET, AVSLATT, AVVIST
}