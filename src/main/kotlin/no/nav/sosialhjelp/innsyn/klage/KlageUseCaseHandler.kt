package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.UUID

@Component
class KlageUseCaseHandler(
    private val klageService: KlageService,
    private val jsonKlageGenerator: JsonKlageGenerator,
    private val kommuneService: KommuneService,
    private val klageTilgangService: KlageTilgangService,
    private val klageMetricsService: KlageMetricsService,
) {
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
                .onFailure { klageMetricsService.registerKlageSent(kommunenummer = null, success = false) }
                .getOrElse { throw KlageIkkeSentException("Kunne ikke sende klage", it) }

        if (input.tekst.isBlank() && vedleggJson.hasNoFiles()) {
            throw KlageValidationException("Klage mangler både tekst og vedlegg", KlageValidationException.Reason.MANGLER_TEKST_OG_VEDLEGG)
        }

        val jsonKlage =
            jsonKlageGenerator.generateJsonKlage(
                fiksDigisosId = digisosId,
                input = input,
            )
        val klagePdf = KlagePdfGenerator.generatePdf(jsonKlage)

        runCatching { klageService.sendKlage(jsonKlage, klagePdf, vedleggJson) }
            .onSuccess { klageMetricsService.registerKlageSent(jsonKlage.mottaker.kommunenummer, success = true) }
            .onFailure { klageMetricsService.registerKlageSent(jsonKlage.mottaker.kommunenummer, success = false) }
            .getOrElse { throw KlageIkkeSentException("Kunne ikke sende klage", it) }

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

    suspend fun lastOppVedlegg(
        digisosId: UUID,
        navEksternRefId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentsForKlage = klageService.lastOppVedlegg(digisosId, navEksternRefId, rawFiles)

    suspend fun sendEttersendelse(
        digisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
    ) {
        kommuneService.validerMottakOgInnsynForKommune(digisosId)
        runCatching { klageService.sendEttersendelse(digisosId, klageId, ettersendelseId) }
            .onSuccess { klageMetricsService.registerEttersendelseSent(success = true) }
            .onFailure { klageMetricsService.registerEttersendelseSent(success = false) }
            .getOrThrow()
    }
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
    }
}
