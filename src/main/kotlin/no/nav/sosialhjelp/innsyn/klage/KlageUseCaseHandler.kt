package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.MeterRegistry
import java.util.UUID
import no.nav.sosialhjelp.innsyn.prometheus.PrometheusMetricsService
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class KlageUseCaseHandler(
    private val kommuneHandler: KommuneHandler,
    private val klageService: KlageService,
    meterRegistry: MeterRegistry,
) {
    private val klageMetricsService = KlageMetricsService(meterRegistry)

    suspend fun sendKlage(digisosId: UUID, input: KlageInput): KlageDto {


        val (kommunenummer, navEnhet) = kommuneHandler.getMottakerInfo(digisosId)
        kommuneHandler.validateKommuneConfig(kommunenummer)

        runCatching { klageService.sendKlage(digisosId, input, kommunenummer, navEnhet) }
            .onSuccess { klageMetricsService.registerSent() }
            .onFailure { klageMetricsService.registerSendError() }
            .getOrElse { throw KlageIkkeSentException("Kunne ikke sende klage", it) }
    }

    fun hentAlleKlager(digisosId: UUID): List<KlageRef> {

    }

    fun hentKlage(digisosId: UUID, klageId: UUID): KlageDto? {

    }

    suspend fun lastOppVedlegg(
        digisosId: UUID,
        navEksternRefId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentsForKlage {
        return klageService.lastOppVedlegg(digisosId, navEksternRefId, rawFiles)
    }

    fun sendEttersendelse(
        digisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
    ) {

    }
}

class KlageIkkeSentException(message: String, cause: Throwable): RuntimeException(message, cause)

private class KlageMetricsService(meterRegistry: MeterRegistry): PrometheusMetricsService(meterRegistry) {

    private val klageSentCounter = createCounter("klage_sent_counter")
    private val sendKlageErrorCounter = createCounter("send_klage_error_counter")

    fun registerSent() = klageSentCounter.increment()
    fun registerSendError() = sendKlageErrorCounter.increment()
}