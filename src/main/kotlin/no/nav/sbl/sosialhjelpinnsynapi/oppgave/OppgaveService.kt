package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(OppgaveService::class.java)

@Component
class OppgaveService(private val eventService: EventService) {

    fun hentOppgaver(fiksDigisosId: String, token: String): List<OppgaveResponse> {
        val model = eventService.createModel(fiksDigisosId)

        if (model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val oppgaveResponseList = model.oppgaver.sortedBy { it.innsendelsesfrist }.map { OppgaveResponse(it.innsendelsesfrist.toString(), it.tittel, it.tilleggsinfo) }
        log.info("Hentet ${oppgaveResponseList.size} oppgaver for fiksDigisosId=$fiksDigisosId")
        return oppgaveResponseList
    }
}