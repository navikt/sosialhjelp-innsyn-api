package no.nav.sosialhjelp.innsyn.valkey

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlNavn
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class JsonDigisosSokerCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<JsonDigisosSoker>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "dokumentJsonDigisosSoker"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class JsonSoknadCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<JsonSoknad>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "dokumentJsonSoknad"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class JsonVedleggSpesifikasjonCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<JsonVedleggSpesifikasjon>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "dokumentJsonVedleggSpesifikasjon"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class NavEnhetCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<NavEnhet>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "navenhet"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class DigisosSakCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<DigisosSak>()) {
    companion object {
        const val CACHE_NAME: String = "digisosSak"
    }
}

@Configuration
class KommuneInfoCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<KommuneInfo>()) {
    companion object {
        const val CACHE_NAME: String = "kommuneinfo"
    }
}

@Configuration
class PdlNavnCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<PdlNavn>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlNavn"
        private val ttl: Duration = Duration.ofDays(1)
    }
}

@Configuration
class AdressebeskyttelseCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<Boolean>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlAdressebeskyttelse"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class HistoriskeIdenterOldCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<List<String>>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlHistoriskeIdenterOld"
        private val ttl: Duration = Duration.ofDays(1)
    }
}

@Configuration
class AdressebeskyttelseOldCacheConfig : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<PdlHentPerson>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "pdlAdressebeskyttelseOld"
        private val ttl: Duration = Duration.ofHours(1)
    }
}

@Configuration
class SkjuleOriginalSoknadCache : InnsynApiCacheConfig(CACHE_NAME, cacheValueType<Boolean>(), ttl) {
    companion object {
        const val CACHE_NAME: String = "skjuleOriginalSoknad"
        private val ttl: Duration = Duration.ofHours(5)
    }
}
