package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonKlage
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

interface KlageService {
    fun opprett(
        digisosId: UUID,
        vedtakIds: List<UUID>,
    ): UUID

    fun oppdater(klageId: UUID, klageTekst: String): Klage

    fun sendKlage(klageId: UUID,)

    fun hentKlager(fiksDigisosId: String,): List<Klage>

    fun hentKlageDraft(klageId: UUID): Klage

    fun hentKlage(fiksDigisosId: UUID, klageId: UUID): Klage
}

@Service
class KlageServiceImpl(
    private val klageRepository: KlageRepository,
    private val klageClient: KlageClient
) : KlageService {

    override fun opprett(digisosId: UUID, vedtakIds: List<UUID>): UUID =
        Klage(
            fiksDigisosId = digisosId,
            vedtakIds = vedtakIds,
        )
            .let { klageRepository.save(it) }
            .id

    override fun oppdater(klageId: UUID, klageTekst: String): Klage =
        klageRepository.findByIdOrNull(klageId)
            ?.run { copy(tekst = klageTekst,) }
            ?.let { klageRepository.save(it) }
            ?: throw IllegalArgumentException("Klage $klageId not found")


    override fun sendKlage(klageId: UUID) {
        val klage = klageRepository.findByIdOrNull(klageId)
            ?: throw IllegalArgumentException("Klage $klageId not found")

        runCatching { klageClient.send(klage) }
            .onFailure { logger.error("Failed to send klage $klageId", it) }
            .onSuccess { klageRepository.delete(klage) }
            .getOrThrow()
    }

    override fun hentKlager(fiksDigisosId: String): List<Klage> =
        klageClient.hentKlager(fiksDigisosId).map { it.toKlage() }

    override fun hentKlageDraft(klageId: UUID): Klage = klageRepository.findByIdOrNull(klageId)
        ?: throw IllegalArgumentException("Klage $klageId not found")

    override fun hentKlage(fiksDigisosId: UUID, klageId: UUID): Klage =
        klageClient.hentKlage(fiksDigisosId, klageId).toKlage()

    companion object {
        private val logger by logger()
    }
}

private fun JsonKlage.toKlage(): Klage {
    TODO("Not yet implemented")
}

data class KlageInput(
    val tekst: String,
)

data class Klage(
    val id: UUID = UUID.randomUUID(),
    val tekst: String? = null,
    val fiksDigisosId: UUID,
    val vedtakIds: List<UUID>,
    val status: KlageStatus? = null,
    val utfall: KlageUtfall? = null,
)

enum class KlageStatus {
    SENDT,
    MOTTATT,
    UNDER_BEHANDLING,
    FERDIG_BEHANDLET,
    HOS_STATSFORVALTER,
}

enum class KlageUtfall {
    NYTT_VEDTAK,
    AVVIST,
}
