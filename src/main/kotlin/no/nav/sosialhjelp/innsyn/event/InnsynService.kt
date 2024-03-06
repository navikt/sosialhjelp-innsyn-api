package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

@Component
class InnsynService(
    private val fiksClient: FiksClient,
    private val kommuneService: KommuneService,
) {
    suspend fun hentJsonDigisosSoker(
        digisosSak: DigisosSak,
        token: String,
    ): JsonDigisosSoker? {
        val metadataId = digisosSak.digisosSoker?.metadata
        val sistOppdatert = digisosSak.digisosSoker?.timestampSistOppdatert
        return when {
            kommuneService.erInnsynDeaktivertForKommune(
                digisosSak.fiksDigisosId,
                token,
            ) ->
                log.debug("Kommune har deaktivert innsyn -> henter ikke innsynsdata").let {
                    null
                }
            metadataId != null && sistOppdatert != null ->
                fiksClient.hentDokument(
                    digisosSak.fiksDigisosId,
                    metadataId,
                    JsonDigisosSoker::class.java,
                    token,
                    "${metadataId}_$sistOppdatert",
                )

            else -> null
        }
    }

    suspend fun hentOriginalSoknad(
        digisosSak: DigisosSak,
        token: String,
    ): JsonSoknad? {
        val originalMetadataId = digisosSak.originalSoknadNAV?.metadata
        return when {
            originalMetadataId != null ->
                fiksClient.hentDokument(
                    digisosSak.fiksDigisosId,
                    originalMetadataId,
                    JsonSoknad::class.java,
                    token,
                )
            else -> null
        }
    }

    companion object {
        private val log by logger()
    }
}
