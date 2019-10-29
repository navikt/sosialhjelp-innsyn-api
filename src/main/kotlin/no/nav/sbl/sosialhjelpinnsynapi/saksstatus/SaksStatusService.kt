package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedtaksfilUrl
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.stereotype.Component

const val DEFAULT_TITTEL: String = "Økonomisk sosialhjelp"

@Component
class SaksStatusService(private val eventService: EventService) {

    companion object {
        val log by logger()
    }

    fun hentSaksStatuser(fiksDigisosId: String, token: String): List<SaksStatusResponse> {
        val model = eventService.createModel(fiksDigisosId, token)

        if (model.saker.isEmpty()) {
            log.info("Fant ingen saker for $fiksDigisosId")
            return emptyList()
        }

        val responseList = model.saker.filter { it.saksStatus != SaksStatus.FEILREGISTRERT }.map { mapToResponse(it) }
        log.info("Hentet ${responseList.size} sak(er) for $fiksDigisosId")
        return responseList
    }

    private fun mapToResponse(sak: Sak): SaksStatusResponse {
        val saksStatus = hentStatusNavn(sak)
        val vedtakfilUrlList = when {
            sak.vedtak.isEmpty() -> null
            else -> sak.vedtak.map {
                VedtaksfilUrl(it.dato, it.vedtaksFilUrl)
            }
        }
        return SaksStatusResponse(sak.tittel ?: DEFAULT_TITTEL, saksStatus, vedtakfilUrlList)
    }

    private fun hentStatusNavn(sak: Sak): SaksStatus? {
        return when {
            sak.vedtak.isEmpty() -> sak.saksStatus ?: SaksStatus.UNDER_BEHANDLING
            else -> SaksStatus.FERDIGBEHANDLET
        }
    }
}