package no.nav.sbl.sosialhjelpinnsynapi.soknadsstatus

import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(SoknadsStatusService::class.java)

@Component
class SoknadsStatusService(private val eventService: EventService) {

    fun hentSoknadsStatus(fiksDigisosId: String, token: String): SoknadsStatusResponse {
        val model = eventService.createModel(fiksDigisosId)
        val status = model.status
        if (status == null) {
            log.warn("SoknadsStatus kan ikke være null")
            throw RuntimeException("SoknadsStatus kan ikke være null")
        }
        log.info("Hentet nåværende søknadsstatus=${status.name} for $fiksDigisosId")
        return SoknadsStatusResponse(status)
    }
}