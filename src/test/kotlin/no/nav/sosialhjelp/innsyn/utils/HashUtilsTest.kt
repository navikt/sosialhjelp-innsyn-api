package no.nav.sosialhjelp.innsyn.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HashUtilsTest {
    @Test
    fun `similar input creates different output hash`() {
        val input1 = "123"
        val input2 = "124"

        assertThat(sha256(input1)).isNotEqualTo(sha256(input2))
    }

    @Test
    fun `repeated calls to hash function returns same result`() {
        val input = "123"

        assertThat(sha256(input)).isEqualTo(sha256(input))
    }
}
