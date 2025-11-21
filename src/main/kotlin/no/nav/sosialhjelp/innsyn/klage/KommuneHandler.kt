package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneInfoClient
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class KommuneHandler(
    private val fiksClient: FiksClient,
    private val kommuneInfoClient: KommuneInfoClient,
    private val eventService: EventService,
) {
    fun getMottakerInfo(digisosId: UUID): Pair<String, Soknadsmottaker> {
        TODO("Not yet implemented")
    }

    fun validateKommuneConfig(kommunenummer: String) {
        TODO("Not yet implemented")
    }
}
