package no.nav.sbl.sosialhjelpinnsynapi.forelopigSvarService

import no.nav.sbl.sosialhjelpinnsynapi.domain.ForelopigSvar
import no.nav.sbl.sosialhjelpinnsynapi.domain.ForelopigSvarResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.springframework.stereotype.Component

@Component
class ForelopigSvarService(private val eventService: EventService){

    fun hentForelopigSvar(fiksDigisosId: String, token: String): ForelopigSvarResponse{
        val model = eventService.createModel(fiksDigisosId, token)
        val forelopigSvarStatus: ForelopigSvar = model.forelopigSvar
        return ForelopigSvarResponse(forelopigSvarStatus.harMottattForelopigSvar, forelopigSvarStatus.link)
    }
}