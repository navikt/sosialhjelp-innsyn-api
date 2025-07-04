package no.nav.sosialhjelp.innsyn.klage

import org.springframework.stereotype.Component
import java.util.UUID

interface FiksKlageClient {
    suspend fun sendKlage(
        klageId: UUID,
        klage: Klage,
    )

    suspend fun hentKlager(digisosId: UUID): List<Klage>
}

@Component
class LocalFiksKlageClient : FiksKlageClient {
    override suspend fun sendKlage(
        klageId: UUID,
        klage: Klage,
    ) {
        klageStorage[klageId] = klage
    }

    override suspend fun hentKlager(digisosId: UUID): List<Klage> = klageStorage.values.filter { it.digisosId == digisosId }

    companion object {
        val klageStorage = mutableMapOf<UUID, Klage>()
    }
}

data class Klage(
    val digisosId: UUID,
    val klageId: UUID,
    val klageTekst: String,
    val vedtakId: UUID,
    val status: KlageStatus = KlageStatus.SENDT,
)
