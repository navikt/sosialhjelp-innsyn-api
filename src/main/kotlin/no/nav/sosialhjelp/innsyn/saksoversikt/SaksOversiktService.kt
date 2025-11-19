package no.nav.sosialhjelp.innsyn.saksoversikt

import io.getunleash.Unleash
import no.nav.sosialhjelp.innsyn.app.featuretoggle.FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.KILDE_INNSYN_API
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixTimestampToDate
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.util.Date

@Component
class SaksOversiktService(
    private val fiksClient: FiksClient,
    private val unleashClient: Unleash,
    private val oppgaveService: OppgaveService,
    private val eventService: EventService,
) {
    suspend fun hentAlleSaker(): List<SaksListeResponse> =
        hentAlleDigisosSakerFraFiks()
            .sortedByDescending { it.sistOppdatert }

    private suspend fun hentAlleDigisosSakerFraFiks(): List<SaksListeResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker()
        val responseList =
            digisosSaker
                // Ikke returner "tomme" søknader som som regel er feilregistreringer
                .partition { it.originalSoknadNAV == null && it.digisosSoker == null }
                .let { (tommeSoknader, gyldigeSoknader) ->
                    log.info("Fant ${tommeSoknader.size} tomme søknader. Ider: ${tommeSoknader.map { it.fiksDigisosId }}")
                    log.info("Fant ${gyldigeSoknader.size} gyldige søknader. Ider: ${gyldigeSoknader.map { it.fiksDigisosId }}")
                    gyldigeSoknader
                }.map {
                    val model = eventService.createModel(it)
                    val soknadSendtEvent = model.historikk.find { hendelse ->
                        hendelse.hendelseType == HendelseTekstType.SOKNAD_SEND_TIL_KONTOR ||
                            hendelse.hendelseType == HendelseTekstType.SOKNAD_MOTTATT_UTEN_KOMMUNENAVN ||
                            hendelse.hendelseType == HendelseTekstType.SOKNAD_MOTTATT_MED_KOMMUNENAVN
                    }
                    val soknadOpprettet = soknadSendtEvent?.tidspunkt?.let { tidspunkt ->
                        Date.from(tidspunkt.toInstant(ZoneOffset.UTC))
                    } ?: run {
                        log.warn("Ingen søknad opprettet hendelse funnet for fiksDigisosId=${it.fiksDigisosId}. Bruker sistEndret som fallback.")
                        unixTimestampToDate(it.sistEndret)
                    }

                    SaksListeResponse(
                        fiksDigisosId = it.fiksDigisosId,
                        soknadTittel = "saker.default_tittel",
                        sistOppdatert = unixTimestampToDate(it.sistEndret),
                        kilde = KILDE_INNSYN_API,
                        url = null,
                        kommunenummer = it.kommunenummer,
                        soknadOpprettet = soknadOpprettet,
                    )
                }

        if (unleashClient.isEnabled(FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER, false) &&
            digisosSaker.isNotEmpty() &&
            oppgaveService.getFagsystemHarVilkarOgDokumentasjonkrav(digisosSaker[0].fiksDigisosId)
        ) {
            if (oppgaveService.sakHarStatusMottattOgIkkeHattSendt(digisosSaker[0].fiksDigisosId)) {
                log.info("Kommune med kommunenummer ${digisosSaker[0].kommunenummer} har aktivert innsyn i papirsøknader")
            } else {
                log.info(
                    "Kommune med kommunenummer ${digisosSaker[0].kommunenummer} har fagsystemversjon som støtter innsyn i papirsøknader",
                )
            }
        }

        return responseList
    }

    companion object {
        private val log by logger()
    }
}
