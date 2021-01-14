package no.nav.sbl.sosialhjelpinnsynapi.service.soknadsstatus

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.common.VIS_SOKNADEN
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.UrlResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneService
import no.nav.sbl.sosialhjelpinnsynapi.utils.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
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
            throw RuntimeException("SoknadsStatus er null på digisosId=$fiksDigisosId")
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
