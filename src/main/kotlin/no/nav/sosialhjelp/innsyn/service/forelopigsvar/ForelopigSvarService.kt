package no.nav.sosialhjelp.innsyn.service.forelopigsvar

import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvar
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvarResponse
import no.nav.sosialhjelp.innsyn.event.EventService
import org.springframework.stereotype.Component

@Component
class ForelopigSvarService(
        private val fiksClient: FiksClient,
        private val eventService: EventService
) {

    fun hentForelopigSvar(fiksDigisosId: String, token: String): ForelopigSvarResponse {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        val forelopigSvarStatus: ForelopigSvar = model.forelopigSvar
        return ForelopigSvarResponse(forelopigSvarStatus.harMottattForelopigSvar, forelopigSvarStatus.link)
    }
}