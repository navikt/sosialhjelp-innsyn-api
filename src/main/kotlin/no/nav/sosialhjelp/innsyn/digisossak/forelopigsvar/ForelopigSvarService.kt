package no.nav.sosialhjelp.innsyn.digisossak.forelopigsvar

import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvar
import no.nav.sosialhjelp.innsyn.event.EventService
import org.springframework.stereotype.Component

@Component
class ForelopigSvarService(
    private val fiksService: FiksService,
    private val eventService: EventService,
) {
    suspend fun hentForelopigSvar(
        fiksDigisosId: String,
        token: Token,
    ): ForelopigSvarResponse {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        val forelopigSvarStatus: ForelopigSvar = model.forelopigSvar
        return ForelopigSvarResponse(forelopigSvarStatus.harMottattForelopigSvar, forelopigSvarStatus.link)
    }
}
