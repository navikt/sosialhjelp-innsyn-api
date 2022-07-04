package no.nav.sosialhjelp.innsyn.service.soknadsstatus

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.event.VIS_SOKNADEN
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SoknadsStatusService(
    private val eventService: EventService,
    private val fiksClient: FiksClient,
    private val kommuneService: KommuneService,
    private val clientProperties: ClientProperties,
) {

    fun hentSoknadsStatus(fiksDigisosId: String, token: String): UtvidetSoknadsStatus {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        val status = model.status
        if (status == null) {
            log.warn("SoknadsStatus er null")
            throw RuntimeException("SoknadsStatus er null")
        }
        log.info("Hentet nåværende søknadsstatus=${status.name}")
        val erInnsynDeaktivertForKommune = kommuneService.erInnsynDeaktivertForKommune(fiksDigisosId, token)
        val dokumentlagerId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId
        return UtvidetSoknadsStatus(
            status = status,
            tidspunktSendt = model.tidspunktSendt,
            navKontor = if (erInnsynDeaktivertForKommune) model.soknadsmottaker?.navEnhetsnavn else null,
            soknadUrl = if (erInnsynDeaktivertForKommune && dokumentlagerId != null) UrlResponse(VIS_SOKNADEN, hentDokumentlagerUrl(clientProperties, dokumentlagerId)) else null
        )
    }

    data class UtvidetSoknadsStatus(
        val status: SoknadsStatus,
        val tidspunktSendt: LocalDateTime?,
        val navKontor: String?,
        val soknadUrl: UrlResponse?
    )

    companion object {
        private val log by logger()
    }
}
