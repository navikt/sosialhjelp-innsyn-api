package no.nav.sosialhjelp.innsyn.valkey

import java.time.Duration
import org.springframework.context.annotation.Configuration


@Configuration
class DokumentCacheConfig : InnsynApiCacheConfig(CACHE_NAME, ttl) {
    companion object {
        const val CACHE_NAME: String = "dokument"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class NavEnhetCacheConfig : InnsynApiCacheConfig(CACHE_NAME, ttl) {
    companion object {
        const val CACHE_NAME: String = "navenhet"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class DigisosSakCacheConfig : InnsynApiCacheConfig(CACHE_NAME) {
    companion object {
        const val CACHE_NAME: String = "digisosSak"
    }
}

@Configuration
class KommuneInfoCacheConfig : InnsynApiCacheConfig(CACHE_NAME) {
    companion object {
        const val CACHE_NAME: String = "kommuneinfo"
    }
}

@Configuration
class PdlNavnCacheConfig : InnsynApiCacheConfig(CACHE_NAME, ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlNavn"
        private val ttl: Duration = Duration.ofDays(1)
    }
}

@Configuration
class AdressebeskyttelseCacheConfig : InnsynApiCacheConfig(CACHE_NAME, ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlAdressebeskyttelse"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class HistoriskeIdenterOldCacheConfig : InnsynApiCacheConfig(CACHE_NAME, ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlHistoriskeIdenterOld"
        private val ttl: Duration = Duration.ofDays(1)
    }
}

@Configuration
class AdressebeskyttelseOldCacheConfig : InnsynApiCacheConfig(CACHE_NAME, ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlAdressebeskyttelseOld"
        private val ttl: Duration = Duration.ofHours(1)
    }
}
