package no.nav.sosialhjelp.innsyn.valkey

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

internal class DokumentCacheResolverTest {
    @Test
    fun `selects typed cache for each supported document type`() {
        assertThat(cacheNameFor(JsonDigisosSoker::class.java)).isEqualTo(JsonDigisosSokerCacheConfig.CACHE_NAME)
        assertThat(cacheNameFor(JsonSoknad::class.java)).isEqualTo(JsonSoknadCacheConfig.CACHE_NAME)
        assertThat(cacheNameFor(JsonVedleggSpesifikasjon::class.java)).isEqualTo(JsonVedleggSpesifikasjonCacheConfig.CACHE_NAME)
    }

    @Test
    fun `rejects unsupported document type`() {
        assertThatIllegalStateException()
            .isThrownBy { cacheNameFor(String::class.java) }
            .withMessage("Unsupported document type for cache: java.lang.String")
    }
}
