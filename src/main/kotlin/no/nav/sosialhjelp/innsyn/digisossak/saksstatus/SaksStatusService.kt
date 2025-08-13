package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

const val DEFAULT_SAK_TITTEL = "default_sak_tittel"

@Component
class SaksStatusService(
    private val eventService: EventService,
    private val fiksClient: FiksClient,
) {
    private val log by logger()

    suspend fun hentSaksStatuser(
        fiksDigisosId: String,
        token: Token,
    ): List<SaksStatusResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId)
        val model = eventService.createModel(digisosSak)

        if (model.saker.isEmpty()) {
            log.info("Fant ingen saker")
            return emptyList()
        }

        val responseList = model.saker.filter { it.saksStatus != SaksStatus.FEILREGISTRERT }.map { mapToResponse(it) }
        log.info("Hentet ${responseList.size} sak(er) ${responseList.map { it.status?.name ?: "UKJENT_STATUS" }}")
        return responseList
    }

    private fun mapToResponse(sak: Sak): SaksStatusResponse {
        val saksStatus =
            if (sak.vedtak.isEmpty()) {
                sak.saksStatus ?: SaksStatus.UNDER_BEHANDLING
            } else {
                SaksStatus.FERDIGBEHANDLET
            }
        val vedtakfilUrlList =
            sak.vedtak
                .map {
                    log.info("Hentet url til vedtaksfil: ${it.vedtaksFilUrl}")
                    FilUrl(it.dato, it.vedtaksFilUrl, it.id)
                }.ifEmpty { null }
        val skalViseVedtakInfoPanel = getSkalViseVedtakInfoPanel(sak)
        return SaksStatusResponse(sak.tittel ?: DEFAULT_SAK_TITTEL, saksStatus, skalViseVedtakInfoPanel, vedtakfilUrlList)
    }

    fun getSkalViseVedtakInfoPanel(sak: Sak): Boolean =
        sak.vedtak.lastOrNull()?.let { it.utfall in listOf(UtfallVedtak.DELVIS_INNVILGET, UtfallVedtak.INNVILGET) } ?: false
}
