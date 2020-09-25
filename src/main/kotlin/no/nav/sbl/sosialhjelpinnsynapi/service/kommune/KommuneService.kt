package no.nav.sbl.sosialhjelpinnsynapi.service.kommune

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import org.springframework.stereotype.Component

@Component
class KommuneService(
        private val fiksClient: FiksClient,
        private val kommuneInfoClient: KommuneInfoClient
) {

    fun hentKommuneInfo(fiksDigisosId: String, token: String): KommuneInfo? {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val kommunenummer: String? = digisosSak.kommunenummer

        if (kommunenummer == null) {
            log.warn("Forsøkte å hente kommuneStatus, men JsonSoknad.mottaker.kommunenummer finnes ikke i soknad.json for digisosId=$fiksDigisosId")
            throw RuntimeException("KommuneStatus kan ikke hentes fordi kommunenummer mangler for digisosId=$fiksDigisosId")
        }

        return try {
            kommuneInfoClient.get(kommunenummer)
        } catch (e: FiksClientException) {
            null
        } catch (e: FiksServerException) {
            null
        } catch (e: FiksException) {
            null
        }
    }

    companion object {
        private val log by logger()
    }
}
