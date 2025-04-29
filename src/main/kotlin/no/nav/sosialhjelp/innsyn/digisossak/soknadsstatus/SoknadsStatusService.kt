package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
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
        token: String,
        fnr: String,
    ): UtvidetSoknadsStatus {
        val digisosSak = fiksClient.hentDigisosSakMedFnr(fiksDigisosId, token, fnr)
        val model = eventService.createModel(digisosSak, token)
        val status = model.status

        log.info("Hentet nåværende søknadsstatus=${status.name}")
        val erInnsynDeaktivertForKommune = kommuneService.erInnsynDeaktivertForKommune(fiksDigisosId, token)
        val dokumentlagerId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId

        // TODO henting av fnr og sammeligning samt sammenligning av digisosId benyttes til søk i feilsituasjon. Fjernes når feilsøking er ferdig.
        val fnr2 = SubjectHandlerUtils.getUserIdFromToken()

        if (fnr2 != fnr) {
            log.error("Fødselsnr i kontekst har blitt endret - SoknadsstatusService.hentsSoknadStatus")
        }

        val modelFiksDigisosId = model.fiksDigisosId
        if (modelFiksDigisosId != null && fiksDigisosId != modelFiksDigisosId) {
            log.error(
                "Intern model inneholder en annen digisosID en det som ble sendt inn. Intern model har digisosId: ${model.fiksDigisosId}" +
                    " og digisosID: $fiksDigisosId ble sendt inn",
            )
        }

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
