package no.nav.sbl.sosialhjelpinnsynapi.domain

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

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

data class VedleggResponse(
        val filnavn: String,
        val storrelse: Long,
        val url: String,
        val beskrivelse: String,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        val datoLagtTil: LocalDateTime
)

data class VedleggOpplastingResponse(
        val filnavn: String?,
        val storrelse: Long
)