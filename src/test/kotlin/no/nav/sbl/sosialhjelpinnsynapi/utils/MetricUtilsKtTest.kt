package no.nav.sbl.sosialhjelpinnsynapi.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class MetricUtilsKtTest {

    @Test
    fun `SHA256 skal gi unike hash for ulike uuid men samme hash for samme uuid`() {
        val uuid1 = UUID.randomUUID().toString()
        val uuid2 = UUID.randomUUID().toString()

        assertThat(createSHA256Hash(uuid1)).isNotEqualTo(createSHA256Hash(uuid2))
        assertThat(createSHA256Hash(uuid1)).isEqualTo(createSHA256Hash(uuid1))
    }
}
