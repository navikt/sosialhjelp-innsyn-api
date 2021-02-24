package no.nav.sosialhjelp.innsyn.service.kommune

import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.service.idporten.IdPortenService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.stereotype.Component

@Component
class KommuneService(
        private val fiksClient: FiksClient,
        private val kommuneInfoClient: KommuneInfoClient,
        private val idPortenService: IdPortenService,
        private val redisService: RedisService
) {

    fun hentKommuneInfo(fiksDigisosId: String, token: String): KommuneInfo? {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val kommunenummer: String? = digisosSak.kommunenummer

        if (kommunenummer == null) {
            log.warn("Forsøkte å hente kommuneStatus, men JsonSoknad.mottaker.kommunenummer finnes ikke i soknad.json")
            throw RuntimeException("KommuneStatus kan ikke hentes fordi kommunenummer mangler for digisosId=$fiksDigisosId")
        }

        return hentFraCache(kommunenummer) ?: hentKommuneInfoFraFiks(kommunenummer)
    }

    private fun hentFraCache(kommunenummer: String) =
            redisService.get(kommunenummer, KommuneInfo::class.java) as KommuneInfo?

    private fun hentKommuneInfoFraFiks(kommunenummer: String): KommuneInfo? {
        return try {
            kommuneInfoClient.get(kommunenummer, getToken())
                    .also { redisService.put(kommunenummer, objectMapper.writeValueAsBytes(it)) }
        } catch (e: FiksClientException) {
            null
        } catch (e: FiksServerException) {
            null
        } catch (e: FiksException) {
            null
        }
    }

    private fun getToken(): String {
        return idPortenService.getToken().token
    }

    fun erInnsynDeaktivertForKommune(fiksDigisosId: String, token: String): Boolean {
        val kommuneInfo = hentKommuneInfo(fiksDigisosId, token)
        return kommuneInfo == null || !kommuneInfo.kanOppdatereStatus
    }

    companion object {
        private val log by logger()
    }
}