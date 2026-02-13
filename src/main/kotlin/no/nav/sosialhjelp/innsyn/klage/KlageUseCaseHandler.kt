package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.MeterRegistry
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
    meterRegistry: MeterRegistry,
) {
    private val klageMetricsService = KlageMetricsService(meterRegistry)

    suspend fun sendKlage(
        digisosId: UUID,
        input: KlageInput,
    ): KlageDto {
        // TODO Trengs det å validere at kommune har innsyn, etc?!
        kommuneService.validerMottakOgInnsynForKommune(digisosId)

        val jsonKlage =
            jsonKlageGenerator.generateJsonKlage(
                fiksDigisosId = digisosId,
                input = input,
            )

        val klagePdf = KlagePdfGenerator.generatePdf(jsonKlage)

        runCatching { klageService.sendKlage(jsonKlage, klagePdf) }
            .onSuccess { klageMetricsService.registerSent() }
            .onFailure { klageMetricsService.registerSendError() }
            .getOrElse { throw KlageIkkeSentException("Kunne ikke sende klage", it) }

        return hentKlage(digisosId, input.klageId) ?: error("Fant ikke innsendt klage")
    }

    suspend fun hentAlleKlager(digisosId: UUID): List<KlageRef> = klageService.hentKlager(digisosId)

    suspend fun hentKlage(
        digisosId: UUID,
        klageId: UUID,
    ): KlageDto? = klageService.hentKlage(digisosId, klageId)

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
        klageService.sendEttersendelse(digisosId, klageId, ettersendelseId)
    }
}

class KlageIkkeSentException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
