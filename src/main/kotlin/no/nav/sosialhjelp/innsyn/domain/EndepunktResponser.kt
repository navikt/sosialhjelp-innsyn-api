package no.nav.sosialhjelp.innsyn.domain

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import java.time.LocalDate
import java.util.Date

data class UtbetalingerResponse(
    val ar: Int,
    val maned: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val foersteIManeden: LocalDate,
    val utbetalinger: List<ManedUtbetaling>
)

data class ManedUtbetaling(
    val tittel: String,
    val belop: Double,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val utbetalingsdato: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val forfallsdato: LocalDate?,
    val status: String,
    val fiksDigisosId: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate?,
    val mottaker: String?,
    val annenMottaker: Boolean,
    val kontonummer: String?,
    val utbetalingsmetode: String?,
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

data class OrginalJsonSoknadResponse(
    val jsonSoknad: JsonSoknad
)

data class OrginalSoknadPdfLinkResponse(
    val orginalSoknadPdfLink: String
)
