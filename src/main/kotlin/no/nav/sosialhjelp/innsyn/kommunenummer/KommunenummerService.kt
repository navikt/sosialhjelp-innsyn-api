package no.nav.sosialhjelp.innsyn.kommunenummer

import no.nav.sosialhjelp.innsyn.redis.KARTVERKET_KOMMUNENUMMER_KEY
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.springframework.stereotype.Component

@Component
class KommunenummerService(
    private val kartverketClient: KartverketClient,
    private val redisService: RedisService
) {

    fun getKommunenummer(): String {
        return hentFraCache() ?: hentFraServer()
    }

    private fun hentFraCache(): String? {
        return redisService.get(KARTVERKET_KOMMUNENUMMER_KEY, String::class.java)?.let { it as String }
    }

    private fun hentFraServer(): String {
        return kartverketClient.getKommunenummer()
            .also {
                redisService.put(KARTVERKET_KOMMUNENUMMER_KEY, it.toByteArray(), ONE_HOUR_IN_SECONDS)
            }
    }

    companion object {
        private const val ONE_HOUR_IN_SECONDS = 1 * 60L * 60L
    }
}
