package no.nav.sosialhjelp.innsyn.saksoversikt

import java.time.LocalDateTime

data class SaksListeResponse(
    val fiksDigisosId: String,
    val soknadTittel: String,
    val sistOppdatert: LocalDateTime,
    val kommunenummer: String?,
    val soknadOpprettet: LocalDateTime?,
    val isPapirSoknad: Boolean,
)
