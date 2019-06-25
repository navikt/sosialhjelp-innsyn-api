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