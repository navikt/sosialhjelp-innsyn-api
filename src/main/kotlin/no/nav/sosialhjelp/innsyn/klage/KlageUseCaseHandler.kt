package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class KlageUseCaseHandler(
    private val klageService: KlageService,
    private val jsonKlageGenerator: JsonKlageGenerator,
    private val kommuneService: KommuneService,
    private val klageTilgangService: KlageTilgangService,
    private val klageMetricsService: KlageMetricsService,
) {
    private val log by logger()

    suspend fun sendKlage(
        digisosId: UUID,
        input: KlageInput,
    ): KlageDto {
        kommuneService.validerMottakOgInnsynForKommune(digisosId)
        if (!klageTilgangService.kanSendeKlage(digisosId)) {
            throw KlageIkkeTilgjengeligException("Klage er ikke tilgjengelig for denne Nav-enheten")
        }

        val vedleggJson =
            runCatching { klageService.fetchJsonVedleggSpec(input.klageId) }
                .onFailure { klageMetricsService.registerKlageSent(success = false) }
                .getOrElse { throw KlageIkkeSentException("Kunne ikke sende klage", it) }

        if (input.tekst.isBlank() && vedleggJson.hasNoFiles()) {
            throw KlageValidationException("Klage mangler både tekst og vedlegg", KlageValidationException.Reason.MANGLER_TEKST_OG_VEDLEGG)
        }

        val jsonKlage =
            jsonKlageGenerator.generateJsonKlage(
                fiksDigisosId = digisosId,
                input = input,
            )
        val klagePdf = KlagePdfGenerator.generatePdf(jsonKlage, vedleggJson)

        runCatching {
            klageService.sendKlage(jsonKlage, klagePdf, vedleggJson)
        }.onSuccess {
            log.info("Klage sendt til kommunenummer=${jsonKlage.mottaker.kommunenummer}, enhetsnummer=${jsonKlage.mottaker.enhetsnummer}")
            klageMetricsService.registerKlageSent(success = true)
        }.onFailure {
            log.warn("Kunne ikke sende klage for digisosId=$digisosId, klageId=${input.klageId}", it)
            klageMetricsService.registerKlageSent(success = false)
        }.getOrElse {
            throw KlageIkkeSentException("Kunne ikke sende klage", it)
        }

        return hentKlage(digisosId, input.klageId) ?: error("Fant ikke innsendt klage")
    }

    suspend fun kanSendeKlage(digisosId: UUID): Boolean = klageTilgangService.kanSendeKlage(digisosId)

    suspend fun hentAlleKlager(digisosId: UUID): List<KlageRef> = klageService.hentKlager(digisosId)

    suspend fun hentKlage(
        digisosId: UUID,
        klageId: UUID,
    ): KlageDto? = klageService.hentKlage(digisosId, klageId)

    suspend fun hentKlageForVedtak(
        digisosId: UUID,
        vedtakId: UUID,
    ): KlageDto? = klageService.hentKlageForVedtak(digisosId, vedtakId)
}

class KlageIkkeSentException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

class KlageValidationException(
    message: String,
    val reason: Reason,
) : RuntimeException(message) {
    enum class Reason {
        TEKST_FOR_LANG,
        MANGLER_TEKST_OG_VEDLEGG,
        KLAGEFRIST_UTLOPT,
    }
}
