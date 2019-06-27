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