package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HendelseService(private val eventService: EventService) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    fun hentHendelser(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val model = eventService.createModel(fiksDigisosId)

        val responseList = model.historikk.map { HendelseResponse(it.tidspunkt.toString(), it.tittel, it.url) }
        log.info("Hentet historikk for fiksDigisosId=$fiksDigisosId")
        return responseList
    }
}