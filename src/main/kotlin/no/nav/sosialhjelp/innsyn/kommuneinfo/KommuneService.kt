package no.nav.sosialhjelp.innsyn.kommuneinfo

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.kommuneinfo.domain.Kommune
import no.nav.sosialhjelp.innsyn.kommuneinfo.dto.KommuneDto
import no.nav.sosialhjelp.innsyn.redis.KOMMUNE_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.stereotype.Component

@Component
class KommuneService(
    private val fiksClient: FiksClient,
    private val kommuneClient: KommuneClient,
    private val redisService: RedisService
) {

    fun hentKommune(fiksDigisosId: String, token: String): Kommune? {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val kommunenummer: String = digisosSak.kommunenummer

        if (kommunenummer.isBlank()) {
            log.warn("Forsøkte å hente kommuneStatus, men JsonSoknad.mottaker.kommunenummer er tom i soknad.json")
            throw RuntimeException("KommuneStatus kan ikke hentes fordi DigisosSak mangler kommunenummer")
        }

        return hentFraCache(kommunenummer) ?: hentFraServer(kommunenummer)
    }

    fun erInnsynDeaktivertForKommune(fiksDigisosId: String, token: String): Boolean {
        val kommune = hentKommune(fiksDigisosId, token)
        return kommune == null || !kommune.kanOppdatereStatus
    }

    private fun hentFraCache(kommunenummer: String) =
        redisService.get(cacheKey(kommunenummer), KommuneDto::class.java)
            ?.toDomain()

    private fun hentFraServer(kommunenummer: String): Kommune? {
        return try {
            kommuneClient.getKommuneDto(kommunenummer)
                ?.also { redisService.put(cacheKey(kommunenummer), objectMapper.writeValueAsBytes(it)) }
                ?.toDomain()
        } catch (e: FiksClientException) {
            null
        } catch (e: FiksServerException) {
            null
        } catch (e: FiksException) {
            null
        }
    }

    private fun cacheKey(kommunenummer: String): String = KOMMUNE_CACHE_KEY_PREFIX + kommunenummer

    companion object {
        private val log by logger()
    }
}
