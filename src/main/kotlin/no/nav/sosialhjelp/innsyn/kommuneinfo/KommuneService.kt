package no.nav.sosialhjelp.innsyn.kommuneinfo

import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

@Component
class KommuneService(
    private val fiksClient: FiksClient,
    private val kommuneInfoClient: KommuneInfoClient,
) {
    suspend fun hentKommuneInfo(
        fiksDigisosId: String,
        token: String,
    ): KommuneInfo? {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        val kommunenummer: String = digisosSak.kommunenummer

        if (kommunenummer.isBlank()) {
            log.warn("Forsøkte å hente kommuneStatus, men JsonSoknad.mottaker.kommunenummer er tom i soknad.json")
            throw RuntimeException("KommuneStatus kan ikke hentes fordi DigisosSak mangler kommunenummer")
        }

        return hentKommuneInfoFraFiks(kommunenummer)
    }

    private suspend fun hentKommuneInfoFraFiks(kommunenummer: String): KommuneInfo? =
        try {
            kommuneInfoClient.getKommuneInfo(kommunenummer).also {
                if (it.harMidlertidigDeaktivertMottak) {
                    log.warn("Kommune $kommunenummer har midlertidig deaktivert mottak")
                }
                if (!it.kanMottaSoknader) {
                    log.warn("Kommune $kommunenummer kan ikke motta søknader/ettersendelser")
                }
                if (!it.kanOppdatereStatus) {
                    log.warn("Kommune $kommunenummer har ikke aktivert innsyn")
                }
                if (it.harMidlertidigDeaktivertOppdateringer) {
                    log.warn("Kommune $kommunenummer har midlertidig deaktivert innsyn")
                }
            }
        } catch (e: FiksClientException) {
            null
        } catch (e: FiksServerException) {
            null
        } catch (e: FiksException) {
            null
        }

    suspend fun erInnsynDeaktivertForKommune(
        fiksDigisosId: String,
        token: String,
    ): Boolean {
        val kommuneInfo = hentKommuneInfo(fiksDigisosId, token)
        return kommuneInfo == null || !kommuneInfo.kanOppdatereStatus
    }

    companion object {
        private val log by logger()
    }
}
