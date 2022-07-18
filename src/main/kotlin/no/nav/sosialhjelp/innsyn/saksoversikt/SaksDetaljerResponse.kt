package no.nav.sosialhjelp.innsyn.saksoversikt

data class SaksDetaljerResponse(
    val fiksDigisosId: String,
    val soknadTittel: String,
    val status: String,
    val antallNyeOppgaver: Int?
)
