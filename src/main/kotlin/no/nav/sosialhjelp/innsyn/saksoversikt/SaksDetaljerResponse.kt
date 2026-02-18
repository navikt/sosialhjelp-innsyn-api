package no.nav.sosialhjelp.innsyn.saksoversikt

import no.nav.sosialhjelp.innsyn.domain.ForelopigSvar
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class SaksDetaljerResponse(
    val fiksDigisosId: String,
    val soknadTittel: String,
    val status: SoknadsStatus,
    val antallNyeOppgaver: Int?,
    val dokumentasjonEtterspurt: Boolean,
    val dokumentasjonkrav: Boolean,
    val vilkar: Boolean,
    val forelopigSvar: ForelopigSvar,
    val saker: List<Sak> = emptyList(),
    val forsteOppgaveFrist: LocalDate?,
    val mottattTidspunkt: LocalDateTime?,
    val sisteDokumentasjonKravFrist: LocalDate?,
    val soknadOpprettet: LocalDateTime?,
    val sistOppdatert: LocalDateTime,
) {
    data class Sak(
        val antallVedtak: Int,
        val status: SaksStatus,
    )
}
