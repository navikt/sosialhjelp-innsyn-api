package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallVedtak
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedtaksfilUrl
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.stereotype.Component

const val DEFAULT_TITTEL: String = "Økonomisk sosialhjelp"

@Component
class SaksStatusService(
        private val eventService: EventService,
        private val fiksClient: FiksClient
) {

    fun hentSaksStatuser(fiksDigisosId: String, token: String): List<SaksStatusResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)

        if (model.saker.isEmpty()) {
            log.info("Fant ingen saker for digisosId=$fiksDigisosId")
            return emptyList()
        }

        val responseList = model.saker.filter { it.saksStatus != SaksStatus.FEILREGISTRERT }.map { mapToResponse(it) }
        log.info("Hentet ${responseList.size} sak(er) for digisosId=$fiksDigisosId")
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
        val skalViseVedtakInfoPanel = getSkalViseVedtakInfoPanel(sak);
        return SaksStatusResponse(sak.tittel ?: DEFAULT_TITTEL, saksStatus, skalViseVedtakInfoPanel, vedtakfilUrlList)
    }

    private fun hentStatusNavn(sak: Sak): SaksStatus? {
        return when {
            sak.vedtak.isEmpty() -> sak.saksStatus ?: SaksStatus.UNDER_BEHANDLING
            else -> SaksStatus.FERDIGBEHANDLET
        }
    }

    fun getSkalViseVedtakInfoPanel(sak: Sak): Boolean {
        var sakHarVedtakslisteMedGjeldendeVedtakInnvilgetEllerDelvisInnvilget = false
        for (vedtak in sak.vedtak) {
            when {
                vedtak.utfall == UtfallVedtak.DELVIS_INNVILGET || vedtak.utfall == UtfallVedtak.INNVILGET -> sakHarVedtakslisteMedGjeldendeVedtakInnvilgetEllerDelvisInnvilget = true
                vedtak.utfall == UtfallVedtak.AVSLATT || vedtak.utfall == UtfallVedtak.AVVIST -> sakHarVedtakslisteMedGjeldendeVedtakInnvilgetEllerDelvisInnvilget = false
            }
        }
        return sakHarVedtakslisteMedGjeldendeVedtakInnvilgetEllerDelvisInnvilget;
    }

    companion object {
        private val log by logger()
    }
}
