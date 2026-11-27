package no.nav.sosialhjelp.innsyn.klage.draft

import no.nav.sosialhjelp.innsyn.klage.KlageIkkeTilgjengeligException
import no.nav.sosialhjelp.innsyn.klage.KlageMetricsService
import no.nav.sosialhjelp.innsyn.klage.KlageTilgangService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class KlageDraftUseCaseHandler(
    private val kommuneService: KommuneService,
    private val klageTilgangService: KlageTilgangService,
    private val klageService: KlageDraftService,
    private val metricsService: KlageMetricsService,
) {
    suspend fun createKlage(
        digisosId: UUID,
        vedtakId: UUID,
    ): KlageMetadata {
        kommuneService.validerMottakOgInnsynForKommune(digisosId)
        if (!klageTilgangService.kanSendeKlage(digisosId)) {
            throw KlageIkkeTilgjengeligException("Klage er ikke tilgjengelig for digisosId=$digisosId")
        }

        return runCatching { klageService.createKlage(digisosId, vedtakId) }
            .onFailure { metricsService.registerKlageOpprettet(false) }
            .onSuccess { metricsService.registerKlageOpprettet(true) }
            .getOrThrow()
    }

    suspend fun getDraft(klageId: UUID): Pair<KlageMetadata, Klage>? = klageService.getDraft(klageId)

    suspend fun updateDraft(
        klageId: UUID,
        begrunnelse: String?,
    ) {
        klageService.updateDraft(klageId, begrunnelse ?: "")
    }

    suspend fun deleteDraft(klageId: UUID) {
        runCatching { klageService.deleteDraft(klageId) }
            .onSuccess { metricsService.registerKlageAvbrutt() }
            .getOrThrow()
    }

    suspend fun kanSendeKlage(fiksDigisosId: UUID): Boolean = klageTilgangService.kanSendeKlage(fiksDigisosId)
}
