package no.nav.sbl.sosialhjelpinnsynapi.domain

import java.time.LocalDate

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

data class UtbetalingerResponse(
        val maned: MutableList<UtbetalingerManedResponse>
)

data class UtbetalingerManedResponse(
        val tittel: String,
        val utbetalinger: MutableList<UtbetalingResponse>,
        val belop: Double
)

data class UtbetalingResponse(
        val tittel: String,
        val belop: Double,
        val utbetalingsdato: LocalDate
)

data class VedleggOpplastingResponse(
        val filnavn: String?,
        val storrelse: Long
)