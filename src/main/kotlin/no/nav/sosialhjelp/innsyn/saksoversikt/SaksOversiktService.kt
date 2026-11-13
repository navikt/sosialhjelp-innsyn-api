package no.nav.sosialhjelp.innsyn.saksoversikt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.klage.KlageService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.stereotype.Component

@Component
class SaksOversiktService(
    private val fiksService: FiksService,
    private val klageService: KlageService,
) {
    suspend fun hentAlleSaker(): List<SaksListeResponse> =
        hentAlleDigisosSakerFraFiks()
            .sortedByDescending { it.sistOppdatert }

    private suspend fun hentAlleDigisosSakerFraFiks(): List<SaksListeResponse> {
        val digisosSaker = fiksService.getAllSoknader()
        val klager =
            runCatching { klageService.hentKlager() }
                .onFailure {
                    if (it is CancellationException) {
                        currentCoroutineContext().ensureActive()
                    }
                }.getOrDefault(emptyList())
        val responseList =
            digisosSaker
                // Ikke returner "tomme" søknader som som regel er feilregistreringer
                .filterNot { it.originalSoknadNAV == null && it.digisosSoker == null }
                .map {
                    val soknadOpprettet =
                        it.originalSoknadNAV?.timestampSendt?.let { timestamp ->
                            unixToLocalDateTime(timestamp)
                        }

                    SaksListeResponse(
                        fiksDigisosId = it.fiksDigisosId,
                        soknadTittel = "saker.default_tittel",
                        sistOppdatert = unixToLocalDateTime(setOfNotNull(it.digisosSoker?.timestampSistOppdatert, it.sistEndret).max()),
                        kommunenummer = it.kommunenummer,
                        soknadOpprettet = soknadOpprettet,
                        isPapirSoknad = it.originalSoknadNAV == null,
                        hasKlage = klager.any { klage -> klage.fiksDigisosId.toString() == it.fiksDigisosId },
                    )
                }

        return responseList
    }

    companion object {
        private val log by logger()
    }
}
