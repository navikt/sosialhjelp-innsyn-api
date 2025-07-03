package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import org.springframework.stereotype.Repository

interface KlageRepository {

    fun getKlagerStatusSendt(): List<KlageRef>

    fun save(
        digisosId: UUID,
        vedtakId: UUID,
        klageId: UUID,
    )

    fun delete(klageId: UUID)
}

@Repository
class InMemoryKlageRepository : KlageRepository {
    override fun getKlagerStatusSendt(): List<KlageRef> = klagerStorage.filter { it.status == KlageStatus.SENDT }

    override fun save(digisosId: UUID, vedtakId: UUID, klageId: UUID) {
        klagerStorage.add(KlageRef(digisosId, vedtakId, klageId))
    }

    override fun delete(klageId: UUID) {
        klagerStorage.removeIf { it.klageId == klageId }
    }

    companion object {
        val klagerStorage = mutableListOf<KlageRef>()

        fun reset() { klagerStorage.clear() }
    }
}

data class KlageRef(
    val digisosId: UUID,
    val vedtakId: UUID,
    val klageId: UUID,
    val status: KlageStatus = KlageStatus.SENDT,
)

enum class KlageStatus {
    SENDT,
    MOTTATT,
}
