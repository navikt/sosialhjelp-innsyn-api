package no.nav.sosialhjelp.innsyn.klage.draft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil
import no.nav.sosialhjelp.innsyn.klage.VedtakNotValidException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class KlageDraftService(
    private val fiksService: FiksService,
    private val eventService: EventService,
    private val klageRepository: KlageRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    suspend fun createKlage(
        digisosId: UUID,
        vedtakId: UUID,
    ): KlageMetadata {
        val digisosSak = fiksService.getSoknad(digisosId.toString())
        val model = eventService.createModel(digisosSak)
        if (model.saker.flatMap { it.vedtak }.none { it.id == vedtakId.toString() }) {
            throw VedtakNotValidException("Fant ikke vedtak med vedtaksreferanse $vedtakId")
        }

        return runInTransaction {
            val klageId = klageRepository.createKlageMetadata(digisosId, vedtakId)
            klageRepository.createKlage(klageId)
            klageRepository.getMetadata(klageId) ?: error("Fant ikke opprettet klage")
        }
    }

    suspend fun getDraft(klageId: UUID): Pair<KlageMetadata, Klage>? =
        runQuery {
            val metadata = klageRepository.getMetadata(klageId)
            val klage = klageRepository.getKlage(klageId)

            if (metadata != null && klage != null) {
                Pair(metadata, klage)
            } else {
                null
            }
        }

    suspend fun updateDraft(
        klageId: UUID,
        begrunnelse: String,
    ) = runQuery { klageRepository.updateKlage(klageId, begrunnelse) }

    suspend fun deleteDraft(klageId: UUID) {
        // TODO UploadClient?

        klageRepository.delete(klageId)
    }

    suspend fun deleteKlage(klageId: UUID) {
        klageRepository.deleteKlage(klageId)
    }

    suspend fun setStatusSendt(klageId: UUID) =
        runQuery {
            klageRepository.markAsSent(klageId, TimestampUtil.nowWithMillis())
        }

    suspend fun setStatusFeilet(klageId: UUID) =
        runQuery {
            klageRepository.updateStatus(klageId, KlageStatus.INNSENDING_FEILET)
        }

    // TODO MDC context?
    private suspend fun <T> runInTransaction(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transactionTemplate.execute { block.invoke() } ?: error("Transaksjon feilet")
        }

    private suspend fun <T> runQuery(block: () -> T): T = withContext(Dispatchers.IO) { block.invoke() }
}
