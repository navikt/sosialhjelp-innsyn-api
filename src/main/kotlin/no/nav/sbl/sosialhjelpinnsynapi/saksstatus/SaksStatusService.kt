package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallEllerSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

const val DEFAULT_TITTEL: String = "Saken"

private val log = LoggerFactory.getLogger(SaksStatusService::class.java)

@Component
class SaksStatusService(private val eventService: EventService) {

    /* TODO:
        - Skal IKKE_INNSYN filtreres vekk i backend eller frontend?
     */
    fun hentSaksStatuser(fiksDigisosId: String, token: String): List<SaksStatusResponse> {
        val model = eventService.createModel(fiksDigisosId, token)

        if (model.saker.isEmpty()) {
            log.info("Fant ingen saker for $fiksDigisosId")
            return emptyList()
        }

        val responseList = model.saker.map { mapToResponse(it) }
        log.info("Hentet ${responseList.size} sak(er) for $fiksDigisosId")
        return responseList
    }

    private fun mapToResponse(sak: Sak): SaksStatusResponse {
        val utfallEllerStatus = hentStatusNavn(sak)
        val vedtakfilUrlList = when {
            sak.vedtak.isEmpty() -> null
            else -> sak.vedtak.map { it.vedtaksFilUrl }
        }
        return SaksStatusResponse(sak.tittel, utfallEllerStatus, vedtakfilUrlList)
    }

    private fun hentStatusNavn(sak: Sak): UtfallEllerSaksStatus? {
        return when {
            sak.vedtak.size > 1 -> null
            else -> UtfallEllerSaksStatus.valueOf(sak.vedtak.firstOrNull()?.utfall?.name ?: sak.saksStatus.name)
        }
    }
}