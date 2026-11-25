package no.nav.sosialhjelp.innsyn.klage.database

import java.time.LocalDateTime
import java.util.UUID

data class KlageMetadata(
    val klageId: UUID,
    val digisosId: String,
    val vedtakFilreferanseId: String,
    val opprettet: LocalDateTime,
    val sendt: LocalDateTime?,
    val status: String,
)

data class Klage(
    val id: UUID,
    val begrunnelse: String?,
)
