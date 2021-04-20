package no.nav.sosialhjelp.innsyn.service.saksstatus

import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SaksStatusResponse
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.domain.VedtaksfilUrl
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
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
            log.info("Fant ingen saker")
            return emptyList()
        }

        val responseList = model.saker.filter { it.saksStatus != SaksStatus.FEILREGISTRERT }.map { mapToResponse(it) }
        log.info("Hentet ${responseList.size} sak(er)")
        return responseList
    }

    private fun mapToResponse(sak: Sak): SaksStatusResponse {
        val saksStatus = hentStatusNavn(sak)
        val vedtakfilUrlList = when {
            sak.vedtak.isEmpty() -> null
            else -> sak.vedtak.map {
                log.info("Hentet url til vedtaksfil: ${it.vedtaksFilUrl}")
                VedtaksfilUrl(it.dato, it.vedtaksFilUrl)
            }
        }
        val skalViseVedtakInfoPanel = getSkalViseVedtakInfoPanel(sak)
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
        return sakHarVedtakslisteMedGjeldendeVedtakInnvilgetEllerDelvisInnvilget
    }

    companion object {
        private val log by logger()
    }
}
