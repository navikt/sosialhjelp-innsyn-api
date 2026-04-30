package no.nav.sosialhjelp.innsyn.soknad.api

import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.valkey.SkjuleOriginalSoknadCache
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SoknadApiService(
    private val soknadApiClient: SoknadApiClient?,
) {
    /*
        DSOS-649
        Pga. en bug hvor nye søknader fikk feil telefonnummer fra register, så skal vi ikke
        vise originalsøknad for søknader som ble sendt inn i tidsrommet 28.04.26 14:32 til 29.04.26 12:30
        Disse søknadene ligger ikke lenger i databasen for at digisos-id ikke skal kunne knyttes til personnummer
        TODO Dette kan slettes når bruker ikke lenger kan se søknad i innsyn (hvor lenge?)
     */
    @Cacheable(SkjuleOriginalSoknadCache.CACHE_NAME, key = "#fiksDigisosId")
    suspend fun skalSkjuleOriginalSoknad(fiksDigisosId: String): Boolean {
        logger.info("Sjekker om originalsøknad skal skjules for digisosId: $fiksDigisosId")
        return soknadApiClient?.skalSkjuleOriginalSoknad(fiksDigisosId) == true
    }

    companion object {
        private val logger by logger()
    }
}
