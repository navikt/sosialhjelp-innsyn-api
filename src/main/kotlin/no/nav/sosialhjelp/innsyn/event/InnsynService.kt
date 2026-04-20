package no.nav.sosialhjelp.innsyn.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

@Component
class InnsynService(
    private val fiksService: FiksService,
    private val kommuneService: KommuneService,
) {
    suspend fun hentJsonDigisosSoker(digisosSak: DigisosSak): JsonDigisosSoker? {
        val metadataId = digisosSak.digisosSoker?.metadata
        val sistOppdatert = digisosSak.digisosSoker?.timestampSistOppdatert
        return when {
            kommuneService.erInnsynDeaktivertForKommune(
                digisosSak.fiksDigisosId,
            ) ->
                log.debug("Kommune har deaktivert innsyn -> henter ikke innsynsdata").let {
                    null
                }

            metadataId != null && sistOppdatert != null ->
                fiksService.getDocument(
                    digisosSak.fiksDigisosId,
                    metadataId,
                    JsonDigisosSoker::class.java,
                    "${metadataId}_$sistOppdatert",
                )

            else -> null
        }
    }

    suspend fun hentJsonDigisosSokerBulk(saker: List<DigisosSak>): Map<String, JsonDigisosSoker> {
        val scope = CoroutineScope(Dispatchers.IO)
        val kommuneDeaktivert =
            saker
                .map { sak ->
                    scope.async {
                        sak.fiksDigisosId to
                            kommuneService.erInnsynDeaktivertForKommune(
                                sak.fiksDigisosId,
                            )
                    }
                }.awaitAll()
                .toMap()

        val ids =
            saker
                .filter {
                    it.digisosSoker?.metadata != null && it.digisosSoker?.timestampSistOppdatert != null &&
                        kommuneDeaktivert[it.fiksDigisosId] == false
                }.associate { it.fiksDigisosId to it.digisosSoker?.metadata!! }

        return fiksService.getAllInnsynsfiler(ids)
    }

    suspend fun hentOriginalSoknad(digisosSak: DigisosSak): JsonSoknad? {
        val originalMetadataId = digisosSak.originalSoknadNAV?.metadata
        return when {
            originalMetadataId != null ->
                fiksService.getDocument(
                    digisosSak.fiksDigisosId,
                    originalMetadataId,
                    JsonSoknad::class.java,
                )

            else -> null
        }
    }

    companion object {
        private val log by logger()
    }
}
