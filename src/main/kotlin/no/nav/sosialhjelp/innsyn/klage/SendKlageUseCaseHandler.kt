package no.nav.sosialhjelp.innsyn.klage

import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.klage.draft.Klage
import no.nav.sosialhjelp.innsyn.klage.draft.KlageDraftService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SendKlageUseCaseHandler(
    private val klageDraftService: KlageDraftService,
    private val metricsService: KlageMetricsService,
    private val klageService: KlageService,
    private val jsonKlageGenerator: JsonKlageGenerator,
) {
    suspend fun sendKlage(
        digisosId: UUID,
        klageId: UUID,
    ) {
        val (metadata, klage) = klageDraftService.getDraft(klageId) ?: error("Klage med klageId=$klageId finnes ikke")

        val vedleggJson = createVedleggJson(klageId)
        if (klage.begrunnelse.isNullOrBlank() && vedleggJson.hasNoFiles()) {
            throw KlageValidationException("Klage mangler både tekst og vedlegg", KlageValidationException.Reason.MANGLER_TEKST_OG_VEDLEGG)
        }

        val jsonKlage = createKlageJson(digisosId, klage, metadata.vedtakFilreferanseId)
        val klagePdf = KlagePdfGenerator.generatePdf(jsonKlage, vedleggJson)

        runCatching { doSendKlage(jsonKlage, klagePdf, vedleggJson) }
            .onSuccess { klageDraftService.setStatusSendt(klageId) }
            .getOrElse {
                klageDraftService.setStatusFeilet(klageId)
                throw it
            }
    }

    private suspend fun createVedleggJson(klageId: UUID): JsonVedleggSpesifikasjon =
        runCatching { klageService.fetchJsonVedleggSpec(klageId) }
            .onFailure { metricsService.registerKlageSent(success = false) }
            .getOrElse { throw KlageIkkeSentException("Kunne ikke hente vedlegg for klageId=$klageId", it) }

    private suspend fun createKlageJson(
        digisosId: UUID,
        klage: Klage,
        vedtakId: UUID,
    ): JsonKlage =
        runCatching { jsonKlageGenerator.generateJsonKlage(klage, vedtakId, digisosId) }
            .onFailure { metricsService.registerKlageSent(success = false) }
            .getOrElse { throw KlageIkkeSentException("Kunne ikke generere JSON for klageId=${klage.id}", it) }

    private suspend fun doSendKlage(
        jsonKlage: JsonKlage,
        klagePdf: FilForOpplasting,
        vedleggJson: JsonVedleggSpesifikasjon,
    ) {
        runCatching { klageService.sendKlage(jsonKlage, klagePdf, vedleggJson) }
            .onSuccess {
                logger.info(
                    "Klage sendt til kommunenummer=${jsonKlage.mottaker.kommunenummer}, enhetsnummer=${jsonKlage.mottaker.enhetsnummer}",
                )
                metricsService.registerKlageSent(success = true)
            }.onFailure {
                logger.error("Kunne ikke sende klage for digisosId=${jsonKlage.digisosId}, klageId=${jsonKlage.klageId}", it)
                metricsService.registerKlageSent(success = false)
            }.getOrElse {
                throw KlageIkkeSentException("Kunne ikke sende klage", it)
            }
    }

    companion object {
        private val logger by logger()
    }
}
