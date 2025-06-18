package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.event.EventService
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
    suspend fun hentSoknadsStatus(
        fiksDigisosId: String,
        token: Token,
        fnr: String,
    ): UtvidetSoknadsStatus {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        val model = eventService.createModel(digisosSak, token)
        val status = model.status

        log.info("Hentet nåværende søknadsstatus=${status.name}")
        val erInnsynDeaktivertForKommune = kommuneService.erInnsynDeaktivertForKommune(fiksDigisosId, token)
        val dokumentlagerId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId

        return UtvidetSoknadsStatus(
            status = status,
            tidspunktSendt = model.tidspunktSendt,
            navKontor = model.soknadsmottaker?.navEnhetsnavn?.takeIf { erInnsynDeaktivertForKommune },
            soknadUrl =
                dokumentlagerId
                    ?.let {
                        UrlResponse(
                            HendelseTekstType.VIS_BREVET_LENKETEKST,
                            hentDokumentlagerUrl(clientProperties, it),
                        )
                    }.takeIf { erInnsynDeaktivertForKommune },
            digisosSak.kommunenummer,
            digisosSak.originalSoknadNAV?.navEksternRefId,
        )
    }

    data class UtvidetSoknadsStatus(
        val status: SoknadsStatus,
        val tidspunktSendt: LocalDateTime?,
        val navKontor: String?,
        val soknadUrl: UrlResponse?,
        val kommunenummer: String?,
        val navEksternRefId: String?,
    )

    companion object {
        private val log by logger()
    }
}
