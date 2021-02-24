package no.nav.sosialhjelp.innsyn.client.unleash.strategy

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ByInstanceIdStrategyTest {

    @Test
    fun shouldReturnFalse_instanceIdNotInMap() {
        val strategy = ByInstanceIdStrategy("local")
        val parameters = mutableMapOf("instance.id" to "dev-sbs,dev-sbs-intern")
        assertFalse(strategy.isEnabled(parameters))
    }

    @Test
    fun shoudReturnTrue_instanceIdInMap() {
        val strategy = ByInstanceIdStrategy("dev-sbs")
        val parameters = mutableMapOf("instance.id" to "dev-sbs,dev-sbs-intern")
        assertTrue(strategy.isEnabled(parameters))
    }
}