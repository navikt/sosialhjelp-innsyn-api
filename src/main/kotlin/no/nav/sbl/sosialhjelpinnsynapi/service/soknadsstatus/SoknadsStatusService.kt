package no.nav.sbl.sosialhjelpinnsynapi.service.soknadsstatus

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.stereotype.Component


@Component
class SoknadsStatusService(
        private val eventService: EventService,
        private val fiksClient: FiksClient
) {

    fun hentSoknadsStatus(fiksDigisosId: String, token: String): SoknadsStatusResponse {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        val status = model.status
        if (status == null) {
            log.warn("SoknadsStatus er null på digisosId=$fiksDigisosId")
            throw RuntimeException("SoknadsStatus er null på digisosId=$fiksDigisosId")
        }
        log.info("Hentet nåværende søknadsstatus=${status.name} for digisosId=$fiksDigisosId")
        return SoknadsStatusResponse(status, model.tidspunktSendt)
    }

    companion object {
        private val log by logger()
    }
}
