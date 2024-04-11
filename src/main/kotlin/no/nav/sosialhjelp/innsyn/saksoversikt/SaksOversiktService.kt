package no.nav.sosialhjelp.innsyn.saksoversikt

import io.getunleash.Unleash
import no.nav.sosialhjelp.innsyn.app.featuretoggle.FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.KILDE_INNSYN_API
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixTimestampToDate
import org.springframework.stereotype.Component

@Component
class SaksOversiktService(
    private val fiksClient: FiksClient,
    private val unleashClient: Unleash,
    private val oppgaveService: OppgaveService,
) {
    suspend fun hentAlleSaker(token: String): List<SaksListeResponse> {
        return hentAlleDigisosSakerFraFiks(token).toMutableList()
            .sortedByDescending { it.sistOppdatert }
    }

    private suspend fun hentAlleDigisosSakerFraFiks(token: String): List<SaksListeResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)
        val responseList =
            digisosSaker
                .filterNot { it.originalSoknadNAV == null && it.digisosSoker == null } // Ikke returner "tomme" søknader som som regel er feilregistreringer
                .map {
                    SaksListeResponse(
                        fiksDigisosId = it.fiksDigisosId,
                        soknadTittel = "saker.default_tittel",
                        sistOppdatert = unixTimestampToDate(it.sistEndret),
                        kilde = KILDE_INNSYN_API,
                        url = null,
                        kommunenummer = it.kommunenummer,
                    )
                }

        // flyttet fra SaksOversiktController inn hit.
        if (unleashClient.isEnabled(FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER, false) &&
            digisosSaker.isNotEmpty() &&
            oppgaveService.getFagsystemHarVilkarOgDokumentasjonkrav(digisosSaker[0].fiksDigisosId, token)
        ) {
            if (oppgaveService.sakHarStatusMottattOgIkkeHattSendt(digisosSaker[0].fiksDigisosId, token)) {
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
