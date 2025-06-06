package no.nav.sosialhjelp.innsyn.saksoversikt

import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus

data class SaksDetaljerResponse(
    val fiksDigisosId: String,
    val soknadTittel: String,
    val status: SoknadsStatus,
    val antallNyeOppgaver: Int?,
    val dokumentasjonEtterspurt: Boolean,
    val vilkar: Boolean,
    val dokuemntasjonkrav: Boolean,
)
