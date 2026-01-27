package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.UUID

@Component
class KlageUseCaseHandler(
    private val kommuneHandler: KommuneHandler,
    private val klageService: KlageService,
    private val jsonKlageGenerator: JsonKlageGenerator,
    meterRegistry: MeterRegistry,
) {
    private val klageMetricsService = KlageMetricsService(meterRegistry)

    suspend fun sendKlage(
        digisosId: UUID,
        input: KlageInput,
    ): KlageDto {
        // TODO Trengs det å validere at kommune har innsyn, etc?!

        val jsonKlage = jsonKlageGenerator.generateJsonKlage(
            fiksDigisosId = digisosId,
            input = input
        )

        val klagePdf = KlagePdfGenerator.generatePdf(jsonKlage)

        runCatching { klageService.sendKlage(jsonKlage, klagePdf) }
            .onSuccess { klageMetricsService.registerSent() }
            .onFailure { klageMetricsService.registerSendError() }
            .getOrElse { throw KlageIkkeSentException("Kunne ikke sende klage", it) }

        TODO("Returner KlageDto")
    }

    fun hentAlleKlager(digisosId: UUID): List<KlageRef> {
        TODO("Returner KlageRefs")
    }

    fun hentKlage(
        digisosId: UUID,
        klageId: UUID,
    ): KlageDto? {
        TODO("Hente klage for klageId")
    }

    suspend fun lastOppVedlegg(
        digisosId: UUID,
        navEksternRefId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentsForKlage = klageService.lastOppVedlegg(digisosId, navEksternRefId, rawFiles)

    fun sendEttersendelse(
        digisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
    ) {
        TODO("Sende ettersendelse")
    }
}

class KlageIkkeSentException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
