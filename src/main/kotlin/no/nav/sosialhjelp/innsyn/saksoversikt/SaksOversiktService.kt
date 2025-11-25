package no.nav.sosialhjelp.innsyn.saksoversikt

import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.stereotype.Component

@Component
class SaksOversiktService(
    private val fiksClient: FiksClient,
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
                    val soknadOpprettet =
                        it.originalSoknadNAV?.timestampSendt?.let { timestamp ->
                            unixToLocalDateTime(timestamp)
                        }

                    SaksListeResponse(
                        fiksDigisosId = it.fiksDigisosId,
                        soknadTittel = "saker.default_tittel",
                        sistOppdatert = unixToLocalDateTime(it.sistEndret),
                        kommunenummer = it.kommunenummer,
                        soknadOpprettet = soknadOpprettet,
                        isPapirSoknad = it.originalSoknadNAV == null,
                    )
                }

        return responseList
    }

    companion object {
        private val log by logger()
    }
}
