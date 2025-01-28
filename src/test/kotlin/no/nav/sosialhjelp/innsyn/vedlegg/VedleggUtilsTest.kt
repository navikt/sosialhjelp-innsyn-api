package no.nav.sosialhjelp.innsyn.vedlegg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VedleggUtilsTest {
    @Test
    fun `getSha512FromInputStream skal returnere unik sha512 for forskjellig input`() {
        val is1 = "test".toByteArray().inputStream()
        val is2 = "hdi.palrcga dil 2".toByteArray().inputStream()

        val sha1 = getSha512FromInputStream(is1)
        val sha2 = getSha512FromInputStream(is2)

        is1.readAllBytes()
        is2.readAllBytes()

        assertThat(sha1).isNotEqualTo(sha2)
    }
}
