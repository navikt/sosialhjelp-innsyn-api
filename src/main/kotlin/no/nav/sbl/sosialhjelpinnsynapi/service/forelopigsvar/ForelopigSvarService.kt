package no.nav.sbl.sosialhjelpinnsynapi.service.forelopigsvar

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.ForelopigSvar
import no.nav.sbl.sosialhjelpinnsynapi.domain.ForelopigSvarResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
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