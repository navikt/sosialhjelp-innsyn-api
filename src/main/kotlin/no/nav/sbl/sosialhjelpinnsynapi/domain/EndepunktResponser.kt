package no.nav.sbl.sosialhjelpinnsynapi.domain

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class SoknadsStatusResponse(
        val status: SoknadsStatus
)

data class SaksStatusResponse(
        val tittel: String,
        val status: SaksStatus?,
        val vedtaksfilUrlList: List<VedtaksfilUrl>?
)

data class VedtaksfilUrl(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val dato: LocalDate?,
        val vedtaksfilUrl: String
)
data class HendelseResponse(
        val tidspunkt: String,
        val beskrivelse: String,
        val filUrl: String?
)

data class OppgaveResponse(
        val innsendelsesfrist: String?,
        val dokumenttype: String,
        val tilleggsinformasjon: String?,
        val erFraInnsyn: Boolean
)

data class UtbetalingerResponse(
        val ar: Int,
        val maned: String,
        val sum: Double,
        val utbetalinger: List<ManedUtbetaling>
)

data class ManedUtbetaling(
        val tittel: String?,
        val belop: Double,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val utbetalingsdato: LocalDate?,
        val status: String,
        val fiksDigisosId: String
)

data class VedleggResponse(
        val filnavn: String,
        val storrelse: Long,
        val url: String,
        val type: String,
        val tilleggsinfo: String?,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        val datoLagtTil: LocalDateTime
)

data class VedleggOpplastingResponse(
        val filnavn: String?,
        val status: String
)

data class SaksListeResponse(
        val fiksDigisosId: String,
        val soknadTittel: String,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        val sistOppdatert: Date,
        val kilde: String
)

data class SaksDetaljerResponse(
        val fiksDigisosId: String,
        val soknadTittel: String,
        val status: String,
        val antallNyeOppgaver: Int?
)

data class ForelopigSvarResponse(
        val harMottattForelopigSvar: Boolean,
        val link: String?
)