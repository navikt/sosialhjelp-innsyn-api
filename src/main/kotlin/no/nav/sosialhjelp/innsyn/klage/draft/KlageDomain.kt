package no.nav.sosialhjelp.innsyn.klage.draft

import java.time.LocalDateTime
import java.util.UUID

data class KlageMetadata(
    val klageId: UUID,
    val digisosId: UUID,
    val vedtakFilreferanseId: UUID,
    val opprettet: LocalDateTime,
    val sendt: LocalDateTime?,
    val status: KlageStatus,
) {
    init {
        if (status == KlageStatus.SENDT || status == KlageStatus.MOTTATT_FSL) {
            requireNotNull(sendt) { "sendt må være satt når status er $status" }
        }
    }
}

data class Klage(
    val id: UUID,
    val begrunnelse: String?,
)

enum class KlageStatus {
    OPPRETTET,
    INNSENDING_FEILET,
    SENDT,
    MOTTATT_FSL,
    MANUELT_KVITTERT_UT,
}
