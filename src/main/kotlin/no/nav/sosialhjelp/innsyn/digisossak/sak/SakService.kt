package no.nav.sosialhjelp.innsyn.digisossak.sak

import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import kotlin.collections.ifEmpty
import kotlin.getValue

@Component
class SakService(
    private val eventService: EventService,
    private val fiksService: FiksService,
) {
    private val log by logger()

    suspend fun hentSakForVedtak(
        fiksDigisosId: String,
        vedtakId: String,
    ): SakResponse {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)

        if (model.saker.isEmpty()) {
            throw NotFoundException("Ingen saker funnet for fiksDigisosId $fiksDigisosId")
        }

        val sak =
            model.saker.find { it.vedtak.any { vedtak -> vedtak.id == vedtakId } }
                ?: throw NotFoundException("Sak med vedtakId $vedtakId ikke funnet for fiksDigisosId $fiksDigisosId")

        val vedtakfilUrlList =
            sak.vedtak
                .map {
                    log.info("Hentet url til vedtaksfil: ${it.vedtaksFilUrl}")
                    FilUrl(it.dato, it.vedtaksFilUrl, it.id)
                }.ifEmpty { null }

        return SakResponse(
            sak.tittel ?: DEFAULT_SAK_TITTEL,
            vedtakfilUrlList,
            sak.vedtak.lastOrNull()?.utfall,
            model.soknadsmottaker?.navEnhetsnavn,
        )
    }
}
